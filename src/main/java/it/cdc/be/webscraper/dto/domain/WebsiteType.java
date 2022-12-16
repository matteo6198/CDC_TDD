package it.cdc.be.webscraper.dto.domain;

import java.util.List;
import java.util.Locale;

public enum WebsiteType {
    PAY_COM("/div[@class=collection-list-blog-cards]//div[@class=w-dyn-item]" ,"//h2[@class=h2-blog-card]", "//img[@class=image-blog-thumg-in-a-card]", "//a");


    private final String titleSelector;
    private final String imageSelector;
    private final String linkSelector;
    private final String itemsSelector;

    WebsiteType(String itemsSelector, String titleSelector, String imageSelector, String linkSelector) {
        this.itemsSelector = itemsSelector;
        this.titleSelector = titleSelector;
        this.imageSelector = imageSelector;
        this.linkSelector = linkSelector;
    }

    public static WebsiteType getFromUrl(String url){
        try {
            String name = url.split("/")[2].toUpperCase(Locale.ROOT).replace('.', '_');

            return WebsiteType.valueOf(name);
        }catch (Exception e){
            return null;
        }
    }

    public String getTitleSelector() {
        return titleSelector;
    }

    public String getImageSelector() {
        return imageSelector;
    }

    public String getLinkSelector() {
        return linkSelector;
    }

    public String getItemsSelector() {
        return itemsSelector;
    }
}
