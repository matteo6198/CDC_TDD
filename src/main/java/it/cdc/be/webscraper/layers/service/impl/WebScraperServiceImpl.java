package it.cdc.be.webscraper.layers.service.impl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import it.cdc.be.webscraper.dto.domain.ScrapedData;
import it.cdc.be.webscraper.dto.domain.Selector;
import it.cdc.be.webscraper.layers.service.WebScraperService;
import it.cdc.be.webscraper.repository.ScraperRepository;
import it.cdc.be.webscraper.repository.entity.ScrapedDataEntity;
import it.cdc.be.webscraper.utils.ScraperUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WebScraperServiceImpl implements WebScraperService {
    private static final Logger logger = LoggerFactory.getLogger(WebScraperServiceImpl.class);

    @Value("#{'${scraper.urls}'.split(';\\s*')}")
    private List<String> urlsToBeScraped;

    @Autowired
    private ScraperRepository scraperRepository;

    @Autowired
    private ScraperUtils scraperUtils;

    private WebClient webClient;

    @PostConstruct
    private void init(){
        webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
    }

    @Override
    public void getNewData() {
        List<ScrapedDataEntity> dataToBeStored = new ArrayList<>();

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

            Selector selector = scraperUtils.getWebsiteSelectorsFromUrl(url);
            List<ScrapedData> scrapedDataList = parsePage(page, selector);

            dataToBeStored.addAll(scrapedDataList.stream().map(d->{
                ScrapedDataEntity entity = new ScrapedDataEntity();

                BeanUtils.copyProperties(d, entity);
                entity.setWebsite(selector.getKey());

                return entity;
            }).collect(Collectors.toList()));

        }

        scraperRepository.saveAll(dataToBeStored);
    }

    @Override
    public List<ScrapedData> getAllData(List<String> filters) {
        List<ScrapedDataEntity> allData = scraperRepository.findAll();
        return allData.stream().map(d->{
            ScrapedData data = new ScrapedData();
            BeanUtils.copyProperties(d, data);

            return data;
        }).collect(Collectors.toList());
    }

    private List<ScrapedData> parsePage(HtmlPage page, Selector type) {
        if(type == null){
            logger.error("Null type");
            return new ArrayList<>();
        }

        List<ScrapedData> scrapedDataList = new ArrayList<>();

        List<HtmlElement> elements = page.getByXPath(type.getItems());
        for(HtmlElement el: elements){
            String title = ((HtmlElement)el.getFirstByXPath(type.getTitle())).getVisibleText();
            String image = null;
            if(type.getImage() != null && !type.getBody().isBlank())
                image = ((HtmlElement)el.getFirstByXPath(type.getImage())).getAttribute("href");

            String link = ((HtmlElement)el.getFirstByXPath(type.getLink())).getAttribute(("href"));
            String date = ((HtmlElement)el.getFirstByXPath(type.getDate())).getVisibleText();
            String body = null;
            if(type.getBody() != null && !type.getBody().isBlank()) {
                body = ((HtmlElement) el.getFirstByXPath(type.getBody())).getVisibleText();
                if(body != null){
                    body = body.substring(0,Math.min(200, body.length()));
                }
            }


            ScrapedData data = new ScrapedData();
            data.setImageUrl(image);
            data.setTitle(title);
            data.setLink(link);
            data.setBody(body);
            data.setDateArticle(LocalDate.parse(date, DateTimeFormatter.ofPattern(type.getDateFormatter())));

            scrapedDataList.add(data);
        }

        return scrapedDataList;
    }
}
