package hotel.metier.modele;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Confirmation(
        String referenceReservation,
        String identifiantDemande,
        String identifiantOffre,
        String nomHotel,
        LocalDate dateArrivee,
        LocalDate dateDepart,
        BigDecimal prixConfirme,
        String devise,
        boolean rejeu) {

    public Confirmation avecRejeu(boolean valeur) {
        return new Confirmation(
                referenceReservation,
                identifiantDemande,
                identifiantOffre,
                nomHotel,
                dateArrivee,
                dateDepart,
                prixConfirme,
                devise,
                valeur);
    }
}

