package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;


import java.util.Collection;

@Data
@Entity
@Table(name = "page", uniqueConstraints = {@UniqueConstraint(columnNames = {"site_id", "path"})})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne()
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site siteByPage;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "pageByIndex")
    private Collection<Indexes> pagesById;
}
