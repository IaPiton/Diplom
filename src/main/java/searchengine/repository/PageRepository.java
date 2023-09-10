package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    long countBySiteByPage(Site siteByPage);
    @Override
    @Modifying
    @Query("DELETE FROM Page")
    void deleteAll();
}
