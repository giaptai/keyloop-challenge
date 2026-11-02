package keyloop.taing.documentviewer.data.dto;

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
