package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;



@Entity
@Data
@Table(name = "indexes")
public class Indexes {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @ManyToOne()
    @JoinColumn(name = "page_id", referencedColumnName = "id", nullable = false)
    private Page pageByIndex;

    @ManyToOne()
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", nullable = false)
    private Lemma lemmaByIndex;

    private float frequency;
}