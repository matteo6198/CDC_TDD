package it.cdc.be.webscraper.dto.response;

import it.cdc.be.webscraper.dto.domain.ScrapedData;

import java.util.List;

public class ParsePageServiceResponse {
    private List<ScrapedData> scrapedDataList;
    private boolean stopNextPage;

    public List<ScrapedData> getScrapedDataList() {
        return scrapedDataList;
    }

    public void setScrapedDataList(List<ScrapedData> scrapedDataList) {
        this.scrapedDataList = scrapedDataList;
    }

    public boolean isStopNextPage() {
        return stopNextPage;
    }

    public void setStopNextPage(boolean stopNextPage) {
        this.stopNextPage = stopNextPage;
    }
}
