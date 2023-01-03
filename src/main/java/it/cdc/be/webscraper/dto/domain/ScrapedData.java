package it.cdc.be.webscraper.dto.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ScrapedData {
    private String website;
    private String title;
    private LocalDate dateArticle;
    private String link;
    private String body;
    private String imageUrl;
    private String category;

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDateArticle() {
        return dateArticle;
    }

    public void setDateArticle(LocalDate dateArticle) {
        this.dateArticle = dateArticle;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "ScrapedData{" +
                "website='" + website + '\'' +
                ", title='" + title + '\'' +
                ", dateArticle=" + dateArticle.format(DateTimeFormatter.ISO_DATE) +
                ", link='" + link + '\'' +
                ", body='" + body + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", category='" + category + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScrapedData data = (ScrapedData) o;
        return website.equals(data.website) && title.equals(data.title) && link.equals(data.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(website, title, link);
    }
}
