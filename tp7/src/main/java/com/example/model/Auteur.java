package com.example.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auteurs")
@Cacheable
public class Auteur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true)
    private String email;

    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL)
    private List<Livre> livres = new ArrayList<>();

    public Auteur() {
    }

    public Auteur(String nom, String prenom, String email) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
    }

    public void addLivre(Livre livre) {
        livres.add(livre);
        livre.setAuteur(this);
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public List<Livre> getLivres() { return livres; }
}
