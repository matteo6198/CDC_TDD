package it.cdc.be.webscraper;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebScraperApplicationTests extends AbstractTestNGSpringContextTests {

	@Test()
	void contextLoads() {
	}

}
