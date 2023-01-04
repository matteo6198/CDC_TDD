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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
        List<ScrapedDataEntity> scrapedDataEntities = new ArrayList<>();

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

            scrapedDataEntities.addAll(scrapedDataList.stream().map(d->{
                ScrapedDataEntity entity = new ScrapedDataEntity();

                BeanUtils.copyProperties(d, entity);
                entity.setWebsite(selector.getKey());

                return entity;
            }).collect(Collectors.toList()));

        }

        // get all data present on db
        List<ScrapedDataEntity> alreadyStored = scraperRepository.findAll();
        List<String> urlsToBeIgnored = alreadyStored.stream()
                .map(d-> d.getLink() + d.getTitle() + d.getWebsite())
                .collect(Collectors.toList());

        // filter out already present data
        List<ScrapedDataEntity> dataToBeStored = scrapedDataEntities.stream()
                .filter(d -> {
                    String key = d.getLink() + d.getTitle() + d.getWebsite();
                    return !urlsToBeIgnored.contains(key);
                }).distinct()
                .collect(Collectors.toList());

        scraperRepository.saveAll(dataToBeStored);
    }

    @Override
    public List<ScrapedData> getAllData(List<String> filters) {
        List<ScrapedDataEntity> allData;
        if(filters == null || filters.isEmpty())
            allData = scraperRepository.findScrapedDataOrderedByDateArticle();
        else
            allData = scraperRepository.findScrapedDataByWebsite(filters);
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

        String baseUrl = Arrays.stream(type.getUrl().split("/")).limit(3).collect(Collectors.joining("/"));

        List<HtmlElement> elements = page.getByXPath(type.getItems());
        for(HtmlElement el: elements){
            if(el.getVisibleText().isBlank())
                continue;

            try {
                logger.debug(el.getVisibleText().replace('\n', ';'));
                String title = ((HtmlElement) el.getFirstByXPath(type.getTitle())).getVisibleText().trim();
                String link = ((HtmlElement) el.getFirstByXPath(type.getLink())).getAttribute(("href")).trim();
                if(!link.isBlank() && link.matches("^/.*")){
                    logger.debug("link: {}, base: {}", link, type.getUrl());
                    link = baseUrl + link;
                }

                String image = null;
                if (type.getImage() != null && !type.getImage().isBlank()) {
                    if (!el.getAttribute("data-back").isBlank()) {
                        image = el.getAttribute("data-back");
                    } else {
                        HtmlElement imgElement = el.getFirstByXPath(type.getImage());
                        if (imgElement != null) {
                            image = imgElement.getAttribute("src").trim();
                            if (image.isBlank()) {
                                // only for blog.osservatori
                                image = imgElement.getAttribute("style")
                                        .trim()
                                        .replace("background-image:url(", "")
                                        .replace(")", "");
                            }
                            if (!image.isBlank() && image.startsWith("data:image")) {
                                image = imgElement.getAttribute("data-src");
                            }

                            if (!image.isBlank() && image.matches("^/.*")) {
                                image = baseUrl + image;
                            }
                            logger.debug("{} -> {}", type.getKey(), image);
                        }
                    }
                }


                String date = null;
                if (type.getDate() != null && !type.getDate().isBlank())
                    date = ((HtmlElement) el.getFirstByXPath(type.getDate())).getVisibleText().trim();

                String body = null;
                if (type.getBody() != null && !type.getBody().isBlank()) {
                    logger.debug("url: {}, xpath: {}, element: {}", type.getUrl(), type.getBody(), el.getVisibleText());
                    body = ((HtmlElement) el.getFirstByXPath(type.getBody())).getVisibleText().trim().replaceFirst("read more$","").replaceFirst("more$","").trim();
                    body = body.substring(0, Math.min(200, body.length()));
                }
                String category = null;
                if (type.getCategory() != null && !type.getCategory().isBlank()) {
                    category = ((HtmlElement) el.getFirstByXPath(type.getCategory())).getVisibleText().trim();
                }


                ScrapedData data = new ScrapedData();
                data.setImageUrl(image);
                data.setTitle(title);
                data.setLink(link);
                data.setBody(body);
                data.setCategory(category);
                logger.debug("link: {}, image: {}", link, image);
                LocalDate dateArticle = null;
                if (date != null) {
                    try {
                        DateFormat format = new SimpleDateFormat(type.getDateFormatter(), Locale.forLanguageTag(type.getDateLocale()));
                        dateArticle = format.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    } catch (Exception e) {
                        logger.error("date: {}, formatter: {}", date, type.getDateFormatter());
                        dateArticle = LocalDate.now();
                    }
                }

                data.setDateArticle(dateArticle);

                scrapedDataList.add(data);
            }catch (Exception e){
                logger.error("Can't parse: {}", el.asXml(), e);
            }
        }

        return scrapedDataList;
    }
}
