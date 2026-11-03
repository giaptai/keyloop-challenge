package keyloop.taing.servicemock.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import keyloop.taing.servicemock.business.ServiceBusiness;
import keyloop.taing.servicemock.data.ServiceDoc;

@RestController
@RequestMapping(path = "/api/service/vehicles")
public class ServiceMockController {
    private final ServiceBusiness business;

    public ServiceMockController(ServiceBusiness business) {
        this.business = business;
    }

    @GetMapping(path = "/{vin}/documents")
    public ResponseEntity<List<ServiceDoc>> getVin(@PathVariable(name = "vin") String vin) {
        if(!business.vehicleExists(vin)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VIN not found in service system.");
        }
        return new ResponseEntity<>(business.getDocuments(vin), HttpStatus.OK);
    }
}
