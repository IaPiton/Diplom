package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Data
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne()
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site siteByLemma;

    @Column(name = "lemma")
    private String lemma;
    private Integer frequency;
//    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
//    private List<Index> index = new ArrayList<>();
}
