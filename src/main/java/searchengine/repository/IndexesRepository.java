package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.Indexes;
import searchengine.model.Page;

import java.util.List;

public interface IndexesRepository  extends JpaRepository<Indexes, Integer> {
    @Modifying
    @Query(value = "SELECT lemma_id from Indexes i where i.page_id =:pageId",
            nativeQuery = true)
    List<Integer> lemmaIdByPath( @Param("pageId") Integer idPath);

    @Modifying
    @Query(value = "delete from Indexes i where i.lemma_id=:id",
            nativeQuery = true)
    void deleteIndexPathByPage( @Param("id") Integer id);

    @Override
    @Modifying
    @Query("DELETE FROM Indexes")
    void deleteAll();
}
