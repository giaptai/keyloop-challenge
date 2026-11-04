package keyloop.taing.documentviewer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import keyloop.taing.documentviewer.business.DocumentViewerBusiness;
import keyloop.taing.documentviewer.data.dto.ErrorRes;
import keyloop.taing.documentviewer.data.dto.SuccessRes;

@RestController
@RequestMapping(path = "/api/v1/documents")
public class DocumentViewerController {
    private final DocumentViewerBusiness business;

    public DocumentViewerController(DocumentViewerBusiness business) {
        this.business = business;
    }

    @Operation(summary = "Search vehicle documents", description = "Searches and aggregates documents from Sales and Service systems using a VIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully", content = @Content(schema = @Schema(implementation = SuccessRes.class))),
            @ApiResponse(responseCode = "400", description = "Invalid VIN", content = @Content(schema = @Schema(implementation = ErrorRes.class))),
            @ApiResponse(responseCode = "404", description = "VIN Not Found", content = @Content(schema = @Schema(implementation = ErrorRes.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorRes.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorRes.class)))
    })
    @GetMapping(path = "")
    public SuccessRes getDocument(@RequestParam(name = "vin") String vin) {
        return business.search(vin);
    }
}
