package fr.umontpellier.hai704i.hotel.contrat;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "identifiantOffre", "nomHotel", "ville", "nombreEtoiles",
        "descriptionChambre", "capacite", "dateArrivee", "dateDepart",
        "prixTotal", "devise"
})
public class Offre {
    private String identifiantOffre;
    private String nomHotel;
    private String ville;
    private int nombreEtoiles;
    private String descriptionChambre;
    private int capacite;
    @XmlSchemaType(name = "date")
    private XMLGregorianCalendar dateArrivee;
    @XmlSchemaType(name = "date")
    private XMLGregorianCalendar dateDepart;
    private BigDecimal prixTotal;
    private String devise;

    public Offre() {
    }

    public String getIdentifiantOffre() {
        return identifiantOffre;
    }

    public void setIdentifiantOffre(String identifiantOffre) {
        this.identifiantOffre = identifiantOffre;
    }

    public String getNomHotel() {
        return nomHotel;
    }

    public void setNomHotel(String nomHotel) {
        this.nomHotel = nomHotel;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public int getNombreEtoiles() {
        return nombreEtoiles;
    }

    public void setNombreEtoiles(int nombreEtoiles) {
        this.nombreEtoiles = nombreEtoiles;
    }

    public String getDescriptionChambre() {
        return descriptionChambre;
    }

    public void setDescriptionChambre(String descriptionChambre) {
        this.descriptionChambre = descriptionChambre;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public XMLGregorianCalendar getDateArrivee() {
        return dateArrivee;
    }

    public void setDateArrivee(XMLGregorianCalendar dateArrivee) {
        this.dateArrivee = dateArrivee;
    }

    public XMLGregorianCalendar getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(XMLGregorianCalendar dateDepart) {
        this.dateDepart = dateDepart;
    }

    public BigDecimal getPrixTotal() {
        return prixTotal;
    }

    public void setPrixTotal(BigDecimal prixTotal) {
        this.prixTotal = prixTotal;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }
}

