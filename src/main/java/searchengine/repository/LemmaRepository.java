package searchengine.repository;


import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    long countBySiteByLemma(Site siteByLemma);


    @Modifying
    @Query(value = "delete from Lemma l where l.id=:id",
            nativeQuery = true)
    void deleteLemmaPathByPage(@Param("id") Integer id);


    @Query(value = "select l.id from Lemma l where l.lemma = ?1 and l.site_id = ?2", nativeQuery = true)
    Integer idToLemmaInt(String lemma, Integer siteId);


    @Query(value = "Select l.frequency from Lemma l where l.id = :idLemma")
    Integer frequencyById(@Param("idLemma") Integer idLemma);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Lemma l SET l.frequency = l.frequency - 1 WHERE l.id = :lemmaId")
    void updateLemmaFrequencyDelete(@Param("lemmaId") Integer lemmaId);


    @Query(value = "SELECT sum(l.frequency) FROM Lemma l " +
            "where l.lemma = ?1 and site_id IN ?2"
            , nativeQuery = true)
    Integer frequencyLemma(@Param("lemma") String lemma,
                           @Param("siteId") List<Integer> siteId);



    Lemma findByLemmaAndSiteByLemma(String lemmas, Site site);

    boolean existsByLemmaAndSiteByLemma(String lemmas, Site site);

    Lemma findLemmaById(Integer lemmaId);
}
