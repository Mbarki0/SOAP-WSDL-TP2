package agence.metier;

import java.util.List;

/** Frontiere cliente : aucun type SOAP ou engendre ne sort de cette interface. */
public interface HotelGateway {
    String cle();
    String nom();
    List<Offre> chercher(Recherche recherche, IdentiteAgence agence) throws RefusMetier, IncidentTechnique;
    Reservation.Confirmation reserver(Reservation demande, IdentiteAgence agence, PaiementFictif paiement)
            throws RefusMetier, IncidentTechnique;
}
