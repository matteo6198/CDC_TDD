package it.cdc.be.webscraper.layers.service.impl;

import com.gargoylesoftware.htmlunit.WebClient;
import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
import it.cdc.be.webscraper.dto.request.GetAllDataRequest;
import it.cdc.be.webscraper.dto.response.GetAllDataResponse;
import it.cdc.be.webscraper.exception.ScraperException;
import it.cdc.be.webscraper.layers.service.WebScraperService;
import it.cdc.be.webscraper.utils.ScraperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Service
public class WebScraperServiceImpl implements WebScraperService {
    private static final Logger logger = LoggerFactory.getLogger(WebScraperServiceImpl.class);

    @Value("#{'${scraper.urls}'.split(';\\s*')}")
    private List<String> urlsToBeScraped;

    //@Autowired
    //private ScraperRepository scraperRepository;

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
        /*List<ScrapedDataEntity> scrapedDataEntities = new ArrayList<>();

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
        // filter out already present data
        scrapedDataEntities.removeAll(alreadyStored);
        // remove duplicates
        List<ScrapedDataEntity> dataToBeStored = scrapedDataEntities.stream()
                .distinct()
                .collect(Collectors.toList());

        scraperRepository.saveAll(dataToBeStored);*/
    }

    @Override
    public GetAllDataResponse getAllData(@NotNull GetAllDataRequest request) throws ScraperException {
        GetAllDataResponse response = new GetAllDataResponse();
        /*Page<ScrapedDataEntity> allData;
        List<String> filters = request.getWebsiteFilter();
        if(filters != null && !filters.stream().allMatch(el->scraperUtils.isWebsiteFilterValid(el))){
            logger.error("Invalid filters");
            throw new ScraperException();
        }
        // check date
        String requestMonth = request.getMonth();
        if(requestMonth != null){
            if(!requestMonth.matches("^\\d{4}-((0[1-9])|(1[12]))$")){
                logger.error("Wrong month format {}", requestMonth);
                throw new ScraperException();
            }

            String oldestDateString = scraperUtils.getOldestDatePossible().format(ScraperUtils.DATE_YEAR_MONTH_FORMATTER);
            if(oldestDateString.compareTo(requestMonth) > 0){
                logger.error("Date too old {}", requestMonth);
                throw new ScraperException();
            }
        } else{
            requestMonth = "-1";
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
        allData = scraperRepository.findScrapedDataByWebsite(filters, requestMonth, pagination);


        List<ScrapedData> retrievedData = allData.stream()
                .map(d->{
                    ScrapedData data = new ScrapedData();
                    BeanUtils.copyProperties(d, data);
                    return data;
                }).collect(Collectors.toList());

        response.setScrapedDataList(retrievedData);

        Pagination paginationOut = new Pagination();
        if(request.getPagination() != null){
            BeanUtils.copyProperties(request.getPagination(), paginationOut);
        }else{
            paginationOut.setPageNumber(0);
            paginationOut.setPageLength(Integer.MAX_VALUE);
        }
        paginationOut.setTotalPages(allData.getTotalPages());
*/
        //response.setPagination(paginationOut);
        return response;
    }

    @Override
    @Scheduled(cron = "${scraper.scheduled.clean}")
    public void clean() {
        logger.info("Database clean");

        LocalDate oldestDate = scraperUtils.getOldestDatePossible();

        //scraperRepository.deleteScrapedDataByDateArticle(oldestDate);
    }
}
