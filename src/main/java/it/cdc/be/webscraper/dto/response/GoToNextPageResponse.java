package it.cdc.be.webscraper.dto.response;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class GoToNextPageResponse {
    private HtmlPage newPage;
    private String nextUrl;

    public HtmlPage getNewPage() {
        return newPage;
    }

    public void setNewPage(HtmlPage newPage) {
        this.newPage = newPage;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public void setNextUrl(String nextUrl) {
        this.nextUrl = nextUrl;
    }
}
