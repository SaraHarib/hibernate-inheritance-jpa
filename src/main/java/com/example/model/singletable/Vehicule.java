package com.example.model.singletable;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
// Tous les véhicules sont stockés dans une seule table.
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
// Le discriminateur distingue les voitures des motos.
@DiscriminatorColumn(name = "vehicule_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String marque;

    public Vehicule() {
    }

    public Vehicule(String marque) {
        this.marque = marque;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    @Override
    public String toString() {
        return "Vehicule{id=" + id + ", marque='" + marque + "'}";
    }
}
