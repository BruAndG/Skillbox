package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "page", indexes = {@Index(name = "idx_page_path_site_id", columnList = "path, site_id", unique = true)})
@NamedEntityGraph(name = "PageEntity.indexes", attributeNodes = @NamedAttributeNode("indexes"))
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntity;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(nullable = true)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "pageEntity", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<IndexEntity> indexes = new HashSet<>();
}
