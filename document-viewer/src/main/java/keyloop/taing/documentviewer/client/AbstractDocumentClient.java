package keyloop.taing.documentviewer.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

public abstract class AbstractDocumentClient<T> {

    private final RestClient restClient;

    public AbstractDocumentClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public final List<T> fetchDocuments(String vin) {
        return restClient.get().uri(path(), vin).retrieve().body(responseType());
    }

    protected abstract String path();

    protected abstract ParameterizedTypeReference<List<T>> responseType();
}
