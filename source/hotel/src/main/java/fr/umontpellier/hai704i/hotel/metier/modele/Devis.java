package fr.umontpellier.hai704i.hotel.metier.modele;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Devis(
        String identifiantOffre,
        String identifiantAgence,
        String identifiantChambre,
        String nomHotel,
        String ville,
        int nombreEtoiles,
        String descriptionChambre,
        int capacite,
        LocalDate dateArrivee,
        LocalDate dateDepart,
        int nombrePersonnes,
        BigDecimal prixTotal,
        String devise) {
}

