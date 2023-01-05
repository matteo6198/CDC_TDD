package it.cdc.be.webscraper.layers.service;

import it.cdc.be.webscraper.dto.request.ScrapingServiceRequest;
import it.cdc.be.webscraper.dto.response.GetAllDataResponse;
import it.cdc.be.webscraper.exception.ScraperException;

public interface WebScraperService {
    void getNewData();

    GetAllDataResponse getAllData(ScrapingServiceRequest request) throws ScraperException;

    void clean();
}
