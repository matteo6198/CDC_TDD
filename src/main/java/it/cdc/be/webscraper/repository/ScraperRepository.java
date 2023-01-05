package it.cdc.be.webscraper.repository;

import it.cdc.be.webscraper.repository.entity.ScrapedDataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository("ScraperRepository")
public interface ScraperRepository extends JpaRepository<ScrapedDataEntity, Long> {

    @Query(value = "SELECT * FROM SCRAPED_DATA ORDER BY DATE_ARTICLE DESC", nativeQuery = true)
    Page<ScrapedDataEntity> findScrapedDataOrderedByDateArticle(Pageable pageable);

    @Query(value = "SELECT * FROM SCRAPED_DATA WHERE WEBSITE IN (:websites) ORDER BY DATE_ARTICLE DESC", nativeQuery = true)
    Page<ScrapedDataEntity> findScrapedDataByWebsite(@Param("websites") List<String> websites, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM SCRAPED_DATA WHERE DATE_ARTICLE <= :date", nativeQuery = true)
    void deleteScrapedDataByDateArticle(@Param("date") LocalDate oldestDate);
}
