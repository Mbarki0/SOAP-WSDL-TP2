package fr.umontpellier.hai704i.hotel.metier.modele;

public record CoordonneesPaiement(
        String numeroCarte,
        int moisExpiration,
        int anneeExpiration,
        String cvc) {
}

