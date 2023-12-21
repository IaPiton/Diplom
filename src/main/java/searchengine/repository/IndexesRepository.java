package searchengine.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Indexes;
import searchengine.model.Page;


import java.util.List;

@Repository
public interface IndexesRepository extends JpaRepository<Indexes, Integer> {
    @Transactional
    @Modifying
    @Query(value = "SELECT lemma_id from Indexes i where i.page_id = :pageId",
            nativeQuery = true)
    List<Integer> findLemmaByPath(@Param("pageId") Integer idPath);

    @Transactional
    @Query(value = "SELECT i.id, i.page_id, i.lemma_id, i.rank_lemma FROM Indexes i " +
            "JOIN Lemma l ON i.lemma_id = l.id " +
            "where l.lemma = ?1 and l.site_id IN ?2 "
            , nativeQuery = true
    )
    List<Indexes> findIndexByLemmas(@Param("lemma") String lemma,
                                    @Param("siteId") List<Integer> siteId);

    @Transactional
    @Query(value = "SELECT i.id, i.page_id, i.lemma_id, i.rank_lemma FROM Indexes i " +
            "JOIN Lemma l ON i.lemma_id = l.id " +
            "where l.lemma = ?2 AND i.page_id IN ?1"
            , nativeQuery = true
    )
    List<Indexes> findIndexByLemmaAndPage(@Param("pageId") List<Integer> pageId,
                                          @Param("lemma") String lemma,
                                          @Param("rank_lemma") Pageable pageable);

    List<Indexes> findIndexesByPageByIndex(Page page);
}



