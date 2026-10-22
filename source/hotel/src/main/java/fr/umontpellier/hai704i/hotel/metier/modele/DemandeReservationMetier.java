package fr.umontpellier.hai704i.hotel.metier.modele;

import java.util.Arrays;

public record DemandeReservationMetier(
        String identifiantAgence,
        char[] secretPresente,
        String identifiantOffre,
        VoyageurPrincipal voyageur,
        CoordonneesPaiement paiement,
        String identifiantDemande) {

    public DemandeReservationMetier {
        secretPresente = secretPresente == null ? null : secretPresente.clone();
    }

    @Override
    public char[] secretPresente() {
        return secretPresente == null ? null : secretPresente.clone();
    }

    public void effacerSecret() {
        if (secretPresente != null) {
            Arrays.fill(secretPresente, '\0');
        }
    }
}


