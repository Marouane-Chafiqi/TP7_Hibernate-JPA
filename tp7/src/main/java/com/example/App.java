package com.example;

import com.example.model.Auteur;
import com.example.model.Categorie;
import com.example.model.Livre;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;

public class App {

    private static EntityManagerFactory emf;
    private static Statistics stats;
    private static long debut;

    public static void main(String[] args) {
        emf = Persistence.createEntityManagerFactory("tp7");
        stats = emf.unwrap(SessionFactory.class).getStatistics();

        creerDonnees();

        testN1();
        testJoinFetch();
        testEntityGraph();
        testCacheEntite();
        testCacheRequete();
        testComparaison();

        emf.close();
    }

    private static void creerDonnees() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Categorie roman = new Categorie("Roman");
        Categorie sf = new Categorie("Science-Fiction");
        Categorie fantasy = new Categorie("Fantasy");
        Categorie policier = new Categorie("Policier");
        em.persist(roman);
        em.persist(sf);
        em.persist(fantasy);
        em.persist(policier);

        Auteur hugo = new Auteur("Hugo", "Victor", "hugo@example.com");
        ajouterLivre(hugo, "Les Miserables", 1862, "isbn-1", roman);
        ajouterLivre(hugo, "Notre-Dame de Paris", 1831, "isbn-2", roman);

        Auteur asimov = new Auteur("Asimov", "Isaac", "asimov@example.com");
        ajouterLivre(asimov, "Fondation", 1951, "isbn-3", sf);
        ajouterLivre(asimov, "Les Robots", 1950, "isbn-4", sf);

        Auteur tolkien = new Auteur("Tolkien", "J.R.R.", "tolkien@example.com");
        ajouterLivre(tolkien, "Le Seigneur des Anneaux", 1954, "isbn-5", fantasy);
        ajouterLivre(tolkien, "Le Hobbit", 1937, "isbn-6", fantasy);

        Auteur christie = new Auteur("Christie", "Agatha", "christie@example.com");
        for (int i = 1; i <= 20; i++) {
            ajouterLivre(christie, "Mystere " + i, 1920 + i, "isbn-m" + i, policier);
        }

        em.persist(hugo);
        em.persist(asimov);
        em.persist(tolkien);
        em.persist(christie);

        em.getTransaction().commit();
        em.close();
        System.out.println("Donnees creees : 4 auteurs, 26 livres");
    }

    private static void ajouterLivre(Auteur auteur, String titre, int annee, String isbn, Categorie categorie) {
        Livre livre = new Livre(titre, annee, isbn);
        livre.addCategorie(categorie);
        auteur.addLivre(livre);
    }

    private static void demarrer(String titre) {
        System.out.println("\n=== " + titre + " ===");
        emf.getCache().evictAll();
        stats.clear();
        debut = System.currentTimeMillis();
    }

    private static void afficherSql() {
        System.out.println("Requetes SQL : " + stats.getPrepareStatementCount());
        System.out.println("Temps : " + (System.currentTimeMillis() - debut) + " ms");
    }

    private static void testN1() {
        demarrer("TEST 1 : probleme N+1");
        EntityManager em = emf.createEntityManager();

        List<Auteur> auteurs = em.createQuery("SELECT a FROM Auteur a", Auteur.class).getResultList();
        int nbLivres = 0;
        for (Auteur a : auteurs) {
            for (Livre l : a.getLivres()) {
                nbLivres++;
                l.getCategories().size();
            }
        }

        em.close();
        System.out.println("Livres lus : " + nbLivres);
        afficherSql();
    }

    private static void testJoinFetch() {
        demarrer("TEST 2 : JOIN FETCH");
        EntityManager em = emf.createEntityManager();

        List<Auteur> auteurs = em.createQuery(
                "SELECT DISTINCT a FROM Auteur a LEFT JOIN FETCH a.livres l LEFT JOIN FETCH l.categories",
                Auteur.class).getResultList();
        int nbLivres = 0;
        for (Auteur a : auteurs) {
            for (Livre l : a.getLivres()) {
                nbLivres++;
                l.getCategories().size();
            }
        }

        em.close();
        System.out.println("Livres lus : " + nbLivres);
        afficherSql();
    }

    private static void testEntityGraph() {
        demarrer("TEST 3 : Entity Graph");
        EntityManager em = emf.createEntityManager();

        EntityGraph<?> graph = em.getEntityGraph("graph.Livre.categoriesEtAuteur");
        List<Livre> livres = em.createQuery("SELECT DISTINCT l FROM Livre l", Livre.class)
                .setHint("javax.persistence.fetchgraph", graph)
                .getResultList();
        for (Livre l : livres) {
            l.getAuteur().getNom();
            l.getCategories().size();
        }

        em.close();
        System.out.println("Livres lus : " + livres.size());
        afficherSql();
    }

    private static void testCacheEntite() {
        demarrer("TEST 4 : cache de second niveau (find)");

        for (int i = 1; i <= 2; i++) {
            stats.clear();
            EntityManager em = emf.createEntityManager();
            em.find(Auteur.class, 1L);
            em.close();
            System.out.println("Acces " + i + " -> SQL : " + stats.getPrepareStatementCount()
                    + " | cache hit : " + stats.getSecondLevelCacheHitCount()
                    + " | cache miss : " + stats.getSecondLevelCacheMissCount());
        }
    }

    private static void testCacheRequete() {
        demarrer("TEST 5 : cache de requete");

        for (int i = 1; i <= 2; i++) {
            stats.clear();
            EntityManager em = emf.createEntityManager();
            em.createQuery("SELECT a FROM Auteur a WHERE a.nom = :nom", Auteur.class)
                    .setParameter("nom", "Hugo")
                    .setHint("org.hibernate.cacheable", true)
                    .getResultList();
            em.close();
            System.out.println("Execution " + i + " -> SQL : " + stats.getPrepareStatementCount()
                    + " | query cache hit : " + stats.getQueryCacheHitCount()
                    + " | query cache miss : " + stats.getQueryCacheMissCount());
        }
    }

    private static void testComparaison() {
        int n = 1000;

        demarrer("TEST 6 : " + n + " lectures SANS cache");
        for (int i = 0; i < n; i++) {
            emf.getCache().evictAll();
            lireLivre(i % 26 + 1);
        }
        afficherSql();

        demarrer("TEST 6 : " + n + " lectures AVEC cache");
        for (int i = 0; i < n; i++) {
            lireLivre(i % 26 + 1);
        }
        afficherSql();
        System.out.println("Cache hit : " + stats.getSecondLevelCacheHitCount());
    }

    private static void lireLivre(long id) {
        EntityManager em = emf.createEntityManager();
        em.find(Livre.class, id);
        em.close();
    }
}
