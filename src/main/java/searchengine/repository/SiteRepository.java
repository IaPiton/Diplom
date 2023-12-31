package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {
    Site findByName(String siteName);

    List<Site> findByUrl(String siteUrl);
}
