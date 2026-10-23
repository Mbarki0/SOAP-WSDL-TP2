package agence.metier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Reservation(String identifiantDemande, Offre offre, Voyageur voyageur) {
    public Reservation {
        if (identifiantDemande == null || identifiantDemande.isBlank())
            throw new IllegalArgumentException("Identifiant de demande absent.");
        Objects.requireNonNull(offre);
        Objects.requireNonNull(voyageur);
    }

    public record Confirmation(String reference, String identifiantDemande, String identifiantOffre,
                               String hotel, LocalDate arrivee, LocalDate depart,
                               BigDecimal prix, String devise, boolean rejeu) { }
}
