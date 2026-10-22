package fr.umontpellier.hai704i.hotel.contrat;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"numeroCarte", "moisExpiration", "anneeExpiration", "cvc"})
public class Paiement {
    private String numeroCarte;
    private int moisExpiration;
    private int anneeExpiration;
    private String cvc;

    public Paiement() {
    }

    public String getNumeroCarte() {
        return numeroCarte;
    }

    public void setNumeroCarte(String numeroCarte) {
        this.numeroCarte = numeroCarte;
    }

    public int getMoisExpiration() {
        return moisExpiration;
    }

    public void setMoisExpiration(int moisExpiration) {
        this.moisExpiration = moisExpiration;
    }

    public int getAnneeExpiration() {
        return anneeExpiration;
    }

    public void setAnneeExpiration(int anneeExpiration) {
        this.anneeExpiration = anneeExpiration;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }
}

