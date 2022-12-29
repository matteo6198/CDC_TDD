package it.cdc.be.webscraper.utils;

import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
import it.cdc.be.webscraper.dto.domain.Selector;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
}
