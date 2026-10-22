package fr.umontpellier.hai704i.hotel.depot;

import fr.umontpellier.hai704i.hotel.metier.modele.AgencePartenaire;
import fr.umontpellier.hai704i.hotel.metier.modele.Chambre;
import fr.umontpellier.hai704i.hotel.metier.modele.Devis;
import fr.umontpellier.hai704i.hotel.metier.modele.ReservationEnregistree;

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

