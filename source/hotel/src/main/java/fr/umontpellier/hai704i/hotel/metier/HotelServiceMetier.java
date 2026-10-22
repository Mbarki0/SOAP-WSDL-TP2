package fr.umontpellier.hai704i.hotel.metier;

import fr.umontpellier.hai704i.hotel.depot.HotelRepository;
import fr.umontpellier.hai704i.hotel.metier.exception.AgenceNonPartenaireException;
import fr.umontpellier.hai704i.hotel.metier.exception.AuthentificationMetierException;
import fr.umontpellier.hai704i.hotel.metier.exception.DemandeMetierInvalideException;
import fr.umontpellier.hai704i.hotel.metier.exception.OffreMetierIndisponibleException;
import fr.umontpellier.hai704i.hotel.metier.exception.PaiementMetierInvalideException;
import fr.umontpellier.hai704i.hotel.metier.modele.AgencePartenaire;
import fr.umontpellier.hai704i.hotel.metier.modele.Chambre;
import fr.umontpellier.hai704i.hotel.metier.modele.Confirmation;
import fr.umontpellier.hai704i.hotel.metier.modele.CoordonneesPaiement;
import fr.umontpellier.hai704i.hotel.metier.modele.CritereRecherche;
import fr.umontpellier.hai704i.hotel.metier.modele.DemandeReservationMetier;
import fr.umontpellier.hai704i.hotel.metier.modele.Devis;
import fr.umontpellier.hai704i.hotel.metier.modele.IdentiteHotel;
import fr.umontpellier.hai704i.hotel.metier.modele.ReservationEnregistree;
import fr.umontpellier.hai704i.hotel.metier.modele.VoyageurPrincipal;
import fr.umontpellier.hai704i.hotel.securite.VerificateurSecret;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class HotelServiceMetier {
    private static final String DEVISE = "EUR";

    private final IdentiteHotel hotel;
    private final HotelRepository repository;
    private final VerificateurSecret verificateurSecret;

    public HotelServiceMetier(
            IdentiteHotel hotel,
            HotelRepository repository,
            VerificateurSecret verificateurSecret) {
        this.hotel = Objects.requireNonNull(hotel);
        this.repository = Objects.requireNonNull(repository);
        this.verificateurSecret = Objects.requireNonNull(verificateurSecret);
    }

    public synchronized List<Devis> chercherOffres(CritereRecherche critere)
            throws AgenceNonPartenaireException, DemandeMetierInvalideException {
        validerRecherche(critere);
        AgencePartenaire agence = repository.trouverAgence(critere.identifiantAgence())
                .orElseThrow(() -> new AgenceNonPartenaireException(
                        "Agence " + critere.identifiantAgence() + " non partenaire"));

        if (!hotel.ville().equalsIgnoreCase(critere.ville())
                || hotel.nombreEtoiles() < critere.nombreEtoilesMinimum()) {
            return List.of();
        }

        long nombreNuits = ChronoUnit.DAYS.between(critere.dateArrivee(), critere.dateDepart());
        List<Devis> resultat = new ArrayList<>();
        for (Chambre chambre : repository.listerChambres()) {
            if (chambre.capacite() < critere.nombrePersonnes()
                    || !repository.chambreDisponible(
                    chambre.identifiant(), critere.dateArrivee(), critere.dateDepart())) {
                continue;
            }

            BigDecimal prix = chambre.prixParNuit()
                    .multiply(BigDecimal.valueOf(nombreNuits))
                    .multiply(agence.coefficientTarif())
                    .setScale(2, RoundingMode.HALF_UP);
            if (prix.compareTo(critere.prixMaximum()) > 0) {
                continue;
            }

            Devis devis = new Devis(
                    "OFF-" + UUID.randomUUID(),
                    agence.identifiant(),
                    chambre.identifiant(),
                    hotel.nom(),
                    hotel.ville(),
                    hotel.nombreEtoiles(),
                    chambre.description(),
                    chambre.capacite(),
                    critere.dateArrivee(),
                    critere.dateDepart(),
                    critere.nombrePersonnes(),
                    prix,
                    DEVISE);
            repository.enregistrerDevis(devis);
            resultat.add(devis);
        }
        return List.copyOf(resultat);
    }

    public synchronized Confirmation reserver(DemandeReservationMetier demande)
            throws AuthentificationMetierException,
            DemandeMetierInvalideException,
            PaiementMetierInvalideException,
            OffreMetierIndisponibleException {
        if (demande == null) {
            throw new DemandeMetierInvalideException("La demande de reservation est absente");
        }

        try {
            validerStructureReservation(demande);
            AgencePartenaire agence = repository.trouverAgence(demande.identifiantAgence())
                    .orElseThrow(() -> new AuthentificationMetierException("Authentification refusee"));
            char[] secret = demande.secretPresente();
            try {
                if (!verificateurSecret.verifier(secret, agence)) {
                    throw new AuthentificationMetierException("Authentification refusee");
                }
            } finally {
                if (secret != null) {
                    java.util.Arrays.fill(secret, '\0');
                }
            }

            var reservationExistante = repository.trouverReservation(
                    demande.identifiantAgence(), demande.identifiantDemande());
            if (reservationExistante.isPresent()) {
                ReservationEnregistree reservation = reservationExistante.get();
                if (!reservation.identifiantOffre().equals(demande.identifiantOffre())) {
                    throw new DemandeMetierInvalideException(
                            "Identifiant de demande deja utilise pour une autre offre");
                }
                return reservation.confirmation().avecRejeu(true);
            }

            validerVoyageur(demande.voyageur());
            validerPaiement(demande.paiement());
            Devis devis = repository.trouverDevis(demande.identifiantOffre())
                    .filter(offre -> offre.identifiantAgence().equals(demande.identifiantAgence()))
                    .orElseThrow(() -> new OffreMetierIndisponibleException("Offre inconnue ou indisponible"));

            if (!repository.chambreDisponible(
                    devis.identifiantChambre(), devis.dateArrivee(), devis.dateDepart())) {
                throw new OffreMetierIndisponibleException("L'offre n'est plus disponible");
            }

            Confirmation confirmation = new Confirmation(
                    "RES-" + UUID.randomUUID(),
                    demande.identifiantDemande(),
                    devis.identifiantOffre(),
                    devis.nomHotel(),
                    devis.dateArrivee(),
                    devis.dateDepart(),
                    devis.prixTotal(),
                    devis.devise(),
                    false);
            repository.enregistrerReservation(new ReservationEnregistree(
                    demande.identifiantAgence(),
                    demande.identifiantDemande(),
                    devis.identifiantOffre(),
                    devis.identifiantChambre(),
                    confirmation));
            return confirmation;
        } finally {
            demande.effacerSecret();
        }
    }

    private void validerRecherche(CritereRecherche critere) throws DemandeMetierInvalideException {
        if (critere == null) {
            throw new DemandeMetierInvalideException("La demande de recherche est absente");
        }
        if (vide(critere.identifiantAgence()) || vide(critere.ville())) {
            throw new DemandeMetierInvalideException("L'agence et la ville sont obligatoires");
        }
        if (critere.dateArrivee() == null || critere.dateDepart() == null
                || !critere.dateDepart().isAfter(critere.dateArrivee())) {
            throw new DemandeMetierInvalideException("Les dates du sejour sont invalides");
        }
        if (critere.nombrePersonnes() < 1) {
            throw new DemandeMetierInvalideException("Le nombre de personnes doit etre positif");
        }
        if (critere.nombreEtoilesMinimum() < 0 || critere.nombreEtoilesMinimum() > 5) {
            throw new DemandeMetierInvalideException("Le nombre d'etoiles doit etre compris entre 0 et 5");
        }
        if (critere.prixMaximum() == null || critere.prixMaximum().signum() <= 0) {
            throw new DemandeMetierInvalideException("Le prix maximum doit etre positif");
        }
    }

    private void validerStructureReservation(DemandeReservationMetier demande)
            throws DemandeMetierInvalideException {
        if (demande == null) {
            throw new DemandeMetierInvalideException("La demande de reservation est absente");
        }
        if (vide(demande.identifiantAgence())
                || vide(demande.identifiantOffre())
                || vide(demande.identifiantDemande())) {
            throw new DemandeMetierInvalideException(
                    "L'agence, l'offre et l'identifiant de demande sont obligatoires");
        }
    }

    private void validerVoyageur(VoyageurPrincipal voyageur) throws DemandeMetierInvalideException {
        if (voyageur == null || vide(voyageur.nom()) || vide(voyageur.prenom())) {
            throw new DemandeMetierInvalideException("Le nom et le prenom du voyageur sont obligatoires");
        }
    }

    private void validerPaiement(CoordonneesPaiement paiement) throws PaiementMetierInvalideException {
        if (paiement == null
                || paiement.numeroCarte() == null
                || !paiement.numeroCarte().matches("\\d{16}")) {
            throw new PaiementMetierInvalideException("Le numero de carte fictif doit contenir 16 chiffres");
        }
        if (paiement.moisExpiration() < 1 || paiement.moisExpiration() > 12) {
            throw new PaiementMetierInvalideException("Le mois d'expiration est invalide");
        }
        YearMonth expiration = YearMonth.of(paiement.anneeExpiration(), paiement.moisExpiration());
        if (expiration.isBefore(YearMonth.now())) {
            throw new PaiementMetierInvalideException("La carte fictive est expiree");
        }
        if (paiement.cvc() == null || !paiement.cvc().matches("\\d{3}")) {
            throw new PaiementMetierInvalideException("Le CVC fictif doit contenir 3 chiffres");
        }
    }

    private boolean vide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
