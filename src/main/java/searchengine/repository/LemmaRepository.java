package searchengine.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    long countBySiteByLemma(Site siteByLemma);

    @Transactional
    @Modifying
    @Query(value = "delete from Lemma l where l.id=:id",
            nativeQuery = true)
    void deleteLemmaPathByPage(@Param("id") Integer id);


    @Query(value = "select l.id from Lemma l where l.lemma = ?1 and l.site_id = ?2", nativeQuery = true)
    Integer idToLemmaInt(String lemma, Integer siteId);

    @Transactional
    @Query(value = "Select l.frequency from Lemma l where l.id = :idLemma")
    Integer frequencyById(@Param("idLemma") Integer idLemma);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Lemma l SET l.frequency = l.frequency - 1 WHERE l.id = :lemmaId")
    void updateLemmaFrequencyDelete(@Param("lemmaId") Integer lemmaId);
}
