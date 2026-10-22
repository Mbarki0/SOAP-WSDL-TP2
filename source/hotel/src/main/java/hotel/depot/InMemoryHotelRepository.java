package hotel.depot;

import hotel.metier.modele.AgencePartenaire;
import hotel.metier.modele.Chambre;
import hotel.metier.modele.Devis;
import hotel.metier.modele.ReservationEnregistree;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryHotelRepository implements HotelRepository {
    private final Map<String, AgencePartenaire> agences;
    private final List<Chambre> chambres;
    private final Map<String, Devis> devis = new HashMap<>();
    private final Map<String, ReservationEnregistree> reservations = new HashMap<>();

    public InMemoryHotelRepository(List<Chambre> chambres, List<AgencePartenaire> agences) {
        this.chambres = List.copyOf(chambres);
        this.agences = new HashMap<>();
        agences.forEach(agence -> this.agences.put(agence.identifiant(), agence));
    }

    @Override
    public Optional<AgencePartenaire> trouverAgence(String identifiantAgence) {
        return Optional.ofNullable(agences.get(identifiantAgence));
    }

    @Override
    public List<Chambre> listerChambres() {
        return chambres;
    }

    @Override
    public void enregistrerDevis(Devis devis) {
        this.devis.put(devis.identifiantOffre(), devis);
    }

    @Override
    public Optional<Devis> trouverDevis(String identifiantOffre) {
        return Optional.ofNullable(devis.get(identifiantOffre));
    }

    @Override
    public boolean chambreDisponible(String identifiantChambre, LocalDate dateArrivee, LocalDate dateDepart) {
        return reservations.values().stream()
                .filter(reservation -> reservation.identifiantChambre().equals(identifiantChambre))
                .map(ReservationEnregistree::confirmation)
                .noneMatch(confirmation -> chevauche(
                        dateArrivee,
                        dateDepart,
                        confirmation.dateArrivee(),
                        confirmation.dateDepart()));
    }

    private boolean chevauche(
            LocalDate arriveeDemandee,
            LocalDate departDemande,
            LocalDate arriveeExistante,
            LocalDate departExistant) {
        return arriveeDemandee.isBefore(departExistant) && arriveeExistante.isBefore(departDemande);
    }

    @Override
    public Optional<ReservationEnregistree> trouverReservation(
            String identifiantAgence,
            String identifiantDemande) {
        return Optional.ofNullable(reservations.get(cleReservation(identifiantAgence, identifiantDemande)));
    }

    @Override
    public void enregistrerReservation(ReservationEnregistree reservation) {
        reservations.put(
                cleReservation(reservation.identifiantAgence(), reservation.identifiantDemande()),
                reservation);
    }

    private String cleReservation(String identifiantAgence, String identifiantDemande) {
        return identifiantAgence + "\u0000" + identifiantDemande;
    }
}

