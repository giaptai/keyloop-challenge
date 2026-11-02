package keyloop.taing.documentviewer.data.dto;

import java.time.LocalDateTime;

public record ErrorRes(
        LocalDateTime timestamp,
        int status,
        String error,
        String message) {

}
