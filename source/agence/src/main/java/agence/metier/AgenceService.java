package agence.metier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static agence.metier.ResultatRecherche.Nature.*;

public final class AgenceService {
    private final Map<String, HotelGateway> hotels = new LinkedHashMap<>();
    private final IdentiteAgence identite;

    public AgenceService(List<HotelGateway> gateways, IdentiteAgence identite) {
        this.identite = Objects.requireNonNull(identite);
        for (HotelGateway gateway : gateways) {
            if (hotels.putIfAbsent(gateway.cle(), gateway) != null)
                throw new IllegalArgumentException("Identifiant d'hotel duplique.");
        }
        if (hotels.isEmpty()) throw new IllegalArgumentException("Au moins un hotel doit etre configure.");
    }

    public ResultatRecherche chercher(Recherche recherche) {
        Objects.requireNonNull(recherche);
        List<Offre> offres = new ArrayList<>();
        List<ResultatRecherche.Incident> incidents = new ArrayList<>();
        for (HotelGateway gateway : hotels.values()) {
            try {
                offres.addAll(gateway.chercher(recherche, identite));
            } catch (RefusMetier e) {
                incidents.add(new ResultatRecherche.Incident(gateway.nom(), REFUS_METIER, e.getMessage()));
            } catch (IncidentTechnique e) {
                incidents.add(new ResultatRecherche.Incident(gateway.nom(), PANNE_TECHNIQUE, e.getMessage()));
            }
        }
        // Tous les hotels du contrat de demonstration utilisent EUR ; separer les devises sinon.
        offres.sort(Comparator.comparing(Offre::devise).thenComparing(Offre::prixTotal)
                .thenComparing(Offre::hotel).thenComparing(Offre::identifiant));
        return new ResultatRecherche(offres, incidents);
    }

    public Reservation preparerReservation(Offre offre, Voyageur voyageur) {
        if (!hotels.containsKey(offre.hotelCle())) throw new IllegalArgumentException("Hotel inconnu.");
        return new Reservation(UUID.randomUUID().toString(), offre, voyageur);
    }

    public Reservation.Confirmation reserver(Reservation demande, PaiementFictif paiement)
            throws RefusMetier, IncidentTechnique {
        if (!identite.possedeSecret())
            throw new RefusMetier(RefusMetier.Code.AUTHENTIFICATION_REFUSEE,
                    "Secret d'agence absent : configurez AGENCE_SECRET avant le lancement.");
        HotelGateway gateway = hotels.get(demande.offre().hotelCle());
        if (gateway == null) throw new IllegalArgumentException("Hotel inconnu.");
        var confirmation = gateway.reserver(demande, identite, paiement);
        Offre offre = demande.offre();
        if (confirmation == null || confirmation.reference() == null || confirmation.reference().isBlank()
                || !demande.identifiantDemande().equals(confirmation.identifiantDemande())
                || !offre.identifiant().equals(confirmation.identifiantOffre())
                || !offre.arrivee().equals(confirmation.arrivee()) || !offre.depart().equals(confirmation.depart())
                || confirmation.prix() == null || offre.prixTotal().compareTo(confirmation.prix()) != 0
                || !offre.devise().equals(confirmation.devise())) {
            throw new IncidentTechnique("Confirmation incoherente : verifiez la reservation aupres de l'hotel avant toute nouvelle demande.");
        }
        return confirmation;
    }
}
