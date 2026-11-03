package keyloop.taing.salesmock.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import keyloop.taing.salesmock.business.SalesBusiness;
import keyloop.taing.salesmock.data.SalesDoc;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping(path = "/api/sales/vehicles")
public class SalesMockController {
    private final SalesBusiness salesService;

    public SalesMockController(SalesBusiness salesService) {
        this.salesService = salesService;
    }

    @GetMapping(path = "/{vin}/documents")
    public ResponseEntity<List<SalesDoc>> getVin(@PathVariable(name = "vin") String vin) {
        if(!salesService.vehicleExists(vin)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VIN not found in sales system.");
        }
        return new ResponseEntity<>(salesService.getDocuments(vin), HttpStatus.OK) ;
    }
}
