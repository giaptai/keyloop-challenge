package keyloop.taing.servicemock.data;

import java.time.LocalDateTime;

public record ServiceDoc(
        String documentId,
        String vin,
        String title,
        String documentType,
        String status,
        String referenceNumber,
        LocalDateTime createdAt) {
}
