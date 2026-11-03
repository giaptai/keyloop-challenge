package keyloop.taing.documentviewer.data.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

public record UnifiedDocument(
		String documentId,
		String vin,
		String title,
		String documentType,
		String status,
		String referenceNumber,
		LocalDateTime createdAt,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		String filename,
		Source source) {
}
