package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


import java.util.Objects;


@Data
@Entity
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "page_sequence")
    @SequenceGenerator(name = "page_sequence", allocationSize = 100)
    private int id;
    @ManyToOne()
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site siteByPage;
    @Column(columnDefinition = "TEXT")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return id == page.id && code == page.code
                && Objects.equals(siteByPage, page.siteByPage) && Objects.equals(path, page.path)
                && Objects.equals(content, page.content);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id, siteByPage, path, code, content);
    }
}
