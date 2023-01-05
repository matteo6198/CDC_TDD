package it.cdc.be.webscraper.layers.service.impl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import it.cdc.be.webscraper.dto.domain.Pagination;
import it.cdc.be.webscraper.dto.domain.ScrapedData;
import it.cdc.be.webscraper.dto.domain.Selector;
import it.cdc.be.webscraper.dto.request.GetAllDataRequest;
import it.cdc.be.webscraper.dto.response.GetAllDataResponse;
import it.cdc.be.webscraper.dto.response.GoToNextPageResponse;
import it.cdc.be.webscraper.dto.response.ParsePageServiceResponse;
import it.cdc.be.webscraper.exception.ScraperException;
import it.cdc.be.webscraper.layers.service.WebScraperService;
import it.cdc.be.webscraper.repository.ScraperRepository;
import it.cdc.be.webscraper.repository.entity.ScrapedDataEntity;
import it.cdc.be.webscraper.utils.ScraperUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WebScraperServiceImpl implements WebScraperService {
    private static final Logger logger = LoggerFactory.getLogger(WebScraperServiceImpl.class);

    @Value("#{'${scraper.urls}'.split(';\\s*')}")
    private List<String> urlsToBeScraped;

    @Value("${scraper.date.limit.years}")
    private Long scraperDateLimitYears;

    @Value("${scraper.date.limit.months}")
    private Long scraperDateLimitMonths;

    @Value("${scraper.date.limit.days}")
    private Long scraperDateLimitDays;

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
    @Scheduled(cron = "${scraper.scheduled.getNewData}")
    @org.springframework.context.event.EventListener(ApplicationReadyEvent.class)
    public void getNewData() {
        logger.info("Scraping new data");
        List<ScrapedDataEntity> scrapedDataEntities = new ArrayList<>();

        for(String url:urlsToBeScraped){
            long cnt = 0;
            logger.debug("Scrpaing url: " + url);
            final Selector selector = scraperUtils.getWebsiteSelectorsFromUrl(url);

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

            Set<String> alreadyVisited = new HashSet<>();
            alreadyVisited.add(page.getUrl().toString());

            while(true) {
                ParsePageServiceResponse parsePageServiceResponse;
                try {
                    parsePageServiceResponse = parsePage(page, selector);
                }catch (ScraperException e){
                    logger.error("Error while scraping website {}", selector.getKey());
                    break;
                }

                List<ScrapedData> scrapedDataList = parsePageServiceResponse.getScrapedDataList();

                scrapedDataEntities.addAll(scrapedDataList.stream().map(d -> {
                    ScrapedDataEntity entity = new ScrapedDataEntity();

                    BeanUtils.copyProperties(d, entity);
                    entity.setWebsite(selector.getKey());

                    return entity;
                }).collect(Collectors.toList()));

                if(parsePageServiceResponse.isStopNextPage())
                    break;

                GoToNextPageResponse toNextPageResponse = ScraperUtils.goToNextPage(page, selector, alreadyVisited);
                alreadyVisited.add(toNextPageResponse.getNextUrl());
                page = toNextPageResponse.getNewPage();

                cnt++;
                if(page == null){
                    break;
                }
            }
            logger.info("Scraped {} pages for website {}", cnt, selector.getKey());
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

    private ParsePageServiceResponse parsePage(HtmlPage page, Selector type) throws ScraperException {
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

    @Override
    public GetAllDataResponse getAllData(@Nonnull GetAllDataRequest request) throws ScraperException {
        Page<ScrapedDataEntity> allData;
        List<String> filters = request.getWebsiteFilter();
        if(filters != null && !filters.stream().allMatch(el->scraperUtils.isWebsiteFilterValid(el))){
            logger.error("Invalid filters");
            throw new ScraperException();
        }
        // check date
        final String requestMonth = request.getMonth();
        Integer year = null;
        Integer month = null;
        if(requestMonth != null){
            if(!requestMonth.matches("^\\d{4}-[01]\\d$")){
                logger.error("Wrong month format");
                throw new ScraperException();
            }
            String[] fields = requestMonth.split("-");
            year = Integer.parseInt(fields[0]);
            month = Integer.parseInt(fields[1]);
            if(month > 12 || month <= 0){
                logger.error("Invalid month {}", month);
                throw new ScraperException();
            }
        }

        Pageable pagination;
        if(request.getPagination() == null){
            pagination = PageRequest.of(0, Integer.MAX_VALUE);
        }else{
            pagination = PageRequest.of(request.getPagination().getPageNumber(), request.getPagination().getPageLength());
        }

        if(filters != null && filters.isEmpty()) {
            filters = null;
        }
        allData = scraperRepository.findScrapedDataByWebsite(filters, year, month, pagination);


        List<ScrapedData> retrievedData = allData.stream()
                .map(d->{
                    ScrapedData data = new ScrapedData();
                    BeanUtils.copyProperties(d, data);
                    return data;
                }).collect(Collectors.toList());

        GetAllDataResponse response = new GetAllDataResponse();
        response.setScrapedDataList(retrievedData);

        Pagination paginationOut = new Pagination();
        if(request.getPagination() != null){
            BeanUtils.copyProperties(request.getPagination(), paginationOut);
        }else{
            paginationOut.setPageNumber(0);
            paginationOut.setPageLength(Integer.MAX_VALUE);
        }
        paginationOut.setTotalPages(allData.getTotalPages());

        response.setPagination(paginationOut);
        return response;
    }

    @Override
    @Scheduled(cron = "${scraper.scheduled.clean}")
    public void clean() {
        logger.info("Database clean");

        LocalDate oldestDate = LocalDate.now()
                        .minusYears(scraperDateLimitYears)
                        .minusMonths(scraperDateLimitMonths)
                        .minusDays(scraperDateLimitDays)
                        .withDayOfMonth(1)
                        .minusDays(1);

        scraperRepository.deleteScrapedDataByDateArticle(oldestDate);
    }
}
