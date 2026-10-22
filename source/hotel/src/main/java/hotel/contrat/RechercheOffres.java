package hotel.contrat;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "identifiantAgence", "ville", "dateArrivee", "dateDepart",
        "nombrePersonnes", "nombreEtoilesMinimum", "prixMaximum"
})
public class RechercheOffres {
    private String identifiantAgence;
    private String ville;
    @XmlSchemaType(name = "date")
    private XMLGregorianCalendar dateArrivee;
    @XmlSchemaType(name = "date")
    private XMLGregorianCalendar dateDepart;
    private int nombrePersonnes;
    private int nombreEtoilesMinimum;
    private BigDecimal prixMaximum;

    public RechercheOffres() {
    }

    public String getIdentifiantAgence() {
        return identifiantAgence;
    }

    public void setIdentifiantAgence(String identifiantAgence) {
        this.identifiantAgence = identifiantAgence;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
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

    public int getNombrePersonnes() {
        return nombrePersonnes;
    }

    public void setNombrePersonnes(int nombrePersonnes) {
        this.nombrePersonnes = nombrePersonnes;
    }

    public int getNombreEtoilesMinimum() {
        return nombreEtoilesMinimum;
    }

    public void setNombreEtoilesMinimum(int nombreEtoilesMinimum) {
        this.nombreEtoilesMinimum = nombreEtoilesMinimum;
    }

    public BigDecimal getPrixMaximum() {
        return prixMaximum;
    }

    public void setPrixMaximum(BigDecimal prixMaximum) {
        this.prixMaximum = prixMaximum;
    }
}

