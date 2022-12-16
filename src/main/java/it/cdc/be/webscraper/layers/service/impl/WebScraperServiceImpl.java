package it.cdc.be.webscraper.layers.service.impl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import it.cdc.be.webscraper.dto.domain.ScrapedData;
import it.cdc.be.webscraper.dto.domain.WebsiteType;
import it.cdc.be.webscraper.layers.service.WebScraperService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WebScraperServiceImpl implements WebScraperService {
    private static final Logger logger = LoggerFactory.getLogger(WebScraperServiceImpl.class);

    @Value("#{'${scraper.urls}'.split(';\\s*')}")
    private List<String> urlsToBeScraped;

    private WebClient webClient;

    @PostConstruct
    private void init(){
        webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
    }

    @Override
    public void getNewData() {
        for(String url:urlsToBeScraped){
            logger.debug("Scrpaing url: " + url);

            HtmlPage page;
            try {
                page = webClient.getPage(url);
            }catch (Exception e){
                logger.error("Error while parsing response of url: {}", url, e);
                continue;
            }

            if(page == null){
                logger.error("Empty page for url {}", url);
                continue;
            }

            List<ScrapedData> scrapedDataList = parsePage(page, WebsiteType.getFromUrl(url));

            // TODO save data somewhere
        }
    }

    @Override
    public List<ScrapedData> getAllData() {
        return null;
    }

    private List<ScrapedData> parsePage(HtmlPage page, WebsiteType type) {
        if(type == null){
            logger.error("Null type");
            return new ArrayList<>();
        }

        List<ScrapedData> scrapedDataList = new ArrayList<>();

        List<HtmlElement> elements = page.getByXPath(type.getItemsSelector());
        for(HtmlElement el: elements){
            String link = el.getFirstByXPath(type.getLinkSelector());
            String title = el.getFirstByXPath(type.getTitleSelector());
            String image = el.getFirstByXPath(type.getImageSelector());

            ScrapedData data = new ScrapedData();
            data.setImageUrl(image);
            data.setTitle(title);
            data.setLink(link);

            scrapedDataList.add(data);
        }

        return scrapedDataList;
    }
}
