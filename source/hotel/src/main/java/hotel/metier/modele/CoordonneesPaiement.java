package hotel.metier.modele;

public record CoordonneesPaiement(
        String numeroCarte,
        int moisExpiration,
        int anneeExpiration,
        String cvc) {
}

