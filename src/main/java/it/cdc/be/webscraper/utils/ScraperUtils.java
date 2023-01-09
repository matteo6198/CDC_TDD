package it.cdc.be.webscraper.utils;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
import it.cdc.be.webscraper.dto.domain.ScrapedData;
import it.cdc.be.webscraper.dto.domain.Selector;
import it.cdc.be.webscraper.dto.response.GoToNextPageResponse;
import it.cdc.be.webscraper.dto.response.ParsePageServiceResponse;
import it.cdc.be.webscraper.exception.ScraperException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ScraperUtils {
    private static final Logger logger = LoggerFactory.getLogger(ScraperUtils.class);

    @Value("${scraper.date.limit.years}")
    private Long scraperDateLimitYears;

    @Value("${scraper.date.limit.months}")
    private Long scraperDateLimitMonths;

    @Value("${scraper.date.limit.days}")
    private Long scraperDateLimitDays;

    @Resource(name = "WebsiteSelectorModel")
    private WebsiteSelectorModel websiteSelectorModel;

    public Selector getWebsiteSelectorsFromUrl(String url){
        for(String key: websiteSelectorModel.getMap().keySet()){
            Selector s = websiteSelectorModel.getMap().get(key);

            if(s.getUrl().equalsIgnoreCase(url)) {
                logger.debug(s.getItems());
                return s;
            }
        }
        return null;
    }

    public static GoToNextPageResponse goToNextPage(HtmlPage actualPage, Selector selector, Set<String> alreadyVisited){
        GoToNextPageResponse response = new GoToNextPageResponse();
        HtmlElement nextPageLink = actualPage.getFirstByXPath(selector.getNextPage());
        if(nextPageLink == null) {
            // logger.info("Next page link not found for website {}", selector.getKey());
            return response;
        }

        String nextUrl = nextPageLink.getAttribute("href");
        response.setNextUrl(nextUrl);
        if(alreadyVisited.contains(nextUrl)){
            return response;
        }

        HtmlPage newPage;
        try {
            newPage = nextPageLink.click();
            logger.debug("{}", nextUrl);
        }catch (Exception e){
            logger.info("finished website {} on url {}", selector.getKey(), actualPage.getUrl(), e);
            return response;
        }

        response.setNewPage(newPage);
        return response;
    }

    public boolean isWebsiteFilterValid(@Nonnull String filter){
        return websiteSelectorModel.getMap().containsKey(filter);
    }

    public ParsePageServiceResponse parsePage(HtmlPage page, Selector type) throws ScraperException {
        if(type == null){
            logger.error("Null type");
            throw new ScraperException();
        }

        ParsePageServiceResponse response = new ParsePageServiceResponse();
        response.setStopNextPage(false);
        final LocalDate oldestDate = LocalDate.now()
                .minusYears(scraperDateLimitYears)
                .minusMonths(scraperDateLimitMonths)
                .minusDays(scraperDateLimitDays)
                .withDayOfMonth(1)
                .minusDays(1);

        List<ScrapedData> scrapedDataList = new ArrayList<>();
        String baseUrl = Arrays.stream(type.getUrl().split("/")).limit(3).collect(Collectors.joining("/"));

        List<HtmlElement> elements = page.getByXPath(type.getItems());
        for(HtmlElement el: elements){
            if(el.getVisibleText().isBlank())
                continue;

            try {
                ScrapedData data = new ScrapedData();

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

                String body = null;
                if (type.getBody() != null && !type.getBody().isBlank()) {
                    logger.debug("url: {}, xpath: {}, element: {}", type.getUrl(), type.getBody(), el.getVisibleText());
                    body = ((HtmlElement) el.getFirstByXPath(type.getBody())).getVisibleText().trim().replaceFirst("read more$","").replaceFirst("more$","").trim();
                    body = body.substring(0, Math.min(200, body.length()));
                }
                String category = null;
                if (type.getCategory() != null && !type.getCategory().isBlank()) {
                    HtmlElement categoryElement = el.getFirstByXPath(type.getCategory());
                    if(categoryElement != null)
                        category = categoryElement.getVisibleText().trim();
                }

                data.setImageUrl(image);
                data.setTitle(title);
                data.setLink(link);
                data.setBody(body);
                data.setCategory(category);

                String date = ((HtmlElement) el.getFirstByXPath(type.getDate())).getVisibleText().trim();
                if (!date.isBlank()) {
                    try {
                        DateFormat format = new SimpleDateFormat(type.getDateFormatter(), Locale.forLanguageTag(type.getDateLocale()));
                        LocalDate dateArticle = format.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        data.setDateArticle(dateArticle);

                        if(!dateArticle.isAfter(oldestDate)){
                            logger.info("skipping site {} -> {}", type.getKey(), data);
                            response.setStopNextPage(true);
                            break;
                        }
                    } catch (Exception e) {
                        logger.error("Can't parse date: {}, formatter: {}", date, type.getDateFormatter());
                        continue;
                    }
                }else {
                    logger.error("Date not found");
                    continue;
                }

                scrapedDataList.add(data);
            }catch (Exception e){
                e.printStackTrace();
                logger.debug("Can't parse: {}", el.asXml(), e);
            }
        }

        response.setScrapedDataList(scrapedDataList);
        return response;
    }

    public LocalDate getOldestDatePossible(){
        return LocalDate.now()
                .minusYears(scraperDateLimitYears)
                .minusMonths(scraperDateLimitMonths)
                .minusDays(scraperDateLimitDays)
                .withDayOfMonth(1)
                .minusDays(1);
    }
}
