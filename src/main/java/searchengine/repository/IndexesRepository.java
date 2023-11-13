package searchengine.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Indexes;


import java.util.List;

public interface IndexesRepository extends JpaRepository<Indexes, Integer> {
    @Transactional
    @Modifying
    @Query(value = "SELECT lemma_id from Indexes i where i.page_id = :pageId",
            nativeQuery = true)
    List<Integer> findLemmaByPath(@Param("pageId") Integer idPath);

    @Transactional
    @Modifying
    @Query(value = "delete from Indexes i where i.lemma_id=:id",
            nativeQuery = true)
    void deleteIndexPathByPage(@Param("id") Integer id);

    @Transactional
    @Query(value = "SELECT i.id, i.page_id, i.lemma_id, i.rank_lemma FROM Indexes i " +
            "JOIN Lemma l ON i.lemma_id = l.id " +
            "where l.lemma IN ?1 and l.site_id IN ?2"
            , nativeQuery = true
    )
    List<Indexes> findIndexByLemma(@Param("lemma") List<String> lemma,
                                   @Param("siteId") List<Integer> siteId,
                                   @Param("lemma") Pageable pageable);

    @Transactional
    @Query(value = "SELECT count(distinct i.page_id) FROM Indexes i " +
            "JOIN Lemma l ON i.lemma_id = l.id " +
            "where l.lemma IN ?1 and l.site_id IN ?2"
            , nativeQuery = true
    )
    Integer countIndex(@Param("lemma") List<String> lemma,
                       @Param("siteId") List<Integer> siteId);
}
