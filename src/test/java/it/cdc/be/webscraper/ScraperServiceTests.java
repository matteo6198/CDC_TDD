package it.cdc.be.webscraper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer;
import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
import it.cdc.be.webscraper.dto.domain.Pagination;
import it.cdc.be.webscraper.dto.domain.ScrapedData;
import it.cdc.be.webscraper.dto.request.GetAllDataRequest;
import it.cdc.be.webscraper.dto.response.GetAllDataResponse;
import it.cdc.be.webscraper.exception.ScraperException;
import it.cdc.be.webscraper.layers.service.WebScraperService;
import it.cdc.be.webscraper.repository.ScraperRepository;
import it.cdc.be.webscraper.repository.entity.ScrapedDataEntity;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ScraperServiceTests extends AbstractTestNGSpringContextTests {
    private static final WireMockServer wireMockServer = new WireMockServer(options().port(9000).enableBrowserProxying(true));
    private static final int LIMIT_DATE_SCRAPER_YEARS = 3;
    private static final int LIMIT_DATE_SCRAPER_MONTHS = 0;
    private static final int LIMIT_DATE_SCRAPER_DAYS = 0;

    @Autowired
    WebScraperService scrapingService;

    @Autowired
    ScraperRepository scraperRepository;

    @SuppressWarnings("unused")
    @Resource(name = "WebsiteSelectorModel")
    private WebsiteSelectorModel websiteSelectorModel;

    @SuppressWarnings("rawtypes")
    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest");

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry){
        wireMockServer.start();
        configureFor("localhost", 9000);

        // override variables in properties for tests
        registry.add("scraper.urls", ()->"http://localhost:9000/it_it/tag/innovative-payments;http://localhost:9000/payment-innovation;http://localhost:9000/payment-services;http://localhost:9000/mobile-app;http://localhost:9000/digital-economy;http://localhost:9000/sez/tecnologia/fintech;http://localhost:9000/category/mobile-payments;");
        registry.add("website.map.blog_osservatori.url", ()->"http://localhost:9000/it_it/tag/innovative-payments");
        registry.add("website.map.pagamentidigitali_innovation.url", ()->"http://localhost:9000/payment-innovation");
        registry.add("website.map.pagamentidigitali_services.url", ()->"http://localhost:9000/payment-services");
        registry.add("website.map.pagamentidigitali_mobile.url", ()->"http://localhost:9000/mobile-app");
        registry.add("website.map.corriere.url", ()->"http://localhost:9000/digital-economy");
        registry.add("website.map.sole24ore.url", ()->"http://localhost:9000/sez/tecnologia/fintech");
        registry.add("website.map.paymentscardsandmobile.url", ()->"http://localhost:9000/category/mobile-payments");

        registry.add("scraper.date.limit.years", ()->LIMIT_DATE_SCRAPER_YEARS);
        registry.add("scraper.date.limit.months", ()->LIMIT_DATE_SCRAPER_MONTHS);
        registry.add("scraper.date.limit.days", ()->LIMIT_DATE_SCRAPER_DAYS);

        // database set
        postgreSQLContainer.start();
        System.out.println(postgreSQLContainer.getJdbcUrl());
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driverClassName", ()->"org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", ()->"create-drop");
    }

    @BeforeClass()
    void init() throws IOException {
        JvmProxyConfigurer.configureFor(wireMockServer);
        // init wiremock to provide sample data
        WireMock.stubFor(WireMock.get("/it_it/tag/innovative-payments").willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite1.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/payment-innovation").willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite2.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/payment-services").willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite3.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/mobile-app").willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite4.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/digital-economy").willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite5.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/sez/tecnologia/fintech").willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite6.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/archivi/tecnologia/fintech/1").willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite6_1.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/category/mobile-payments").willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite7.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
    }

    @Test()
    void checkScrapingServiceExists(){
        Assert.assertNotNull(scrapingService);
    }

    @Test(dependsOnMethods = { "checkScrapingServiceExists" })
    void checkScrapingWorks(){
        // ensure that the service can get new data
        try{
            scrapingService.getNewData();
        }catch (Exception e){
            logger.error("Exception: ", e);
            Assert.fail("Error while testing scraping new data", e);
        }

        // ensure files are saved
        List<ScrapedDataEntity> allData = scraperRepository.findAll();
        Assert.assertNotNull(allData);
        Assert.assertFalse(allData.isEmpty());
        Assert.assertTrue(allData.stream()
                .allMatch(d ->
                        d.getLink().matches("^http[s]?://.*") &&
                                (d.getImageUrl() == null || d.getImageUrl().matches("^http[s]?://.*")) &&
                                d.getTitle() != null && !d.getTitle().isBlank() && d.getWebsite() != null &&
                                d.getDateArticle() != null
                        ));
    }

    @Test(dependsOnMethods = {"checkScrapingWorks", "checkRetrieveWorks"})
    void checkServiceDoesNotDuplicateData(){
        scraperRepository.deleteAll();
        Assert.assertEquals(scraperRepository.count(), 0);

        List<ScrapedData> scrapedDataList = null;
        GetAllDataRequest request = new GetAllDataRequest();
        // first get
        try{
            scrapingService.getNewData();

            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getScrapedDataList());
            scrapedDataList = response.getScrapedDataList();
        }catch (Exception e){
            logger.error("Exception: ", e);
            Assert.fail("Error while testing scraping new data", e);
        }
        Assert.assertNotNull(scrapedDataList);

        // second get
        List<ScrapedData> secondRetrieveData = null;
        try{
            scrapingService.getNewData();

            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getScrapedDataList());
            secondRetrieveData = response.getScrapedDataList();
        }catch (Exception e){
            logger.error("Exception: ", e);
            Assert.fail("Error while testing scraping new data", e);
        }
        Assert.assertNotNull(secondRetrieveData);

        // check no data has been added
        Assert.assertEquals(scrapedDataList.size(), secondRetrieveData.size());

        for(ScrapedData d: scrapedDataList){
            long count = secondRetrieveData.stream()
                    .filter(s->s.getLink().equals(d.getLink()) && s.getTitle().equals(d.getTitle()) && s.getWebsite().equals(d.getWebsite()))
                    .count();

            if(count != 1)
                logger.error("Duplicate data found: " + d.getTitle() + " ,link:" + d.getLink());
            Assert.assertEquals(count, 1L);
        }
    }

    @Test(dependsOnMethods = {"checkScrapingWorks"})
    void checkRetrieveWorks(){
        GetAllDataRequest request = new GetAllDataRequest();
        scraperRepository.deleteAll();
        Assert.assertEquals(scraperRepository.count(), 0);

        List<ScrapedData> scrapedDataList = null;
        GetAllDataResponse response = null;
        try{
            response = scrapingService.getAllData(request);
            scrapedDataList = response.getScrapedDataList();
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getScrapedDataList());
        Assert.assertNotNull(response.getPagination());
        Assert.assertEquals(response.getPagination().getPageNumber(), 0);
        Assert.assertEquals(response.getPagination().getPageLength(), Integer.MAX_VALUE);
        Assert.assertEquals(response.getPagination().getTotalPages(), 0);
        Assert.assertNotNull(scrapedDataList);
        Assert.assertEquals(scrapedDataList.size(), 0);

        // ensure that the data retrieved are stored
        scrapingService.getNewData();
        Assert.assertTrue(scraperRepository.count() > 0);

        try{
            response = scrapingService.getAllData(request);
            scrapedDataList = response.getScrapedDataList();
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }
        Assert.assertNotNull(response.getPagination());
        Assert.assertEquals(response.getPagination().getPageNumber(), 0);
        Assert.assertEquals(response.getPagination().getPageLength(), Integer.MAX_VALUE);
        Assert.assertEquals(response.getPagination().getTotalPages(), 1);
        Assert.assertNotNull(scrapedDataList);
        Assert.assertTrue(scrapedDataList.size() > 0);
        Assert.assertEquals(scrapedDataList.size(), scraperRepository.count());
        checkOrder(scrapedDataList);

        // check pagination works
        try{
            Pagination pagination = new Pagination();
            pagination.setPageNumber(0);
            pagination.setPageLength(10);

            request.setPagination(pagination);
            response = scrapingService.getAllData(request);
            scrapedDataList = response.getScrapedDataList();
        }catch (Exception e){
            Assert.fail("Error while retrieving data", e);
        }
        Assert.assertNotNull(response.getPagination());
        Assert.assertEquals(response.getPagination().getPageNumber(), 0);
        Assert.assertEquals(response.getPagination().getPageLength(), 10);
        Assert.assertTrue(response.getPagination().getTotalPages() > 1);
        Assert.assertEquals(scrapedDataList.size(), 10);
    }

    private void checkOrder(List<ScrapedData> scrapedDataList){
        if(scrapedDataList == null || scrapedDataList.isEmpty())
            return;

        Iterator<ScrapedData> iter = scrapedDataList.stream().filter(el->el.getDateArticle()!=null).iterator();
        if(!iter.hasNext())
            return;

        ScrapedData current, previous = iter.next();
        while (iter.hasNext()){
            current = iter.next();
            if(current.getDateArticle() != null && current.getDateArticle().isAfter(previous.getDateArticle())){
                Assert.fail("Retrieved data is not ordered");
            }
            previous = current;
        }
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData", "checkRetrieveWorks"})
    void testWebsiteFilterWorks(){
        try {
            if (scraperRepository.count() == 0)
                scrapingService.getNewData();

            // build map of record present on DB
            HashMap<String, Long> elementsPerWebsite = new HashMap<>();
            List<ScrapedDataEntity> allData = scraperRepository.findAll();
            for (String el : websiteSelectorModel.getMap().keySet()) {
                long tot = allData.stream().filter(e -> e.getWebsite().equals(el)).count();
                elementsPerWebsite.put(el, tot);
            }

            //check filters on source works
            GetAllDataRequest request = new GetAllDataRequest();
            List<ScrapedData> result;
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getScrapedDataList());

            result = response.getScrapedDataList();
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            Assert.assertEquals(result.size(), allData.size());

            ArrayList<String> filter = new ArrayList<>();

            //check filters with all websites
            for (String el : websiteSelectorModel.getMap().keySet()) {
                filter.clear();
                filter.add(el);

                request.setWebsiteFilter(filter);
                response = scrapingService.getAllData(request);
                result = response.getScrapedDataList();
                Assert.assertNotNull(result);
                Assert.assertEquals(result.size(), elementsPerWebsite.get(el).intValue());
                checkOrder(result);
            }

            //check with multiple elements
            filter.clear();
            int total = 0;

            List<String> shuffledKeys = new ArrayList<>(websiteSelectorModel.getMap().keySet());
            Collections.shuffle(shuffledKeys);

            for (String el : shuffledKeys) {
                total += elementsPerWebsite.get(el);

                filter.add(el);
                request.setWebsiteFilter(filter);
                response = scrapingService.getAllData(request);
                result = response.getScrapedDataList();
                Assert.assertNotNull(result);
                Assert.assertEquals(result.size(), total);
                checkOrder(result);
            }

            // check with not existent website
            filter.clear();
            filter.add("NotExistentWebsite");

            request.setWebsiteFilter(filter);
            Assert.assertThrows(ScraperException.class, () -> scrapingService.getAllData(request));
        }catch (Exception e){
            Assert.fail("Unexpected exception thrown", e);
        }
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","testWebsiteFilterWorks"})
    void checkAllDataPresentAreRetrievedOsservatori(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("blog_osservatori");

            GetAllDataRequest request = new GetAllDataRequest();
            request.setWebsiteFilter(filter);
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getScrapedDataList());
            scrapedDataList = response.getScrapedDataList();
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertFalse(scrapedDataList.isEmpty());
        Assert.assertTrue(scrapedDataList.stream().allMatch(el->el.getWebsite().equals("blog_osservatori")));

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                el.getTitle().equals("“Compra ora e paga dopo”: cos’è e come funziona il Buy Now Pay Later") &&
                        el.getImageUrl().equals("https://blog.osservatori.net/hubfs/IP/buy-now-pay-later.jpg") &&
                        el.getCategory().equals("eCommerce B2c, Innovative Payments") &&
                        el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2021-11-22") &&
                        el.getLink().equals("https://blog.osservatori.net/it_it/buy-now-pay-later-come-funziona") &&
                        el.getBody().equals("Grazie all’evoluzione delle tecnologie digitali, il mondo dei pagamenti sta attraversando un periodo di grande fermento, tra nuove modalità e sistemi innovativi sempre più...") &&
                        el.getWebsite().equals("blog_osservatori")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Dispositivi Wearable: cosa sono, come funzionano e come utilizzarli per i pagamenti") &&
                                el.getImageUrl().equals("https://blog.osservatori.net/hubfs/mobile-payment/wearable-dispositivi.jpg") &&
                                el.getCategory().equals("Innovative Payments") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2020-12-29") &&
                                el.getLink().equals("https://blog.osservatori.net/it_it/dispositivi-wearable-cosa-sono-come-funzionano") &&
                                el.getBody().equals("Oltre allo smartphone, nel panorama dei pagamenti innovativi emergono altri dispositivi: i wearable. Per comprendere la crescita di questo mercato, è sufficiente considerarne i...") &&
                                el.getWebsite().equals("blog_osservatori")
                )
        );
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","testWebsiteFilterWorks"})
    void checkAllDataPresentAreRetrievedPagamentidigitaliInnovation(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("pagamentidigitali_innovation");

            GetAllDataRequest request = new GetAllDataRequest();
            request.setWebsiteFilter(filter);
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getScrapedDataList());
            scrapedDataList = response.getScrapedDataList();
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertFalse(scrapedDataList.isEmpty());
        Assert.assertTrue(scrapedDataList.stream().allMatch(el->el.getWebsite().equals("pagamentidigitali_innovation")));

        for (ScrapedData s: scrapedDataList){
            logger.error(s.toString());
        }

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Salone dei Pagamenti 2022, protagonisti: intelligenza artificiale, SmartPOS, piattaforme di pagamento") &&
                                el.getImageUrl().equals("https://d3cs2gzj5td7ug.cloudfront.net/wp-content/uploads/sites/7/2022/12/Salone-cover-678x381.jpg?x62207") &&
                                el.getCategory().equals("News") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-16") &&
                                el.getLink().equals("https://www.pagamentidigitali.it/news/salone-dei-pagamenti-2022-protagonisti-intelligenza-artificiale-smartpos-piattaforme-di-pagamento/") &&
                                el.getBody().equals("Dopo due anni di distanziamento sociale, il desiderio di incontrarsi di persona era tale che ancora si parla dell’edizione 2022 del Salone. Abbiamo chiesto ad alcuni dei protagonisti quali aspetti della loro partecipazione ritengono siano stati più significativi […]".substring(0,200)) &&
                                el.getWebsite().equals("pagamentidigitali_innovation")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Le tendenze future nei servizi finanziari – I parte") &&
                                el.getImageUrl().equals("https://d3cs2gzj5td7ug.cloudfront.net/wp-content/uploads/sites/7/2022/11/word-image-678x381.jpeg?x62207") &&
                                el.getCategory().equals("Payment Innovation") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-11-15") &&
                                el.getLink().equals("https://www.pagamentidigitali.it/payment-innovation/le-tendenze-future-nei-servizi-finanziari-i-parte/") &&
                                el.getBody().equals("L’ascesa del denaro digitale e della società senza contanti, la crescente importanza della customer experience, la diffusione dei pagamenti mobili e contactless e di come le nuove tecnologie dell’intelligenza artificiale e della Blockchain sono destinate a trasformare i servizi finanziari. Capire cosa sta succedendo e prepararsi alle tendenze future può aiutare a stare al passo con i tempi […]".substring(0,200)) &&
                                el.getWebsite().equals("pagamentidigitali_innovation")
                )
        );
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","testWebsiteFilterWorks"})
    void checkAllDataPresentAreRetrievedCorriere(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("corriere");

            GetAllDataRequest request = new GetAllDataRequest();
            request.setWebsiteFilter(filter);
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getScrapedDataList());
            scrapedDataList = response.getScrapedDataList();
            for (ScrapedData s: scrapedDataList){
                logger.error(s.toString());
            }
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertFalse(scrapedDataList.isEmpty());
        Assert.assertTrue(scrapedDataList.stream().allMatch(el->el.getWebsite().equals("corriere")));

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().startsWith("Certificazioni su misura di business: processi e dataset si fanno") &&
                                el.getImageUrl().equals("https://d110erj175o600.cloudfront.net/wp-content/uploads/2022/09/05182508/document-management-workflow-compliance.jpg") &&
                                el.getCategory().equals("SPONSORED STORY") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-29") &&
                                el.getLink().equals("https://www.corrierecomunicazioni.it/digital-economy/certificazioni-su-misura-di-business-processi-e-dataset-si-fanno-smart/") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("corriere")
                )
        );

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().matches("Chip, banda ultralarga e cybersecurity: ecco le priorit. della presidenza svedese Ue") &&
                                el.getImageUrl().equals("https://d110erj175o600.cloudfront.net/wp-content/uploads/2021/12/10143742/italia-europa-lq.jpg") &&
                                el.getCategory().equals("IL PIANO") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-28") &&
                                el.getLink().equals("https://www.corrierecomunicazioni.it/digital-economy/chip-tlc-e-cybersecurity-ecco-le-priorita-della-presidenza-svedese-ue/") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("corriere")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Innovazione, via ai bandi della Regione Lazio. Sul piatto 90 milioni") &&
                                el.getImageUrl().equals("https://d110erj175o600.cloudfront.net/wp-content/uploads/2021/12/20121136/digital-Inclusion-lq.jpg") &&
                                el.getCategory().equals("I FONDI") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-28") &&
                                el.getLink().equals("https://www.corrierecomunicazioni.it/digital-economy/innovazione-via-ai-bandi-della-regione-lazio-sul-piatto-90-milioni/") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("corriere")
                )
        );
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","testWebsiteFilterWorks"})
    void checkAllDataPresentAreRetrievedSole24ore(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("sole24ore");

            GetAllDataRequest request = new GetAllDataRequest();
            request.setWebsiteFilter(filter);
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getScrapedDataList());
            scrapedDataList = response.getScrapedDataList();
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertFalse(scrapedDataList.isEmpty());
        Assert.assertTrue(scrapedDataList.stream().allMatch(el->el.getWebsite().equals("sole24ore")));

        for (ScrapedData s: scrapedDataList){
            logger.error(s.toString());
        }

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Saranno le polizze embedded a trainare il mercato assicurativo del futuro") &&
                                "http://localhost:9000/static/images/placeholders/art/403x210.png".equals(el.getImageUrl()) &&
                                el.getCategory().equals("Insurtech") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-22") &&
                                el.getLink().equals("http://localhost:9000/art/saranno-polizze-embedded-trainare-mercato-assicurativo-futuro-AEUSpHRC") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("sole24ore")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("L’identità digitale entra nei servizi finanziari: cresce la richiesta di polizze") &&
                                el.getImageUrl().equals("http://localhost:9000/static/images/placeholders/art/154x154.png") &&
                                el.getCategory().equals("Insurtech") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-10-20") &&
                                el.getLink().equals("http://localhost:9000/art/l-identita-digitale-entra-servizi-finanziari-cresce-richiesta-polizze-AEIY3CAC") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("sole24ore")
                )
        );
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","testWebsiteFilterWorks"})
    void checkAllDataPresentAreRetrievedPaymentscardsandmobile(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("paymentscardsandmobile");

            GetAllDataRequest request = new GetAllDataRequest();
            request.setWebsiteFilter(filter);
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getScrapedDataList());
            scrapedDataList = response.getScrapedDataList();
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertFalse(scrapedDataList.isEmpty());
        Assert.assertTrue(scrapedDataList.stream().allMatch(el->el.getWebsite().equals("paymentscardsandmobile")));

        for (ScrapedData s: scrapedDataList){
            logger.error(s.toString());
        }

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Digital wallets: supporting financial inclusion in frontier markets") &&
                                "https://www.paymentscardsandmobile.com/wp-content/uploads/2022/12/NassPay_Artic-2.jpg".equals(el.getImageUrl()) &&
                                el.getCategory()==null &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-15") &&
                                el.getLink().equals("https://www.paymentscardsandmobile.com/digital-wallets-supporting-financial-inclusion-in-frontier-markets/") &&
                                el.getBody().equals("As one of the world’s most popular ways to pay, digital wallets have had a remarkable impact in the last decade. But as Waleed Khalid, CEO at NassWallet explains, their...") &&
                                el.getWebsite().equals("paymentscardsandmobile")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("The European Commission’s proposal for a regulation on instant payments") &&
                                el.getImageUrl() == null &&
                                el.getCategory()==null &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-02") &&
                                el.getLink().equals("https://www.paymentscardsandmobile.com/the-european-commissions-proposal-for-a-regulation-on-instant-payments/") &&
                                el.getBody().equals("On 26 October the European Commission (EC) adopted a legislative proposal on instant payments in euro, fulfilling its commitment of the 2020 Retail Payments Str...") &&
                                el.getWebsite().equals("paymentscardsandmobile")
                )
        );
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData", "checkScrapingWorks"})
    void testGoesToNextPage(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        Set<String> localUrls;
        try {
            String file = new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("local_url.txt")).readAllBytes());
            localUrls = file.lines().collect(Collectors.toSet());
        }catch (Exception e){
            Assert.fail("Can't read local urls file", e);
            return;
        }

        List<ScrapedDataEntity> dataEntityList = scraperRepository.findAll();

        // removing local website elements
        Map<String,List<ScrapedDataEntity>> filteredDataMap = dataEntityList.stream().filter(el->!localUrls.contains(el.getLink())).collect(Collectors.groupingBy(ScrapedDataEntity::getWebsite));
        // debug only
        // filteredDataMap.values().stream().flatMap(Collection::stream).sorted(Comparator.comparing(ScrapedDataEntity::getLink)).forEach(el->logger.error(el.toString()));
        Assert.assertTrue(filteredDataMap.values().stream().mapToLong(Collection::size).sum() > 0);

        // check read next pages for all websites
        for(String el:websiteSelectorModel.getMap().keySet()){
            Assert.assertTrue(filteredDataMap.containsKey(el), "Website "+el+" not present");
            Assert.assertFalse(filteredDataMap.get(el).isEmpty());
            for(String url: filteredDataMap.get(el).stream().map(ScrapedDataEntity::getLink).collect(Collectors.toList())){
                Assert.assertFalse(localUrls.contains(url));
            }
        }
    }

    @Test(dependsOnMethods = {"checkScrapingWorks"})
    void testCleanWorks(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        LocalDate limitDate = LocalDate.now()
                .minusYears(LIMIT_DATE_SCRAPER_YEARS)
                .minusMonths(LIMIT_DATE_SCRAPER_MONTHS)
                .minusDays(LIMIT_DATE_SCRAPER_DAYS);

        LocalDate oldestDatePossible = LocalDate.now()
                .minusYears(LIMIT_DATE_SCRAPER_YEARS)
                .minusMonths(LIMIT_DATE_SCRAPER_MONTHS)
                .minusDays(LIMIT_DATE_SCRAPER_DAYS)
                .withDayOfMonth(1)
                .minusDays(1);

        try{
            scrapingService.clean();
        }catch (Exception e){
            Assert.fail("Exception thrown during clean", e);
        }
        List<ScrapedDataEntity> allData = scraperRepository.findAll();
        Assert.assertTrue(allData.stream().allMatch(el->el.getDateArticle() != null && el.getDateArticle().isAfter(oldestDatePossible)));

        // insert new data
        ScrapedDataEntity oldArticle = new ScrapedDataEntity();
        oldArticle.setWebsite("test");
        oldArticle.setTitle("test");
        final String link = "http://test.com/test";
        oldArticle.setLink(link);
        oldArticle.setDateArticle(limitDate);

        scraperRepository.save(oldArticle);
        allData = scraperRepository.findAll();

        Assert.assertTrue(allData.stream().allMatch(el->el.getDateArticle() != null && el.getDateArticle().isAfter(oldestDatePossible)));

        scrapingService.clean();
        allData = scraperRepository.findAll();
        Assert.assertTrue(allData.stream().allMatch(el->el.getDateArticle() != null && el.getDateArticle().isAfter(oldestDatePossible)));
        Assert.assertTrue(allData.stream().anyMatch(el->el.getLink().equals(link)));

        // insert even older data
        ScrapedDataEntity olderArticle = new ScrapedDataEntity();
        olderArticle.setWebsite("test");
        olderArticle.setTitle("test2");
        olderArticle.setLink("http://test.com/test2");
        olderArticle.setDateArticle(limitDate.minusMonths(1));

        scraperRepository.save(olderArticle);
        allData = scraperRepository.findAll();

        Assert.assertFalse(allData.stream().allMatch(el->el.getDateArticle() != null && el.getDateArticle().isAfter(oldestDatePossible)));

        scrapingService.clean();
        allData = scraperRepository.findAll();
        Assert.assertTrue(allData.stream().allMatch(el->el.getDateArticle() != null && el.getDateArticle().isAfter(oldestDatePossible)));
    }

    @Test(dependsOnMethods = {"testGoesToNextPage", "testCleanWorks"})
    void testDoesNotGetTooOldArticles(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedDataEntity> allData = scraperRepository.findAll();
        LocalDate oldestDatePossible = LocalDate.now()
                .minusYears(LIMIT_DATE_SCRAPER_YEARS)
                .minusMonths(LIMIT_DATE_SCRAPER_MONTHS)
                .minusDays(LIMIT_DATE_SCRAPER_DAYS)
                .withDayOfMonth(1)
                .minusDays(1);

        scrapingService.clean();
        scrapingService.getNewData();

        Assert.assertTrue(allData.stream().allMatch(el->el.getDateArticle() != null && el.getDateArticle().isAfter(oldestDatePossible)));
    }

    @Test(dependsOnMethods = {"checkRetrieveWorks", "checkScrapingWorks"})
    void testDateFilter(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();
        Assert.assertTrue(scraperRepository.count() > 0);

        GetAllDataRequest request = new GetAllDataRequest();

        // wrong month
        request.setMonth("2023");
        Assert.assertThrows(ScraperException.class, ()->scrapingService.getAllData(request));

        request.setMonth("2023-13");
        Assert.assertThrows(ScraperException.class, ()->scrapingService.getAllData(request));

        request.setMonth("2023/12");
        Assert.assertThrows(ScraperException.class, ()->scrapingService.getAllData(request));

        request.setMonth("2023-00");
        Assert.assertThrows(ScraperException.class, ()->scrapingService.getAllData(request));

        request.setMonth("2023-3");
        Assert.assertThrows(ScraperException.class, ()->scrapingService.getAllData(request));

        // valid
        List<ScrapedDataEntity> allDataList = scraperRepository.findAll();
        final ScrapedDataEntity data = allDataList.get(0);
        Assert.assertNotNull(data.getDateArticle());
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        final String month = data.getDateArticle().format(formatter);

        try {
            request.setMonth(month);
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);

            List<ScrapedData> scrapedDataList = response.getScrapedDataList();
            Assert.assertNotNull(scrapedDataList);
            Assert.assertFalse(scrapedDataList.isEmpty());
            Assert.assertTrue(scrapedDataList.stream().allMatch(el->month.equals(el.getDateArticle().format(formatter))));
            Assert.assertTrue(scrapedDataList.stream().anyMatch(el->el.getLink().equals(data.getLink())));
        }catch (Exception e){
            Assert.fail("Exception while retrieving filtered data", e);
        }
    }

    @Test(dependsOnMethods = {"testDateFilter","testWebsiteFilterWorks"})
    void testMixedFilterWorks(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();
        Assert.assertTrue(scraperRepository.count() > 0);

        GetAllDataRequest request = new GetAllDataRequest();
        List<ScrapedDataEntity> allDataList = scraperRepository.findAll();
        final ScrapedDataEntity data = allDataList.get(0);
        Assert.assertNotNull(data.getDateArticle());
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        final String month = data.getDateArticle().format(formatter);
        final String website = data.getWebsite();

        List<String> filter = new ArrayList<>();
        filter.add(website);

        request.setWebsiteFilter(filter);
        request.setMonth(month);

        try{
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);

            List<ScrapedData> scrapedDataList = response.getScrapedDataList();
            Assert.assertNotNull(scrapedDataList);
            Assert.assertFalse(scrapedDataList.isEmpty());
            Assert.assertTrue(scrapedDataList.stream()
                    .allMatch(el->month.equals(el.getDateArticle().format(formatter)) && website.equals(el.getWebsite())));
            Assert.assertTrue(scrapedDataList.stream()
                    .anyMatch(el->el.getLink().equals(data.getLink())));
        }catch (Exception e){
            Assert.fail("Exception while retrieving data", e);
        }

        // check with no data present
        final String monthFuture = LocalDate.now().plusYears(10).format(formatter);
        try{
            request.setMonth(monthFuture);
            GetAllDataResponse response = scrapingService.getAllData(request);
            Assert.assertNotNull(response);

            List<ScrapedData> scrapedDataList = response.getScrapedDataList();
            Assert.assertNotNull(scrapedDataList);
            Assert.assertTrue(scrapedDataList.isEmpty());
        }catch (Exception e){
            Assert.fail("Exception while retrieving data", e);
        }
    }
}
