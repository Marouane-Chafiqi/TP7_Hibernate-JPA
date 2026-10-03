package com.example.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "livres")
@Cacheable
@NamedEntityGraph(
        name = "graph.Livre.categoriesEtAuteur",
        attributeNodes = {
                @NamedAttributeNode("categories"),
                @NamedAttributeNode("auteur")
        }
)
public class Livre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(name = "annee_publication")
    private Integer anneePublication;

    @Column(name = "isbn", unique = true)
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private Auteur auteur;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "livre_categorie",
            joinColumns = @JoinColumn(name = "livre_id"),
            inverseJoinColumns = @JoinColumn(name = "categorie_id")
    )
    private Set<Categorie> categories = new HashSet<>();

    public Livre() {
    }

    public Livre(String titre, Integer anneePublication, String isbn) {
        this.titre = titre;
        this.anneePublication = anneePublication;
        this.isbn = isbn;
    }

    public void addCategorie(Categorie categorie) {
        categories.add(categorie);
        categorie.getLivres().add(this);
    }

    public Long getId() { return id; }
    public String getTitre() { return titre; }
    public Integer getAnneePublication() { return anneePublication; }
    public Auteur getAuteur() { return auteur; }
    public void setAuteur(Auteur auteur) { this.auteur = auteur; }
    public Set<Categorie> getCategories() { return categories; }
}
