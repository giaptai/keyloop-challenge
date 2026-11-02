package keyloop.taing.servicemock.business;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import keyloop.taing.servicemock.data.ServiceDoc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ServiceBusiness {
    private final String SERVICE_MOCK_FILE = "mock-data/service-documents.json";
    private final Map<String, List<ServiceDoc>> services;

    public ServiceBusiness(ObjectMapper objectMapper) throws IOException {
        try (InputStream ip = new ClassPathResource(SERVICE_MOCK_FILE).getInputStream()) {
            List<ServiceDoc> servicesMock = objectMapper.readValue(
                    ip,
                    new TypeReference<List<ServiceDoc>>() {});
            services = servicesMock.stream().collect(Collectors.groupingBy(ServiceDoc::vin));
        }
    }

    public List<ServiceDoc> getDocuments(String vin) {
        return services.getOrDefault(vin, List.of());
    }
}
