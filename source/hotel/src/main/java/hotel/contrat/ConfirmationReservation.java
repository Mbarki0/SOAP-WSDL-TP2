package hotel.contrat;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "referenceReservation", "identifiantDemande", "identifiantOffre", "nomHotel",
        "dateArrivee", "dateDepart", "prixConfirme", "devise", "rejeu"
})
public class ConfirmationReservation {
    private String referenceReservation;
    private String identifiantDemande;
    private String identifiantOffre;
    private String nomHotel;
    @XmlSchemaType(name = "date")
    private XMLGregorianCalendar dateArrivee;
    @XmlSchemaType(name = "date")
    private XMLGregorianCalendar dateDepart;
    private BigDecimal prixConfirme;
    private String devise;
    private boolean rejeu;

    public ConfirmationReservation() {
    }

    public String getReferenceReservation() {
        return referenceReservation;
    }

    public void setReferenceReservation(String referenceReservation) {
        this.referenceReservation = referenceReservation;
    }

    public String getIdentifiantDemande() {
        return identifiantDemande;
    }

    public void setIdentifiantDemande(String identifiantDemande) {
        this.identifiantDemande = identifiantDemande;
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

    public BigDecimal getPrixConfirme() {
        return prixConfirme;
    }

    public void setPrixConfirme(BigDecimal prixConfirme) {
        this.prixConfirme = prixConfirme;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public boolean isRejeu() {
        return rejeu;
    }

    public void setRejeu(boolean rejeu) {
        this.rejeu = rejeu;
    }
}
