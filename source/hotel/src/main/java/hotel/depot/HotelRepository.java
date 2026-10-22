package hotel.depot;

import hotel.metier.modele.AgencePartenaire;
import hotel.metier.modele.Chambre;
import hotel.metier.modele.Devis;
import hotel.metier.modele.ReservationEnregistree;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HotelRepository {
    Optional<AgencePartenaire> trouverAgence(String identifiantAgence);

    List<Chambre> listerChambres();

    void enregistrerDevis(Devis devis);

    Optional<Devis> trouverDevis(String identifiantOffre);

    boolean chambreDisponible(String identifiantChambre, LocalDate dateArrivee, LocalDate dateDepart);

    Optional<ReservationEnregistree> trouverReservation(String identifiantAgence, String identifiantDemande);

    void enregistrerReservation(ReservationEnregistree reservation);
}

