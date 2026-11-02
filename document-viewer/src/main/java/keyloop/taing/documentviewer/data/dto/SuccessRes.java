package keyloop.taing.documentviewer.data.dto;

import java.util.List;

public record SuccessRes(
    String vin,
    List<UnifiedDocument> documents,
    List<SourceStatus> sources
) {
    
}
