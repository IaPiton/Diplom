package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    long countBySiteByLemma(Site siteByLemma);

    @Transactional
    @Modifying
    @Query(value = "delete from Lemma l where l.id=:id",
            nativeQuery = true)
    void deleteLemmaPathByPage(@Param("id") Integer id);

    boolean existsByLemmaAndSiteByLemma (String lemma, Site site );

    @Transactional
    @Modifying
    @Query("UPDATE Lemma l SET l.frequency = l.frequency + 1 WHERE l.siteByLemma = :site AND l.lemma = :lemma")
    void updateLemmaFrequency(@Param("site") Site site, @Param("lemma") String lemma);


    @Query(value = "select * from Lemma l where l.lemma = ?1 and l.site_id = ?2", nativeQuery = true)
    Lemma idToLemma(String lemma, Integer siteId);

    @Query(value = "Select l.frequency from Lemma l where l.id = :idLemma")
    Integer frequencyById (@Param("idLemma") Integer idLemma);

    @Transactional
    @Modifying (flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Lemma l SET l.frequency = l.frequency - 1 WHERE l.id = :lemmaId")
    void updateLemmaFrequencyDelete(@Param("lemmaId") Integer lemmaId);
}
