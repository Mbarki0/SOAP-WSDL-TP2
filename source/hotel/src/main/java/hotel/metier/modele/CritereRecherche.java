package hotel.metier.modele;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CritereRecherche(
        String identifiantAgence,
        String ville,
        LocalDate dateArrivee,
        LocalDate dateDepart,
        int nombrePersonnes,
        int nombreEtoilesMinimum,
        BigDecimal prixMaximum) {
}

