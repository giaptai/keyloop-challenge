package keyloop.taing.documentviewer.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import keyloop.taing.documentviewer.data.dto.SalesDoc;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;

@Component
public class SalesClient extends AbstractDocumentClient<SalesDoc> {

    public SalesClient(RestClient.Builder builder, @Value("${sales.api.base-url}") String baseUrl) {
        super(builder, baseUrl);
    }

    @Override
    protected String path() {
        return "/api/sales/vehicles/{vin}/documents";
    }

    @Override
    protected ParameterizedTypeReference<List<SalesDoc>> responseType() {
        return new ParameterizedTypeReference<List<SalesDoc>>() {
        };
    }
}
