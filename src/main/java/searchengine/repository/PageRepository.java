package searchengine.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    long countBySiteByPage(Site siteByPage);
    @Override
    @Modifying
    @Query("DELETE FROM Page")
    void deleteAll();
    @Transactional
    @Query(value = "SELECT * FROM Page p WHERE p.path = :url",
            nativeQuery = true)
    Integer findPathByPage( @Param("url") String path);
    @Transactional
    @Modifying
    @Query(value = "delete from Page p where p.path=:url",
            nativeQuery = true)
    void deletePathByPage( @Param("url") String path);

    @Transactional
    @Query(value = "SELECT p.code, p.id, p.site_id, p.content, p.path  FROM Page p JOIN Indexes ON p.id = Indexes.page_id WHERE Indexes.lemma_id IN ?", nativeQuery = true)
    List<Page> pageInIndex(List<Integer> lemmaId);


}

