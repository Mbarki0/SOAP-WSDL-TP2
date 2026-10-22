package hotel.metier.modele;

import java.math.BigDecimal;

public record Chambre(
        String identifiant,
        int capacite,
        String description,
        BigDecimal prixParNuit) {
}

