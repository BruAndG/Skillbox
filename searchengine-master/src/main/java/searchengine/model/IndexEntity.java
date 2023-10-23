package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Setter
@Getter
@Table(name = "`index`")
@NamedEntityGraph(name = "IndexEntity.lemmaEntity", attributeNodes = @NamedAttributeNode("lemmaEntity"))
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity pageEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaEntity lemmaEntity;

    @Column(name = "`rank`", nullable = false)
    private float rank;

}
