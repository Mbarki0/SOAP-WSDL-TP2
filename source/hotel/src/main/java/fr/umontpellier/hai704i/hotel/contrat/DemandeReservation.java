package fr.umontpellier.hai704i.hotel.contrat;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "identifiantAgence", "secretPresente", "identifiantOffre",
        "voyageur", "paiement", "identifiantDemande"
})
public class DemandeReservation {
    private String identifiantAgence;
    private String secretPresente;
    private String identifiantOffre;
    private Voyageur voyageur;
    private Paiement paiement;
    private String identifiantDemande;

    public DemandeReservation() {
    }

    public String getIdentifiantAgence() {
        return identifiantAgence;
    }

    public void setIdentifiantAgence(String identifiantAgence) {
        this.identifiantAgence = identifiantAgence;
    }

    public String getSecretPresente() {
        return secretPresente;
    }

    public void setSecretPresente(String secretPresente) {
        this.secretPresente = secretPresente;
    }

    public String getIdentifiantOffre() {
        return identifiantOffre;
    }

    public void setIdentifiantOffre(String identifiantOffre) {
        this.identifiantOffre = identifiantOffre;
    }

    public Voyageur getVoyageur() {
        return voyageur;
    }

    public void setVoyageur(Voyageur voyageur) {
        this.voyageur = voyageur;
    }

    public Paiement getPaiement() {
        return paiement;
    }

    public void setPaiement(Paiement paiement) {
        this.paiement = paiement;
    }

    public String getIdentifiantDemande() {
        return identifiantDemande;
    }

    public void setIdentifiantDemande(String identifiantDemande) {
        this.identifiantDemande = identifiantDemande;
    }
}

