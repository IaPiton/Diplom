package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import searchengine.model.Page;
import searchengine.model.Site;


import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    long countBySiteByPage(Site siteByPage);

    @Override
    void deleteAll();

       @Query(value = "SELECT p.id FROM Page p " +
            "JOIN Indexes i ON i.page_id = p.id " +
            "JOIN Lemma l ON i.lemma_id = l.id " +
            "where l.lemma = ?1 and l.site_Id IN ?2", nativeQuery = true)
    List<Integer> idByLemma(String lemma, List<Integer> siteId);

    boolean existsByPath(String path);

    Page findPageByPath(String path);
}

