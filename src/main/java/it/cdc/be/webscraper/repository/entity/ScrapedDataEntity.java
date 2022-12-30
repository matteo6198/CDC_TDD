package it.cdc.be.webscraper.repository.entity;


import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "SCRAPED_DATA")
public class ScrapedDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String website;
    private String title;
    private LocalDate dateArticle;
    private String link;
    private String body;
    private String imageUrl;
    private String category;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
}
