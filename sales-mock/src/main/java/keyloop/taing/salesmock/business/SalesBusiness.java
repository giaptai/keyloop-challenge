package keyloop.taing.salesmock.business;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import keyloop.taing.salesmock.data.SalesDoc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalesBusiness {
    private final String SALES_MOCK_FILE = "mock-data/sales-documents.json";
    private final Map<String, List<SalesDoc>> sales;

    public SalesBusiness(ObjectMapper objectMapper) throws IOException {
        try (InputStream ip = new ClassPathResource(SALES_MOCK_FILE).getInputStream()) {
            List<SalesDoc> salesMock = objectMapper.readValue(ip, new TypeReference<List<SalesDoc>>() {});
            sales = salesMock.stream().collect(Collectors.groupingBy(t -> t.vin()));
        }
    }

    public List<SalesDoc> getDocuments(String vin){
        return sales.getOrDefault(vin, List.of());
    }
}
