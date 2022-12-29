package it.cdc.be.webscraper.repository;

import it.cdc.be.webscraper.repository.entity.ScrapedDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("ScraperRepository")
public interface ScraperRepository extends JpaRepository<ScrapedDataEntity, Long> {
}
