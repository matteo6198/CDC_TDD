package it.cdc.be.webscraper.configuration;

import it.cdc.be.webscraper.configuration.model.WebsiteSelectorModel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties
@Configuration
public class WebsiteSelectorsConfig {

    @Bean("WebsiteSelectorModel")
    @ConfigurationProperties(prefix = "website")
    WebsiteSelectorModel websiteSelectorModel(){
        return new WebsiteSelectorModel();
    }
}
