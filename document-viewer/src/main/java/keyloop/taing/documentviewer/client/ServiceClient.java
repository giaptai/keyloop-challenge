package keyloop.taing.documentviewer.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import keyloop.taing.documentviewer.data.dto.ServiceDoc;

@Component
public class ServiceClient extends AbstractDocumentClient<ServiceDoc> {
    public ServiceClient(RestClient.Builder builder, @Value("${service.api.base-url}") String baseUrl) {
        super(builder, baseUrl);
    }

    @Override
    protected String path() {
        return "/api/service/vehicles/{vin}/documents";
    }

    @Override
    protected ParameterizedTypeReference<List<ServiceDoc>> responseType() {
        return new ParameterizedTypeReference<List<ServiceDoc>>() {
        };
    }
}
