package com.example.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Cacheable
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @ManyToMany(mappedBy = "categories")
    private Set<Livre> livres = new HashSet<>();

    public Categorie() {
    }

    public Categorie(String nom) {
        this.nom = nom;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public Set<Livre> getLivres() { return livres; }
}
