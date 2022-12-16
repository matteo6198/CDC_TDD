package it.cdc.be.webscraper.layers.service;

import it.cdc.be.webscraper.dto.domain.ScrapedData;

import java.util.List;

public interface WebScraperService {
    void getNewData();

    List<ScrapedData> getAllData();
}
