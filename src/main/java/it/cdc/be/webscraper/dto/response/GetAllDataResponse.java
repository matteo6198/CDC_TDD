package it.cdc.be.webscraper.dto.response;

import it.cdc.be.webscraper.dto.domain.Pagination;
import it.cdc.be.webscraper.dto.domain.ScrapedData;

import java.util.List;

public class GetAllDataResponse {
    private List<ScrapedData> scrapedDataList;
    private Pagination pagination;

    public List<ScrapedData> getScrapedDataList() {
        return scrapedDataList;
    }

    public void setScrapedDataList(List<ScrapedData> scrapedDataList) {
        this.scrapedDataList = scrapedDataList;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
