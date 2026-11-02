package keyloop.taing.salesmock.data;

import java.time.LocalDateTime;

public record SalesDoc(
        String documentId,
        String vin,
        String title,
        String documentType,
        String status,
        String referenceNumber,
        LocalDateTime createdAt,
        String filename) {
}
