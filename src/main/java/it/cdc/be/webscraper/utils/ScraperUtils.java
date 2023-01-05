package it.cdc.be.webscraper.utils;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
import it.cdc.be.webscraper.dto.domain.Selector;
import it.cdc.be.webscraper.dto.response.GoToNextPageResponse;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ScraperUtils {
    private static final Logger logger = LoggerFactory.getLogger(ScraperUtils.class);

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
}
