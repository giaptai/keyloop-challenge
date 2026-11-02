package keyloop.taing.salesmock.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import keyloop.taing.salesmock.business.SalesBusiness;
import keyloop.taing.salesmock.data.SalesDoc;

import java.util.List;

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
    public List<SalesDoc> getVin(@PathVariable(name = "vin") String vin) {
        return salesService.getDocuments(vin);
        // throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sales API is unavailable");
    }
}
