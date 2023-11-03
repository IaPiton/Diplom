package searchengine.repository;

import jakarta.persistence.EntityManager;
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
    Lemma findByIdAndSiteByLemma(int id, Site siteByLemma);
    long countBySiteByLemma(Site siteByLemma);
    @Override
    @Modifying
    @Query("DELETE FROM Lemma")
    void deleteAll();


    @Transactional
    @Modifying
    @Query(value = "delete from Lemma l where l.id=:id",
            nativeQuery = true)
    void deleteLemmaPathByPage(@Param("id") Integer id);


    @Transactional
    @Query(value = "select * from Lemma where lemma.lemma in :lemma and lemma.site_id in :siteId and lemma.frequency < 150", nativeQuery = true)
    List<Lemma> findByLemmaAndSiteOrderByFrequency(@Param("lemma") List<String> lemma,
                                                   @Param("siteId") List<Integer> siteId);


    boolean existsByLemmaAndSiteByLemma (String lemma, Site site );

    @Transactional
    @Modifying (flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Lemma l SET l.frequency = l.frequency + 1 WHERE l.siteByLemma = :site AND l.lemma = :lemma")
    void updateLemmaFrequency(@Param("site") Site site, @Param("lemma") String lemma);


    @Query(value = "select * from Lemma l where l.lemma = ?1 and l.site_id = ?2", nativeQuery = true)
    Lemma idToLemma(String lemma, Integer siteId);
    @Transactional
    @Query(value = "select * from Lemma l where l.lemma IN ?1 and l.site_id IN ?2", nativeQuery = true)
    List<Lemma> idToLemmaList(List <String> lemma, List<Integer> siteId);

    @Query(value = "Select l.frequency from Lemma l where l.id = :idLemma")
    Integer frequencyById (@Param("idLemma") Integer idLemma);

    @Transactional
    @Modifying (flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Lemma l SET l.frequency = l.frequency - 1 WHERE l.id = :lemmaId")
    void updateLemmaFrequencyDelete(@Param("lemmaId") Integer lemmaId);
}
