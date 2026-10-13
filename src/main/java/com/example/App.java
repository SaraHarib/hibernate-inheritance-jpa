package com.example;

import com.example.model.joined.Developpeur;
import com.example.model.joined.Employe;
import com.example.model.joined.Manager;
import com.example.model.singletable.Moto;
import com.example.model.singletable.Vehicule;
import com.example.model.singletable.Voiture;
import com.example.model.tableperclass.Electronique;
import com.example.model.tableperclass.Livre;
import com.example.model.tableperclass.Produit;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory factory =
                Persistence.createEntityManagerFactory("hibernate-inheritance");
        try {
            EntityManager em = factory.createEntityManager();
            try {
                executerDemonstration(em);
            } catch (RuntimeException exception) {
                EntityTransaction transaction = em.getTransaction();
                if (transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (RuntimeException erreurRollback) {
                        exception.addSuppressed(erreurRollback);
                    }
                }
                System.err.println("Échec de la démonstration : " + exception.getMessage());
                throw exception;
            } finally {
                em.close();
            }
        } finally {
            factory.close();
        }

        System.out.println();
        System.out.println("Démonstration des stratégies d'héritage terminée avec succès.");
    }

    private static void executerDemonstration(EntityManager em) {
        EntityTransaction transaction = em.getTransaction();

        afficherSection("1. SINGLE_TABLE - VEHICULES");
        Voiture voiture = new Voiture("Dacia", 5);
        Moto moto = new Moto("Yamaha", false);
        transaction.begin();
        em.persist(voiture);
        em.persist(moto);
        transaction.commit();

        // On vide le contexte pour relire les objets depuis H2.
        em.clear();
        List<Vehicule> vehicules = em.createQuery(
                "SELECT v FROM Vehicule v ORDER BY v.id", Vehicule.class).getResultList();
        verifier(vehicules.size() == 2, "Deux véhicules doivent être enregistrés.");
        afficherListe("Véhicules enregistrés (requête polymorphique)", vehicules);

        afficherSection("2. JOINED - EMPLOYES");
        Developpeur developpeur = new Developpeur("Yassine El Amrani", "Java");
        Manager manager = new Manager("Salma Berrada", 4);
        transaction.begin();
        em.persist(developpeur);
        em.persist(manager);
        transaction.commit();

        em.clear();
        List<Employe> employes = em.createQuery(
                "SELECT e FROM Employe e ORDER BY e.id", Employe.class).getResultList();
        verifier(employes.size() == 2, "Deux employés doivent être enregistrés.");
        afficherListe("Employés enregistrés (requête polymorphique)", employes);

        afficherSection("3. TABLE_PER_CLASS - PRODUITS");
        Livre livre = new Livre("Architecture logicielle", "Martin Fowler");
        Electronique electronique = new Electronique("Tablette graphique", 24);
        transaction.begin();
        em.persist(livre);
        em.persist(electronique);
        transaction.commit();

        em.clear();
        List<Produit> produits = em.createQuery(
                "SELECT p FROM Produit p ORDER BY p.id", Produit.class).getResultList();
        verifier(produits.size() == 2, "Deux produits doivent être enregistrés.");
        afficherListe("Produits enregistrés (requête polymorphique)", produits);

        afficherSection("4. RECHERCHES PAR TYPE");
        List<Voiture> voitures = em.createQuery(
                "SELECT v FROM Voiture v ORDER BY v.id", Voiture.class).getResultList();
        List<Developpeur> developpeurs = em.createQuery(
                "SELECT d FROM Developpeur d ORDER BY d.id", Developpeur.class).getResultList();
        List<Livre> livres = em.createQuery(
                "SELECT l FROM Livre l ORDER BY l.id", Livre.class).getResultList();
        verifier(voitures.size() == 1 && developpeurs.size() == 1 && livres.size() == 1,
                "Chaque recherche par type doit retourner un objet.");
        afficherListe("Toutes les voitures", voitures);
        afficherListe("Tous les développeurs", developpeurs);
        afficherListe("Tous les livres", livres);

        afficherSection("5. MODIFICATION");
        System.out.println("Voiture : marque Dacia -> Dacia Spring");
        System.out.println("Développeur : langage Java -> Kotlin");
        System.out.println("Livre : auteur Martin Fowler -> Eric Evans");

        transaction.begin();
        // Les objets relus sont gérés : Hibernate détecte leurs modifications au commit.
        em.find(Voiture.class, voiture.getId()).setMarque("Dacia Spring");
        em.find(Developpeur.class, developpeur.getId()).setLangage("Kotlin");
        em.find(Livre.class, livre.getId()).setAuteur("Eric Evans");
        transaction.commit();

        em.clear();
        Voiture voitureModifiee = em.find(Voiture.class, voiture.getId());
        Developpeur developpeurModifie = em.find(Developpeur.class, developpeur.getId());
        Livre livreModifie = em.find(Livre.class, livre.getId());
        verifier(voitureModifiee != null && "Dacia Spring".equals(voitureModifiee.getMarque()),
                "La modification de la voiture doit être enregistrée.");
        verifier(developpeurModifie != null && "Kotlin".equals(developpeurModifie.getLangage()),
                "La modification du développeur doit être enregistrée.");
        verifier(livreModifie != null && "Eric Evans".equals(livreModifie.getAuteur()),
                "La modification du livre doit être enregistrée.");
        System.out.println();
        System.out.println("Après commit et relecture depuis H2 :");
        afficherObjet(voitureModifiee);
        afficherObjet(developpeurModifie);
        afficherObjet(livreModifie);
        System.out.println("[OK] Les trois modifications sont enregistrées.");

        afficherSection("6. SUPPRESSION");
        System.out.println("Suppression de la moto Yamaha, du manager Salma Berrada");
        System.out.println("et du produit électronique Tablette graphique.");
        transaction.begin();
        em.remove(em.find(Moto.class, moto.getId()));
        em.remove(em.find(Manager.class, manager.getId()));
        em.remove(em.find(Electronique.class, electronique.getId()));
        transaction.commit();

        em.clear();
        verifier(em.find(Vehicule.class, moto.getId()) == null,
                "La moto doit avoir été supprimée.");
        verifier(em.find(Employe.class, manager.getId()) == null,
                "Le manager doit avoir été supprimé, y compris sa ligne Employe.");
        verifier(em.find(Produit.class, electronique.getId()) == null,
                "Le produit électronique doit avoir été supprimé.");

        vehicules = em.createQuery(
                "SELECT v FROM Vehicule v ORDER BY v.id", Vehicule.class).getResultList();
        employes = em.createQuery(
                "SELECT e FROM Employe e ORDER BY e.id", Employe.class).getResultList();
        produits = em.createQuery(
                "SELECT p FROM Produit p ORDER BY p.id", Produit.class).getResultList();
        verifier(vehicules.size() == 1 && vehicules.get(0) instanceof Voiture
                        && voiture.getId().equals(vehicules.get(0).getId())
                        && "Dacia Spring".equals(vehicules.get(0).getMarque()),
                "Seule la voiture modifiée doit rester.");
        verifier(employes.size() == 1 && employes.get(0) instanceof Developpeur
                        && developpeur.getId().equals(employes.get(0).getId())
                        && "Kotlin".equals(((Developpeur) employes.get(0)).getLangage()),
                "Seul le développeur modifié doit rester.");
        verifier(produits.size() == 1 && produits.get(0) instanceof Livre
                        && livre.getId().equals(produits.get(0).getId())
                        && "Eric Evans".equals(((Livre) produits.get(0)).getAuteur()),
                "Seul le livre modifié doit rester.");
        afficherListe("Véhicules restants", vehicules);
        afficherListe("Employés restants", employes);
        afficherListe("Produits restants", produits);
        System.out.println("[OK] Les trois suppressions sont confirmées ; les données restantes sont cohérentes.");

        afficherSection("7. ÉTAT FINAL");
        long nombreVehicules = em.createQuery(
                "SELECT COUNT(v) FROM Vehicule v", Long.class).getSingleResult();
        long nombreEmployes = em.createQuery(
                "SELECT COUNT(e) FROM Employe e", Long.class).getSingleResult();
        long nombreProduits = em.createQuery(
                "SELECT COUNT(p) FROM Produit p", Long.class).getSingleResult();
        verifier(nombreVehicules == 1 && nombreEmployes == 1 && nombreProduits == 1,
                "Il doit rester un objet dans chaque hiérarchie.");
        System.out.println();
        System.out.println("+----------------------+----------+");
        System.out.println("| Hiérarchie           | Restants |");
        System.out.println("+----------------------+----------+");
        System.out.printf("| Véhicules            | %8d |%n", nombreVehicules);
        System.out.printf("| Employés             | %8d |%n", nombreEmployes);
        System.out.printf("| Produits             | %8d |%n", nombreProduits);
        System.out.println("+----------------------+----------+");
        System.out.println("[OK] Toutes les vérifications ont réussi.");
    }

    private static void afficherSection(String titre) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println(titre);
        System.out.println("============================================================");
    }

    private static void afficherListe(String titre, List<?> objets) {
        System.out.println();
        System.out.println(titre + " — " + objets.size() + " résultat(s) :");
        for (Object objet : objets) {
            afficherObjet(objet);
        }
    }

    private static void afficherObjet(Object objet) {
        if (objet instanceof Voiture) {
            Voiture voiture = (Voiture) objet;
            System.out.printf("  - Voiture [id=%d] | Marque : %s | Portes : %d%n",
                    voiture.getId(), voiture.getMarque(), voiture.getNombrePortes());
        } else if (objet instanceof Moto) {
            Moto moto = (Moto) objet;
            System.out.printf("  - Moto [id=%d] | Marque : %s | Sidecar : %s%n",
                    moto.getId(), moto.getMarque(), moto.isSidecar() ? "oui" : "non");
        } else if (objet instanceof Developpeur) {
            Developpeur developpeur = (Developpeur) objet;
            System.out.printf("  - Developpeur [id=%d] | Nom : %s | Langage : %s%n",
                    developpeur.getId(), developpeur.getNom(), developpeur.getLangage());
        } else if (objet instanceof Manager) {
            Manager manager = (Manager) objet;
            System.out.printf("  - Manager [id=%d] | Nom : %s | Équipes : %d%n",
                    manager.getId(), manager.getNom(), manager.getNombreEquipes());
        } else if (objet instanceof Livre) {
            Livre livre = (Livre) objet;
            System.out.printf("  - Livre [id=%d] | Nom : %s | Auteur : %s%n",
                    livre.getId(), livre.getNom(), livre.getAuteur());
        } else if (objet instanceof Electronique) {
            Electronique electronique = (Electronique) objet;
            System.out.printf("  - Electronique [id=%d] | Nom : %s | Garantie : %d mois%n",
                    electronique.getId(), electronique.getNom(), electronique.getGarantie());
        }
    }

    private static void verifier(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
