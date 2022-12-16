package it.cdc.be.webscraper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ScraperServiceTests extends AbstractTestNGSpringContextTests {
    private static final WireMockServer wireMockServer = new WireMockServer(options().port(9000));

    @Autowired
    ScrapingService scrapingService;

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry){
        wireMockServer.start();
        configureFor("localhost", 9000);

        registry.add("scraper.urls", ()->"localhost:9000/index.html");
    }

    @BeforeClass()
    void init() throws IOException {
        // init wiremock to provide sample data
        WireMock.stubFor(
                WireMock.get("/site1/index.html")
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("content-type","text/html")
                                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite1.html")).readAllBytes(), StandardCharsets.UTF_8))
                        )
        );

        WireMock.stubFor(
                WireMock.get("/site2/index.html")
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("content-type","text/html")
                                        .withBody(new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestWebsite2.html")).readAllBytes(), StandardCharsets.UTF_8))
                        )
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
            Assert.fail("Error while testing scraping new data", e);
        }

        // ensure that the data retrieved are stored
        List<ScrapedData> scrapedDataList;
        try{
            scrapedDataList = scrapingService.getAllData();
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertTrue(scrapedDataList.size() == 12 + 8);

        //TODO check that all elements that should be in the list are present
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                el.getTitle().equals("Credit Card Merchant Fees: Full Guide &amp; Comparison for 2022") &&
                el.getImageUrl().equals("https://assets-global.website-files.com/610922bf9b095f4969ed70fb/628a296d8c4f049201df293b_BlogPST-Thumb-Image%20(21).jpg"))
        );

    }
}
