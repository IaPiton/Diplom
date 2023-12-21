package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;


import java.util.Collection;

@Data
@Entity
@Table(name = "page", indexes = @Index(columnList = "path"))
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site siteByPage;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "pageByIndex", cascade = CascadeType.ALL)
    private Collection<Indexes> pagesById;
}
