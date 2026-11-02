package keyloop.taing.documentviewer.data.dto;

public record SourceStatus(
    Source source,
    String status,
    String message
) {
}
