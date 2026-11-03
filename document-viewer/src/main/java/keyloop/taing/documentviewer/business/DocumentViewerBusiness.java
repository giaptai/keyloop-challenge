package keyloop.taing.documentviewer.business;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import keyloop.taing.documentviewer.client.SalesClient;
import keyloop.taing.documentviewer.client.ServiceClient;
import keyloop.taing.documentviewer.data.DocumentSnapshot;
import keyloop.taing.documentviewer.data.dto.Source;
import keyloop.taing.documentviewer.data.dto.SourceStatus;
import keyloop.taing.documentviewer.data.dto.SuccessRes;
import keyloop.taing.documentviewer.data.dto.UnifiedDocument;
import keyloop.taing.documentviewer.data.repository.DocumentRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class DocumentViewerBusiness {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentViewerBusiness.class);
    private final DocumentRepository documentRepository;
    private final SalesClient salesClient;
    private final ServiceClient serviceClient;
    private final Executor executor;
    private final ObjectMapper objectMapper;

    public DocumentViewerBusiness(SalesClient salesClient, ServiceClient serviceClient,
            @Qualifier("aggregationExecutor") Executor executor,
            ObjectMapper objectMapper, DocumentRepository documentRepository) {
        this.salesClient = salesClient;
        this.serviceClient = serviceClient;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.documentRepository = documentRepository;
    }

    public SuccessRes search(String vin) {
        if (!_validVIN(vin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid VIN format. VIN must be 17 characters and exclude I, O, Q.");
        }
        Throwable[] errors = new Throwable[2];
        CompletableFuture<List<UnifiedDocument>> salesFuture = CompletableFuture
                .supplyAsync(new Supplier<List<UnifiedDocument>>() {
                    @Override
                    public List<UnifiedDocument> get() {
                        return salesClient.fetchDocuments(vin).stream()
                                .map(sale -> new UnifiedDocument(sale.documentId(), sale.vin(), sale.title(),
                                        sale.documentType(), sale.status(), sale.referenceNumber(), sale.createdAt(),
                                        sale.fileName(),
                                        Source.SALES))
                                .toList();
                    }
                }, executor)
                .handle((result, ex) -> {
                    errors[0] = ex;
                    return ex == null ? result : List.<UnifiedDocument>of();
                });
        CompletableFuture<List<UnifiedDocument>> serviceFuture = CompletableFuture
                .supplyAsync(
                        () -> serviceClient.fetchDocuments(vin).stream()
                                .map(service -> new UnifiedDocument(service.documentId(), service.vin(),
                                        service.title(), service.documentType(), service.status(),
                                        service.referenceNumber(), service.createdAt(), null, Source.SERVICE))
                                .toList(),
                        executor)
                .handle((result, ex) -> {
                    errors[1] = ex;
                    return ex == null ? result : List.<UnifiedDocument>of();
                });
        CompletableFuture.allOf(salesFuture, serviceFuture).join();
        List<UnifiedDocument> sales;
        SourceStatus salesStatus;
        if (errors[0] == null) {
            sales = salesFuture.join();
            CompletableFuture.runAsync(() -> _saveSnapshot(vin, Source.SALES, sales), executor);
            salesStatus = _statusOf(Source.SALES, null);
        } else {
            LOGGER.warn("SALES call failed for vin={}, message={}", vin, errors[0].getMessage());
            salesStatus = _statusOf(Source.SALES, errors[0]);
            if ("NOT_FOUND".equals(salesStatus.status())) {
                sales = List.of();
            } else {
                Optional<DocumentSnapshot> snap = _loadSnapshot(vin, Source.SALES);
                if (snap.isPresent()) {
                    sales = snap.map(t -> _readDocuments(t)).orElse(List.of());
                    salesStatus = _staleStatus(snap.get());
                } else {
                    sales = List.of();
                }
            }
        }

        List<UnifiedDocument> services;
        SourceStatus servicesStatus;
        if (errors[1] == null) {
            services = serviceFuture.join();
            CompletableFuture.runAsync(() -> _saveSnapshot(vin, Source.SERVICE, services), executor);
            servicesStatus = _statusOf(Source.SERVICE, null);
        } else {
            LOGGER.warn("SERVICE call failed for vin={}, message={}", vin, errors[1].getMessage());
            servicesStatus = _statusOf(Source.SERVICE, errors[1]);
            if ("NOT_FOUND".equals(servicesStatus.status())) {
                services = List.of();
            } else {
                Optional<DocumentSnapshot> snap = _loadSnapshot(vin, Source.SERVICE);
                if (snap.isPresent()) {
                    services = snap.map(t -> _readDocuments(t)).orElse(List.of());
                    servicesStatus = _staleStatus(snap.get());
                } else {
                    services = List.of();
                }
            }
        }
        if (!_isUsable(salesStatus) && !_isUsable(servicesStatus)) {
            if ("NOT_FOUND".equals(salesStatus.status()) && "NOT_FOUND".equals(servicesStatus.status())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VIN not found.");
            }
            LOGGER.error("Both sources unavailable for vin={} sales={} service={}",
                    vin, salesStatus.status(), servicesStatus.status());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Both APIs are unavailable.");
        }
        List<UnifiedDocument> merged = Stream.concat(sales.stream(), services.stream())
                .sorted(Comparator.comparing(UnifiedDocument::createdAt))
                .toList();

        LOGGER.info("vin={} sales={} service={} documents={}", vin, salesStatus.status(), servicesStatus.status(),
                merged.size());
        return new SuccessRes(vin, merged, List.of(salesStatus, servicesStatus));
    }

    private SourceStatus _statusOf(Source source, Throwable ex) {
        if (ex == null) {
            return new SourceStatus(source, HttpStatus.OK.name(), null);
        }

        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof SocketTimeoutException || cause instanceof TimeoutException) {
            return new SourceStatus(source, "TIMEOUT", "Timeout");
        }
        if (cause instanceof HttpClientErrorException.NotFound) {
            return new SourceStatus(source, "NOT_FOUND", "VIN not found");
        }
        return new SourceStatus(source, "UNAVAILABLE", "API is not working");
    }

    private boolean _validVIN(String vin) {
        String regex = "^[A-HJ-NPR-Z0-9]{17}$";
        return vin != null && Pattern.matches(regex, vin.trim().toUpperCase());
    }

    private void _saveSnapshot(String vin, Source source, List<UnifiedDocument> docs) {
        try {
            String payload = objectMapper.writeValueAsString(docs);
            DocumentSnapshot snapshot = documentRepository.findByVinAndSource(vin, source)
                    .orElseGet(DocumentSnapshot::new);
            snapshot.setVin(vin);
            snapshot.setSource(source);
            snapshot.setPayload(payload);
            snapshot.setFetchedAt(LocalDateTime.now());
            documentRepository.save(snapshot);
        } catch (JacksonException e) {
            LOGGER.warn("Cannot serialize snapshot for vin={} source={}", vin, source, e);
        } catch (DataAccessException e) {
            LOGGER.warn("Cannot save snapshot for vin={} source={}", vin, source, e);
        }
    }

    private Optional<DocumentSnapshot> _loadSnapshot(String vin, Source source) {
        try {
            return documentRepository.findByVinAndSource(vin, source);
        } catch (DataAccessException e) {
            LOGGER.warn("Cannot load snapshot for vin={} source={}", vin, source, e);
            return Optional.empty();
        }
    }

    private List<UnifiedDocument> _readDocuments(DocumentSnapshot snap) {
        try {
            return objectMapper.readValue(snap.getPayload(), new TypeReference<List<UnifiedDocument>>() {
            });
        } catch (Exception e) {
            LOGGER.warn("Cannot parse snapshot for source={}", snap.getSource(), e);
            return List.of();
        }
    }

    private SourceStatus _staleStatus(DocumentSnapshot snap) {
        return new SourceStatus(snap.getSource(), "STALE",
                "Data retrieved at " + snap.getFetchedAt() + ", the source is currently unavailable");
    }

    private boolean _isUsable(SourceStatus s) {
        return "OK".equals(s.status()) || "STALE".equals(s.status());
    }
}
