package it.cdc.be.webscraper.layers.service.impl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
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
import jakarta.annotation.Resource;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @Resource(name = "WebsiteSelectorModel")
    private WebsiteSelectorModel websiteSelectorModel;

    private WebClient webClient;

    @PostConstruct
    private void init(){
        webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
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
                    parsePageServiceResponse = scraperUtils.parsePage(page, selector);
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
        int year = -1;
        int month = -1;
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
            if(year <= 0){
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

        if(filters == null || filters.isEmpty()) {
            filters = new ArrayList<>(websiteSelectorModel.getMap().keySet());
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

        LocalDate oldestDate = scraperUtils.getOldestDatePossible();

        scraperRepository.deleteScrapedDataByDateArticle(oldestDate);
    }
}
