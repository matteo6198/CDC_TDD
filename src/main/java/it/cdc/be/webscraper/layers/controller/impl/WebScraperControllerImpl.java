package it.cdc.be.webscraper.layers.controller.impl;

import it.cdc.be.webscraper.dto.request.GetAllDataRequest;
import it.cdc.be.webscraper.dto.response.GetAllDataResponse;
import it.cdc.be.webscraper.exception.ScraperException;
import it.cdc.be.webscraper.layers.controller.WebScraperController;
import it.cdc.be.webscraper.layers.service.WebScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebScraperControllerImpl implements WebScraperController {

    @Autowired
    private WebScraperService webScraperService;

    public ResponseEntity<GetAllDataResponse> getAllData(@RequestBody @Validated GetAllDataRequest request) throws ScraperException{
        GetAllDataResponse response = webScraperService.getAllData(request);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
