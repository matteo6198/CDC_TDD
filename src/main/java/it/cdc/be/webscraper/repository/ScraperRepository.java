package it.cdc.be.webscraper.repository;

import it.cdc.be.webscraper.repository.entity.ScrapedDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("ScraperRepository")
public interface ScraperRepository extends JpaRepository<ScrapedDataEntity, Long> {

    @Query(value = "SELECT * FROM SCRAPED_DATA ORDER BY DATE_ARTICLE DESC", nativeQuery = true)
    List<ScrapedDataEntity> findScrapedDataOrderedByDateArticle();

    @Query(value = "SELECT * FROM SCRAPED_DATA WHERE WEBSITE IN (:websites) ORDER BY DATE_ARTICLE DESC", nativeQuery = true)
    List<ScrapedDataEntity> findScrapedDataByWebsite(@Param("websites") List<String> websites);
}
