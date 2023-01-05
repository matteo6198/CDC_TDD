package it.cdc.be.webscraper.dto.request;

import it.cdc.be.webscraper.dto.domain.Pagination;

import java.util.List;

public class ScrapingServiceRequest {
    private List<String> websiteFilter;
    private Pagination pagination;

    public List<String> getWebsiteFilter() {
        return websiteFilter;
    }

    public void setWebsiteFilter(List<String> websiteFilter) {
        this.websiteFilter = websiteFilter;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
