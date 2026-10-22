package hotel.metier.modele;

public record ReservationEnregistree(
        String identifiantAgence,
        String identifiantDemande,
        String identifiantOffre,
        String identifiantChambre,
        Confirmation confirmation) {
}

