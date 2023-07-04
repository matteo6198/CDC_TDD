package it.cdc.be.webscraper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer;
import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
import it.cdc.be.webscraper.dto.domain.Pagination;
import it.cdc.be.webscraper.dto.request.GetAllDataRequest;
import it.cdc.be.webscraper.dto.response.GetAllDataResponse;
import it.cdc.be.webscraper.layers.service.WebScraperService;
import it.cdc.be.webscraper.repository.ScraperRepository;
import it.cdc.be.webscraper.repository.entity.ScrapedDataEntity;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTests extends AbstractTestNGSpringContextTests {
    private static final int LIMIT_DATE_SCRAPER_YEARS = 0;
    private static final int LIMIT_DATE_SCRAPER_MONTHS = 6;
    private static final int LIMIT_DATE_SCRAPER_DAYS = 0;
    private static final String path = "/getData";
    @Value("${local.server.port}")
    private int port;
    @Autowired
    private WebScraperService webScraperService;
    @Autowired
    ScraperRepository scraperRepository;

    @SuppressWarnings("unused")
    @Resource(name = "WebsiteSelectorModel")
    private WebsiteSelectorModel websiteSelectorModel;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final WireMockServer wireMockServer = new WireMockServer(options().port(9000).enableBrowserProxying(true));
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

        webScraperService.getNewData();
    }

    @Test()
    void testRetrieveServiceWorks(){
        final String url = "http://localhost:"+port+path;

        GetAllDataRequest request = new GetAllDataRequest();
        HttpEntity<GetAllDataRequest> requestHttpEntity = new HttpEntity<>(request);
        ResponseEntity<GetAllDataResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestHttpEntity, GetAllDataResponse.class);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
        Assert.assertTrue(response.hasBody());
        GetAllDataResponse body = response.getBody();
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getScrapedDataList() != null && !body.getScrapedDataList().isEmpty());
        Assert.assertNotNull(body.getPagination());
        Assert.assertEquals(body.getPagination().getPageNumber(), 0);
        Assert.assertEquals(body.getPagination().getTotalPages(), 1);
        Assert.assertEquals(body.getPagination().getPageLength(), Integer.MAX_VALUE);
    }

    @Test(dependsOnMethods = {"testRetrieveServiceWorks"})
    void testDateFilter(){
        final String url = "http://localhost:"+port+path;

        GetAllDataRequest request = new GetAllDataRequest();
        final ScrapedDataEntity entity = scraperRepository.findAll().get(0);
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        final String month = entity.getDateArticle().format(formatter);
        request.setMonth(month);

        HttpEntity<GetAllDataRequest> requestHttpEntity = new HttpEntity<>(request);
        ResponseEntity<GetAllDataResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestHttpEntity, GetAllDataResponse.class);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
        Assert.assertTrue(response.hasBody());
        GetAllDataResponse body = response.getBody();
        Assert.assertNotNull(body);

        Assert.assertTrue(body.getScrapedDataList().size() >= 1);
        Assert.assertTrue(body.getScrapedDataList().stream().allMatch(el->el.getDateArticle() != null && el.getDateArticle().format(formatter).equals(month)));
        Assert.assertTrue(body.getScrapedDataList().stream().anyMatch(el->el.getLink().equals(entity.getLink()) && el.getWebsite().equals(entity.getWebsite())));

        // invalid month
        request.setMonth("2-2");
        HttpEntity<GetAllDataRequest> requestHttpEntityInvalidMonth = new HttpEntity<>(request);
        try {
            restTemplate.exchange(url, HttpMethod.POST, requestHttpEntityInvalidMonth, GetAllDataResponse.class);
            Assert.fail("This should be a 500");
        }catch (HttpStatusCodeException e){
            Assert.assertEquals(e.getStatusCode(), HttpStatusCode.valueOf(500));
        }

        // month too old
        String tooOldMonth = LocalDate.now().minusYears(100).format(formatter);
        request.setMonth(tooOldMonth);
        HttpEntity<GetAllDataRequest> requestHttpEntityTooOld = new HttpEntity<>(request);
        try {
            restTemplate.exchange(url, HttpMethod.POST, requestHttpEntityTooOld, GetAllDataResponse.class);
            Assert.fail("This should be a 500");
        }catch (HttpStatusCodeException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatusCode.valueOf(500));
        }
    }

    @Test(dependsOnMethods = {"testRetrieveServiceWorks"})
    void testWebsiteFilter(){
        final String url = "http://localhost:"+port+path;

        GetAllDataRequest request = new GetAllDataRequest();

        List<String> filter = new ArrayList<>();

        // not existent website
        filter.add("NotExistentWebsite");
        request.setWebsiteFilter(filter);
        HttpEntity<GetAllDataRequest> requestHttpEntityInvalidWebsite = new HttpEntity<>(request);
        try {
            restTemplate.exchange(url, HttpMethod.POST, requestHttpEntityInvalidWebsite, GetAllDataResponse.class);
            Assert.fail("This should be a 500");
        }catch (HttpStatusCodeException e){
            Assert.assertEquals(e.getStatusCode(), HttpStatusCode.valueOf(500));
        }

        // single website
        filter.clear();
        final String website = scraperRepository.findAll().get(0).getWebsite();
        filter.add(website);
        request.setWebsiteFilter(filter);
        HttpEntity<GetAllDataRequest> requestSingleWebsite = new HttpEntity<>(request);
        ResponseEntity<GetAllDataResponse> responseSingleWebsite = restTemplate.exchange(url, HttpMethod.POST, requestSingleWebsite, GetAllDataResponse.class);
        Assert.assertNotNull(responseSingleWebsite);
        Assert.assertEquals(responseSingleWebsite.getStatusCode(), HttpStatusCode.valueOf(200));
        Assert.assertTrue(responseSingleWebsite.hasBody());
        GetAllDataResponse body = responseSingleWebsite.getBody();
        Assert.assertNotNull(body);
        Assert.assertFalse(body.getScrapedDataList().isEmpty());
        Assert.assertTrue(body.getScrapedDataList().stream().allMatch(el -> el.getWebsite() != null && website.equals(el.getWebsite())));

        // multiple websites
        final String website2 = scraperRepository.findAll().stream().filter(el->!website.equals(el.getWebsite())).findAny().get().getWebsite();
        if(website2 == null)
            Assert.fail("Only one website is present");

        filter.add(website2);
        request.setWebsiteFilter(filter);
        HttpEntity<GetAllDataRequest> requestHttp = new HttpEntity<>(request);
        ResponseEntity<GetAllDataResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestHttp, GetAllDataResponse.class);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
        Assert.assertTrue(response.hasBody());
        GetAllDataResponse body2 = response.getBody();
        Assert.assertNotNull(body2);
        Assert.assertFalse(body2.getScrapedDataList().isEmpty());
        Assert.assertTrue(body2.getScrapedDataList().stream().allMatch(el -> el.getWebsite() != null && (website.equals(el.getWebsite()) || website2.equals(el.getWebsite()))));
    }

    @Test(dependsOnMethods = {"testRetrieveServiceWorks"})
    void testPagination(){
        final String url = "http://localhost:"+port+path;

        GetAllDataRequest request = new GetAllDataRequest();

        // without pagination returns all
        HttpEntity<GetAllDataRequest> requestHttp = new HttpEntity<>(request);
        ResponseEntity<GetAllDataResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestHttp, GetAllDataResponse.class);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
        Assert.assertTrue(response.hasBody());
        GetAllDataResponse body = response.getBody();
        Assert.assertNotNull(body);
        Assert.assertNotNull(body.getPagination());
        Assert.assertEquals(body.getPagination().getPageNumber(), 0);
        Assert.assertEquals(body.getPagination().getPageLength(), Integer.MAX_VALUE);
        Assert.assertEquals(body.getPagination().getTotalPages(), 1);

        final int total = body.getScrapedDataList().size();
        // valid
        Pagination pagination = new Pagination();
        pagination.setPageLength(1);
        request.setPagination(pagination);
        requestHttp = new HttpEntity<>(request);
        response = restTemplate.exchange(url, HttpMethod.POST, requestHttp, GetAllDataResponse.class);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
        Assert.assertTrue(response.hasBody());
        body = response.getBody();
        Assert.assertNotNull(body);
        Assert.assertNotNull(body.getPagination());
        Assert.assertEquals(body.getPagination().getPageNumber(), 0);
        Assert.assertEquals(body.getPagination().getPageLength(), 1);
        Assert.assertEquals(body.getPagination().getTotalPages(), total);
        Assert.assertEquals(body.getScrapedDataList().size(), 1);

        // next page
        pagination.setPageNumber(1);
        request.setPagination(pagination);
        requestHttp = new HttpEntity<>(request);
        response = restTemplate.exchange(url, HttpMethod.POST, requestHttp, GetAllDataResponse.class);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
        Assert.assertTrue(response.hasBody());
        body = response.getBody();
        Assert.assertNotNull(body);
        Assert.assertNotNull(body.getPagination());
        Assert.assertEquals(body.getPagination().getPageNumber(), 1);
        Assert.assertEquals(body.getPagination().getPageLength(), 1);
        Assert.assertEquals(body.getPagination().getTotalPages(), total);
        Assert.assertEquals(body.getScrapedDataList().size(), 1);

        // invalid page number
        pagination.setPageNumber(-1);
        request.setPagination(pagination);
        requestHttp = new HttpEntity<>(request);
        try {
            restTemplate.exchange(url, HttpMethod.POST, requestHttp, GetAllDataResponse.class);
            Assert.fail("This should be a 500");
        }catch (HttpStatusCodeException e){
            Assert.assertEquals(e.getStatusCode(), HttpStatusCode.valueOf(500));
        }

        // invalid page length
        pagination.setPageLength(-1);
        request.setPagination(pagination);
        requestHttp = new HttpEntity<>(request);
        try {
            restTemplate.exchange(url, HttpMethod.POST, requestHttp, GetAllDataResponse.class);
            Assert.fail("This should be a 500");
        }catch (HttpStatusCodeException e){
            Assert.assertEquals(e.getStatusCode(), HttpStatusCode.valueOf(500));
        }
    }
}
