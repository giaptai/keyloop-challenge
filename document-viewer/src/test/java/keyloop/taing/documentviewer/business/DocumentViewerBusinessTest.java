package keyloop.taing.documentviewer.business;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import keyloop.taing.documentviewer.client.SalesClient;
import keyloop.taing.documentviewer.client.ServiceClient;
import keyloop.taing.documentviewer.data.DocumentSnapshot;
import keyloop.taing.documentviewer.data.dto.SalesDoc;
import keyloop.taing.documentviewer.data.dto.ServiceDoc;
import keyloop.taing.documentviewer.data.dto.Source;
import keyloop.taing.documentviewer.data.dto.UnifiedDocument;
import keyloop.taing.documentviewer.data.repository.DocumentRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class DocumentViewerBusinessTest {
    private final String VIN = "1FTFW1ET5DFC10312";

    @Mock
    private SalesClient salesClient;

    @Mock
    private ServiceClient serviceClient;

    @Mock
    private DocumentRepository documentRepository;

    private ExecutorService executor;
    private ObjectMapper objectMapper;
    private DocumentViewerBusiness business;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        objectMapper = new ObjectMapper();
        business = new DocumentViewerBusiness(salesClient, serviceClient, executor, objectMapper,
                documentRepository);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    void allNormal() {
        when(salesClient.fetchDocuments(VIN)).thenReturn(List.of(salesDoc("SAL-1", "2026-03-12T14:00:00")));
        when(serviceClient.fetchDocuments(VIN)).thenReturn(List.of(serviceDoc("SVC-1", "2026-04-02T09:00:00")));

        var res = business.search(VIN);
        assertThat(res.documents()).extracting(t -> t.documentId()).containsExactly("SAL-1", "SVC-1");
        assertThat(res.sources()).extracting("status").containsExactly("OK", "OK");
        assertThat(res.documents()).extracting(t -> t.source()).containsExactly(Source.SALES, Source.SERVICE);
    }

    @Test
    void salesFails_noSnapshot() {
        when(salesClient.fetchDocuments(VIN)).thenThrow(new RuntimeException("down"));
        when(serviceClient.fetchDocuments(VIN)).thenReturn(List.of(serviceDoc("SVC-1", "2026-04-02T09:00:00")));
        when(documentRepository.findByVinAndSource(VIN, Source.SALES)).thenReturn(Optional.empty());
        var res = business.search(VIN);
        assertThat(res.documents()).extracting(UnifiedDocument::documentId).containsExactly("SVC-1");
        assertThat(res.sources()).extracting("status").containsExactly("UNAVAILABLE", "OK");
    }

    @Test
    void salesFails_hasSnapshot() throws Exception {
        when(salesClient.fetchDocuments(VIN)).thenThrow(new RuntimeException("down"));
        when(serviceClient.fetchDocuments(VIN)).thenReturn(List.of(serviceDoc("SVC-1", "2026-04-02T09:00:00")));
        when(documentRepository.findByVinAndSource(VIN, Source.SALES))
                .thenReturn(Optional.of(snapshot(Source.SALES, List.of(
                        unified("SAL-OLD", Source.SALES, "2026-03-12T14:00:00")))));

        var res = business.search(VIN);
        assertThat(res.documents()).extracting(UnifiedDocument::documentId)
                .containsExactly("SAL-OLD", "SVC-1");
        assertThat(res.sources()).extracting("status").containsExactly("STALE", "OK");
    }

    @Test
    void bothFail_noSnapshot() {
        when(salesClient.fetchDocuments(VIN)).thenThrow(new RuntimeException("down"));
        when(serviceClient.fetchDocuments(VIN)).thenThrow(new RuntimeException("down"));
        when(documentRepository.findByVinAndSource(VIN, Source.SALES)).thenReturn(Optional.empty());
        when(documentRepository.findByVinAndSource(VIN, Source.SERVICE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> business.search(VIN))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void invalidVin() {
        assertThatThrownBy(() -> business.search("ABC"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void readTimeout() {
        when(salesClient.fetchDocuments(VIN)).thenThrow(
                new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out")));
        when(serviceClient.fetchDocuments(VIN)).thenReturn(List.of(serviceDoc("SVC-1", "2026-04-02T09:00:00")));
        when(documentRepository.findByVinAndSource(VIN, Source.SALES)).thenReturn(Optional.empty());

        var res = business.search(VIN);

        assertThat(res.sources()).extracting("status").containsExactly("TIMEOUT", "OK");
        assertThat(res.sources()).extracting("message").containsExactly("Timeout", null);
    }

    @Test
    void connectTimeout() {
        when(salesClient.fetchDocuments(VIN)).thenThrow(
                new ResourceAccessException("I/O error", new SocketTimeoutException("connect timed out")));
        when(serviceClient.fetchDocuments(VIN)).thenReturn(List.of(serviceDoc("SVC-1", "2026-04-02T09:00:00")));
        when(documentRepository.findByVinAndSource(VIN, Source.SALES)).thenReturn(Optional.empty());

        var res = business.search(VIN);

        assertThat(res.sources()).extracting("status").containsExactly("TIMEOUT", "OK");
    }

    @Test
    void databaseDown_stillReturnsDataFromHealthySource() {
        when(salesClient.fetchDocuments(VIN)).thenReturn(List.of(salesDoc("SAL-1", "2026-03-12T14:00:00")));
        when(serviceClient.fetchDocuments(VIN)).thenThrow(new RuntimeException("down"));
        lenient().when(documentRepository.findByVinAndSource(VIN, Source.SALES))
                .thenThrow(new DataAccessResourceFailureException("db down"));
        lenient().when(documentRepository.findByVinAndSource(VIN, Source.SERVICE))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        var res = business.search(VIN);

        assertThat(res.documents()).extracting(UnifiedDocument::documentId).containsExactly("SAL-1");
        assertThat(res.sources()).extracting("status").containsExactly("OK", "UNAVAILABLE");
    }

    private SalesDoc salesDoc(String id, String createdAt) {
        return new SalesDoc(id, VIN, "Title", "PURCHASE_AGREEMENT", "SIGNED", "REF",
                LocalDateTime.parse(createdAt), "file.pdf");
    }

    private ServiceDoc serviceDoc(String id, String createdAt) {
        return new ServiceDoc(id, VIN, "Title", "SERVICE_ORDER", "COMPLETED", "REF",
                LocalDateTime.parse(createdAt));
    }

    private UnifiedDocument unified(String id, Source source, String createdAt) {
        return new UnifiedDocument(id, VIN, "Title", "PURCHASE_AGREEMENT", "SIGNED", "REF",
                LocalDateTime.parse(createdAt), source);
    }

    private DocumentSnapshot snapshot(Source source, List<UnifiedDocument> docs) throws Exception {
        DocumentSnapshot s = new DocumentSnapshot();
        s.setVin(VIN);
        s.setSource(source);
        s.setPayload(objectMapper.writeValueAsString(docs));
        s.setFetchedAt(LocalDateTime.parse("2026-11-01T10:00:00"));
        return s;
    }
}
