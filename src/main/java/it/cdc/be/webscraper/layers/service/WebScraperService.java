package it.cdc.be.webscraper.layers.service;

import it.cdc.be.webscraper.dto.request.GetAllDataRequest;
import it.cdc.be.webscraper.dto.response.GetAllDataResponse;
import it.cdc.be.webscraper.exception.ScraperException;

public interface WebScraperService {
    void getNewData();

    GetAllDataResponse getAllData(GetAllDataRequest request) throws ScraperException;

    void clean();
}
