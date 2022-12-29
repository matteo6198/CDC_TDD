package it.cdc.be.webscraper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer;
import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
import it.cdc.be.webscraper.dto.domain.ScrapedData;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ScraperServiceTests extends AbstractTestNGSpringContextTests {
    private static final WireMockServer wireMockServer = new WireMockServer(options().port(9000).enableBrowserProxying(true));

    @Autowired
    WebScraperService scrapingService;

    @Autowired
    ScraperRepository scraperRepository;

    @Resource(name = "WebsiteSelectorModel")
    private WebsiteSelectorModel websiteSelectorModel;

    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest");

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry){
        wireMockServer.start();
        JvmProxyConfigurer.configureFor(wireMockServer);
        configureFor("localhost", 9000);

        //registry.add("scraper.urls", ()->"http://localhost:9000/site1;http://localhost:9000/site2;http://localhost:9000/site3;http://localhost:9000/site4;http://localhost:9000/site5;http://localhost:9000/site6;http://localhost:9000/site7;http://localhost:9000/site8");
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
        // init wiremock to provide sample data
        WireMock.stubFor(WireMock.get("/it_it/tag/innovative-payments").withHost(equalTo("blog.osservatori.net")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite1.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/payment-innovation").withHost(equalTo("pagamentidigitali.it")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite2.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/payment-services").withHost(equalTo("pagamentidigitali.it")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite3.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/mobile-app").withHost(equalTo("pagamentidigitali.it")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite4.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/digital-economy").withHost(equalTo("corrierecomunicazioni.it")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite5.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/sez/tecnologia/fintech").withHost(equalTo("ilsole24ore.com")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite6.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/category/mobile-payments").withHost(equalTo("paymentscardsandmobile.com")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite7.html")).readAllBytes(), StandardCharsets.UTF_8)))
        );
        WireMock.stubFor(WireMock.get("/digital-payments/articles").withHost(equalTo("fintechmagazine.com")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("content-type","text/html")
                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite8.html")).readAllBytes(), StandardCharsets.UTF_8)))
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
    }

    @Test(dependsOnMethods = {"checkScrapingWorks"})
    void checkRetrieveWorks(){
        // ensure that the data retrieved are stored
        List<ScrapedData> scrapedDataList = null;
        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertTrue(scrapedDataList.size() > 0);
    }

    @Test(dependsOnMethods = {"checkRetrieveWorks"})
    void checkFiltersWorks(){
        //check filters on source works
        List<ScrapedData> result = scrapingService.getAllData(null);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        ArrayList<String> filter = new ArrayList<>();
        HashMap<String, Integer> elementsPerWebsite = new HashMap<>();

        //check filters with all websites
        for(String el:websiteSelectorModel.getMap().keySet()){
            filter.clear();
            filter.add(el);

            result = scrapingService.getAllData(filter);
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());

            elementsPerWebsite.put(el, result.size());
        }

        //check with multiple elements
        filter.clear();
        int total = 0;

        List<String> shuffledKeys = new ArrayList<>(websiteSelectorModel.getMap().keySet());
        Collections.shuffle(shuffledKeys);

        for(String el: shuffledKeys){
            total += elementsPerWebsite.get(el);

            filter.add(el);
            result = scrapingService.getAllData(filter);
            Assert.assertNotNull(result);
            Assert.assertEquals(result.size(), total);
        }
    }

    @Test(dependsOnMethods = {"checkScrapingWorks","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedOsservatori(){
        //TODO

        List<ScrapedData> scrapedDataList = null;
        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertEquals(scrapedDataList.size(), 12 + 8);

        //TODO check that all elements that should be in the list are present
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                el.getTitle().equals("Credit Card Merchant Fees: Full Guide &amp; Comparison for 2022") &&
                        el.getImageUrl().equals("https://assets-global.website-files.com/610922bf9b095f4969ed70fb/628a296d8c4f049201df293b_BlogPST-Thumb-Image%20(21).jpg"))
        );
    }

    @Test(dependsOnMethods = {"checkScrapingWorks","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedPagamentidigitali(){
        //TODO

        List<ScrapedData> scrapedDataList = null;
        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertEquals(scrapedDataList.size(), 12 + 8);

        //TODO check that all elements that should be in the list are present
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                el.getTitle().equals("Credit Card Merchant Fees: Full Guide &amp; Comparison for 2022") &&
                        el.getImageUrl().equals("https://assets-global.website-files.com/610922bf9b095f4969ed70fb/628a296d8c4f049201df293b_BlogPST-Thumb-Image%20(21).jpg"))
        );
    }

    @Test(dependsOnMethods = {"checkScrapingWorks","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedCorriere(){
        //TODO

        List<ScrapedData> scrapedDataList = null;
        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertEquals(scrapedDataList.size(), 12 + 8);

        //TODO check that all elements that should be in the list are present
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                el.getTitle().equals("Credit Card Merchant Fees: Full Guide &amp; Comparison for 2022") &&
                        el.getImageUrl().equals("https://assets-global.website-files.com/610922bf9b095f4969ed70fb/628a296d8c4f049201df293b_BlogPST-Thumb-Image%20(21).jpg"))
        );
    }

    @Test(dependsOnMethods = {"checkScrapingWorks","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedSole24ore(){
        //TODO

        List<ScrapedData> scrapedDataList = null;
        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertEquals(scrapedDataList.size(), 12 + 8);

        //TODO check that all elements that should be in the list are present
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                el.getTitle().equals("Credit Card Merchant Fees: Full Guide &amp; Comparison for 2022") &&
                        el.getImageUrl().equals("https://assets-global.website-files.com/610922bf9b095f4969ed70fb/628a296d8c4f049201df293b_BlogPST-Thumb-Image%20(21).jpg"))
        );
    }

    @Test(dependsOnMethods = {"checkScrapingWorks","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedPaymentscardsandmobile(){
        //TODO

        List<ScrapedData> scrapedDataList = null;
        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertEquals(scrapedDataList.size(), 12 + 8);

        //TODO check that all elements that should be in the list are present
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                el.getTitle().equals("Credit Card Merchant Fees: Full Guide &amp; Comparison for 2022") &&
                        el.getImageUrl().equals("https://assets-global.website-files.com/610922bf9b095f4969ed70fb/628a296d8c4f049201df293b_BlogPST-Thumb-Image%20(21).jpg"))
        );
    }

    @Test(dependsOnMethods = {"checkScrapingWorks","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedFintechmagazine(){
        //TODO

        List<ScrapedData> scrapedDataList = null;
        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertEquals(scrapedDataList.size(), 12 + 8);

        //TODO check that all elements that should be in the list are present
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                el.getTitle().equals("Credit Card Merchant Fees: Full Guide &amp; Comparison for 2022") &&
                        el.getImageUrl().equals("https://assets-global.website-files.com/610922bf9b095f4969ed70fb/628a296d8c4f049201df293b_BlogPST-Thumb-Image%20(21).jpg"))
        );
    }
}
