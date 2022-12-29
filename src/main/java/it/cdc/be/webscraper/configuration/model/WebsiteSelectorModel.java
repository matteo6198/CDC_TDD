package it.cdc.be.webscraper.configuration.model;

import it.cdc.be.webscraper.dto.domain.Selector;

import java.util.Map;

public class WebsiteSelectorModel {
    private Map<String, Selector> map;

    public Map<String, Selector> getMap() {
        return map;
    }

    public void setMap(Map<String, Selector> map) {
        this.map = map;
    }
}
