package it.cdc.be.webscraper.layers.controller;

import it.cdc.be.webscraper.dto.request.GetAllDataRequest;
import it.cdc.be.webscraper.dto.response.GetAllDataResponse;
import it.cdc.be.webscraper.exception.ScraperException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public interface WebScraperController {

    @RequestMapping(value = "/scraping/get-all-data", method = RequestMethod.POST)
    ResponseEntity<GetAllDataResponse> getAllData(@RequestBody @Validated GetAllDataRequest request) throws ScraperException;
}
