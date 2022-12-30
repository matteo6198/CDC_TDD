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
import java.time.format.DateTimeFormatter;
import java.util.*;

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
        Assert.assertTrue(allData.stream()
                .allMatch(d ->
                        d.getLink().matches("^http[s]?://.*") &&
                                (d.getImageUrl() == null || d.getImageUrl().matches("^http[s]?://.*")) &&
                                d.getTitle() != null && !d.getTitle().isBlank() && d.getWebsite() != null
                        ));
    }

    @Test(dependsOnMethods = {"checkScrapingWorks", "checkRetrieveWorks"})
    void checkServiceDoesNotDuplicateData(){
        scraperRepository.deleteAll();
        Assert.assertEquals(scraperRepository.count(), 0);

        List<ScrapedData> scrapedDataList = null;

        // first get
        try{
            scrapingService.getNewData();
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            logger.error("Exception: ", e);
            Assert.fail("Error while testing scraping new data", e);
        }
        Assert.assertNotNull(scrapedDataList);

        // second get
        List<ScrapedData> secondRetrieveData = null;
        try{
            scrapingService.getNewData();
            secondRetrieveData = scrapingService.getAllData(null);
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
        scraperRepository.deleteAll();
        Assert.assertEquals(scraperRepository.count(), 0);

        List<ScrapedData> scrapedDataList = null;
        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }
        Assert.assertNotNull(scrapedDataList);
        Assert.assertEquals(scrapedDataList.size(), 0);

        // ensure that the data retrieved are stored
        scrapingService.getNewData();
        Assert.assertTrue(scraperRepository.count() > 0);

        try{
            scrapedDataList = scrapingService.getAllData(null);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertTrue(scrapedDataList.size() > 0);
        Assert.assertEquals(scrapedDataList.size(), scraperRepository.count());
        checkOrder(scrapedDataList);
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
    void checkFiltersWorks(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        // build map of record present on DB
        HashMap<String, Long> elementsPerWebsite = new HashMap<>();
        List<ScrapedDataEntity> allData = scraperRepository.findAll();
        for(String el:websiteSelectorModel.getMap().keySet()){
            long tot = allData.stream().filter(e -> e.getWebsite().equals(el)).count();
            elementsPerWebsite.put(el, tot);
        }

        //check filters on source works
        List<ScrapedData> result = scrapingService.getAllData(null);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.size(), allData.size());

        ArrayList<String> filter = new ArrayList<>();

        //check filters with all websites
        for(String el:websiteSelectorModel.getMap().keySet()){
            filter.clear();
            filter.add(el);

            result = scrapingService.getAllData(filter);
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            Assert.assertEquals(result.size(), elementsPerWebsite.get(el).intValue());
            checkOrder(result);
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
            checkOrder(result);
        }
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedOsservatori(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("blog_osservatori");

            scrapedDataList = scrapingService.getAllData(filter);
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

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedPagamentidigitaliInnovation(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("pagamentidigitali_innovation");

            scrapedDataList = scrapingService.getAllData(filter);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertFalse(scrapedDataList.isEmpty());
        Assert.assertTrue(scrapedDataList.stream().allMatch(el->el.getWebsite().equals("pagamentidigitali_innovation")));

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Salone dei Pagamenti 2022, protagonisti: intelligenza artificiale, SmartPOS, piattaforme di pagamento") &&
                                el.getImageUrl().equals("https://d3cs2gzj5td7ug.cloudfront.net/wp-content/uploads/sites/7/2022/12/Salone-cover-678x381.jpg?x62207") &&
                                el.getCategory().equals("News") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-16") &&
                                el.getLink().equals("https://www.pagamentidigitali.it/news/salone-dei-pagamenti-2022-protagonisti-intelligenza-artificiale-smartpos-piattaforme-di-pagamento/?__hstc=181257784.cc17aa605debb3743b8d5a5a7b7d71e0.1672408633465.1672408633465.1672408633465.1&__hssc=181257784.2.1672408633465&__hsfp=1169665805") &&
                                el.getBody().equals("Dopo due anni di distanziamento sociale, il desiderio di incontrarsi di persona era tale che ancora si parla dell’edizione 2022 del Salone. Abbiamo chiesto ad alcuni dei protagonisti quali aspetti della loro partecipazione ritengono siano stati più significativi […]".substring(0,200)) &&
                                el.getWebsite().equals("pagamentidigitali_innovation")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Le tendenze future nei servizi finanziari – I parte") &&
                                el.getImageUrl().equals("https://d3cs2gzj5td7ug.cloudfront.net/wp-content/uploads/sites/7/2022/11/word-image-678x381.jpeg?x62207") &&
                                el.getCategory().equals("Payment Innovation") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-15") &&
                                el.getLink().equals("https://www.pagamentidigitali.it/payment-innovation/le-tendenze-future-nei-servizi-finanziari-i-parte/?__hstc=181257784.cc17aa605debb3743b8d5a5a7b7d71e0.1672408633465.1672408633465.1672408633465.1&__hssc=181257784.2.1672408633465&__hsfp=1169665805") &&
                                el.getBody().equals("L’ascesa del denaro digitale e della società senza contanti, la crescente importanza della customer experience, la diffusione dei pagamenti mobili e contactless e di come le nuove tecnologie dell’intelligenza artificiale e della Blockchain sono destinate a trasformare i servizi finanziari. Capire cosa sta succedendo e prepararsi alle tendenze future può aiutare a stare al passo con i tempi […]".substring(0,200)) &&
                                el.getWebsite().equals("pagamentidigitali_innovation")
                )
        );
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedCorriere(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("corriere");

            scrapedDataList = scrapingService.getAllData(filter);
            for (ScrapedData s: scrapedDataList){
                logger.error(s.toString());
            }
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertFalse(scrapedDataList.isEmpty());
        Assert.assertTrue(scrapedDataList.stream().allMatch(el->el.getWebsite().equals("corriere")));

        scrapedDataList.stream().filter(el ->
                el.getTitle().startsWith("Certificazioni su misura di business: processi e dataset si fanno")).findAny().ifPresent(data -> logger.info("Data: " + data.getTitle() + ", " + data.getImageUrl() + ", " + data.getCategory()));

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().startsWith("Certificazioni su misura di business: processi e dataset si fanno") &&
                                el.getImageUrl() == null &&
                                el.getCategory().equals("SPONSORED STORY") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-29") &&
                                el.getLink().equals("https://www.corrierecomunicazioni.it/digital-economy/certificazioni-su-misura-di-business-processi-e-dataset-si-fanno-smart/?__hstc=181257784.cc17aa605debb3743b8d5a5a7b7d71e0.1672408633465.1672408633465.1672408633465.1&__hssc=181257784.3.1672408633465&__hsfp=1169665805") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("corriere")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Chip, banda ultralarga e cybersecurity: ecco le priorità della presidenza svedese Ue") &&
                                el.getImageUrl().equals("https://d110erj175o600.cloudfront.net/wp-content/uploads/2021/12/10143742/italia-europa.jpg") &&
                                el.getCategory().equals("IL PIANO") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-28") &&
                                el.getLink().equals("https://www.corrierecomunicazioni.it/digital-economy/chip-tlc-e-cybersecurity-ecco-le-priorita-della-presidenza-svedese-ue/?__hstc=181257784.cc17aa605debb3743b8d5a5a7b7d71e0.1672408633465.1672408633465.1672408633465.1&__hssc=181257784.3.1672408633465&__hsfp=1169665805") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("corriere")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Innovazione, via ai bandi della Regione Lazio. Sul piatto 90 milioni") &&
                                el.getImageUrl()==null &&
                                el.getCategory().equals("I FONDI") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-28") &&
                                el.getLink().equals("https://www.corrierecomunicazioni.it/digital-economy/innovazione-via-ai-bandi-della-regione-lazio-sul-piatto-90-milioni/?__hstc=181257784.cc17aa605debb3743b8d5a5a7b7d71e0.1672408633465.1672408633465.1672408633465.1&__hssc=181257784.3.1672408633465&__hsfp=1169665805") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("corriere")
                )
        );
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedSole24ore(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

        List<ScrapedData> scrapedDataList = null;
        try{
            List<String> filter = new ArrayList<>();
            filter.add("sole24ore");

            scrapedDataList = scrapingService.getAllData(filter);
        }catch (Exception e){
            Assert.fail("Can't retrieve stored scraped data", e);
        }

        Assert.assertNotNull(scrapedDataList);
        Assert.assertFalse(scrapedDataList.isEmpty());
        Assert.assertTrue(scrapedDataList.stream().allMatch(el->el.getWebsite().equals("sole24ore")));

        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("Saranno le polizze embedded a trainare il mercato assicurativo del futuro") &&
                                "https://www.ilsole24ore.com/static/images/placeholders/art/403x210.png".equals(el.getImageUrl()) &&
                                el.getCategory().equals("Insurtech") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-12-22") &&
                                el.getLink().equals("https://www.ilsole24ore.com/art/saranno-polizze-embedded-trainare-mercato-assicurativo-futuro-AEUSpHRC") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("sole24ore")
                )
        );
        Assert.assertTrue(scrapedDataList.stream().anyMatch(el->
                        el.getTitle().equals("L’identità digitale entra nei servizi finanziari: cresce la richiesta di polizze") &&
                                el.getImageUrl().equals("https://www.ilsole24ore.com/static/images/placeholders/art/154x154.png") &&
                                el.getCategory().equals("Insurtech") &&
                                el.getDateArticle().format(DateTimeFormatter.ISO_DATE).equals("2022-10-20") &&
                                el.getLink().equals("https://www.ilsole24ore.com/art/l-identita-digitale-entra-servizi-finanziari-cresce-richiesta-polizze-AEIY3CAC") &&
                                el.getBody()==null &&
                                el.getWebsite().equals("sole24ore")
                )
        );
    }

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedPaymentscardsandmobile(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

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

    @Test(dependsOnMethods = {"checkServiceDoesNotDuplicateData","checkRetrieveWorks","checkFiltersWorks"})
    void checkAllDataPresentAreRetrievedFintechmagazine(){
        if(scraperRepository.count() == 0)
            scrapingService.getNewData();

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
