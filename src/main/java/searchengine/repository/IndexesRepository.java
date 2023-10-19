package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Indexes;
import searchengine.model.Page;

import java.util.List;

public interface IndexesRepository  extends JpaRepository<Indexes, Integer> {
    @Transactional
    @Modifying
    @Query(value = "SELECT lemma_id from Indexes i where i.page_id =:pageId",
            nativeQuery = true)
    List<Integer> lemmaIdByPath( @Param("pageId") Integer idPath);

    @Transactional
    @Modifying
    @Query(value = "delete from Indexes i where i.lemma_id=:id",
            nativeQuery = true)
    void deleteIndexPathByPage( @Param("id") Integer id);

    @Override
    @Modifying
    @Query("DELETE FROM Indexes")
    void deleteAll();

    @Transactional
    @Query(value = "select * from Indexes where Indexes.lemma_id in :lemmaId and Indexes.page_id in :pageId", nativeQuery = true)
    List<Indexes> findByPageAndLemmas(@Param("lemmaId") List<Integer> lemmaId,
                                      @Param("pageId") List<Integer> pageId);
}
