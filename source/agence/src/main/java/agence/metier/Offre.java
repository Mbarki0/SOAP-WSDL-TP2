package agence.metier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Offre(String hotelCle, String identifiant, String hotel, String ville,
                    int etoiles, String chambre, int capacite, LocalDate arrivee,
                    LocalDate depart, BigDecimal prixTotal, String devise) {
    public Offre {
        Objects.requireNonNull(hotelCle);
        Objects.requireNonNull(identifiant);
        Objects.requireNonNull(hotel);
        Objects.requireNonNull(arrivee);
        Objects.requireNonNull(depart);
        Objects.requireNonNull(prixTotal);
        Objects.requireNonNull(devise);
    }
}
