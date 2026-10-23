package agence.metier;

import java.util.ArrayList;
import java.util.List;

/** Double de test uniquement ; aucun faux serveur dans l'application. */
public final class FauxHotel implements HotelGateway {
    public final String cle;
    public List<Offre> offres = List.of();
    public RefusMetier refusRecherche;
    public RefusMetier refusReservation;
    public boolean panne;
    public boolean prixIncorrect;
    public final List<String> demandes = new ArrayList<>();

    public FauxHotel(String cle) { this.cle = cle; }
    @Override public String cle() { return cle; }
    @Override public String nom() { return "Hotel " + cle; }
    @Override public List<Offre> chercher(Recherche recherche, IdentiteAgence agence)
            throws RefusMetier, IncidentTechnique {
        if (panne) throw new IncidentTechnique("Hotel arrete.");
        if (refusRecherche != null) throw refusRecherche;
        return offres;
    }
    @Override public Reservation.Confirmation reserver(Reservation demande, IdentiteAgence agence, PaiementFictif paiement)
            throws RefusMetier, IncidentTechnique {
        if (panne) throw new IncidentTechnique("Delai depasse.");
        if (refusReservation != null) throw refusReservation;
        boolean rejeu = demandes.contains(demande.identifiantDemande());
        demandes.add(demande.identifiantDemande());
        Offre offre = demande.offre();
        return new Reservation.Confirmation("RES-TEST", demande.identifiantDemande(), offre.identifiant(), offre.hotel(),
                offre.arrivee(), offre.depart(), prixIncorrect ? offre.prixTotal().add(java.math.BigDecimal.ONE) : offre.prixTotal(),
                offre.devise(), rejeu);
    }
}
