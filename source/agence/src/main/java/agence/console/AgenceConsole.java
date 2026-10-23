package agence.console;

import agence.metier.*;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** La console ne connait que le metier : aucun proxy, aucune URL de service. */
public final class AgenceConsole {
    private final AgenceService service;
    private final EntreeConsole entree;
    private final PrintWriter out;
    private List<Offre> offres = List.of();
    private Reservation derniereDemande;

    public AgenceConsole(AgenceService service, EntreeConsole entree, PrintWriter out) {
        this.service = service;
        this.entree = entree;
        this.out = out;
    }

    public void executer() {
        out.println("Agence de voyage - demonstration avec paiements exclusivement fictifs.");
        try {
            boolean continuer = true;
            while (continuer) {
                out.println("\n1 Rechercher | 2 Reserver une offre | 3 Reessayer la derniere demande | 4 Revoir les offres | 0 Quitter");
                try {
                    switch (entree.lire("Votre choix : ")) {
                        case "1" -> chercher();
                        case "2" -> choisirEtReserver();
                        case "3" -> {
                            if (derniereDemande == null) out.println("Aucune demande a renvoyer.");
                            else envoyer(derniereDemande);
                        }
                        case "4" -> afficherOffres();
                        case "0" -> continuer = false;
                        default -> out.println("Choix invalide. Utilisez un numero du menu.");
                    }
                } catch (IllegalArgumentException e) {
                    out.println("Saisie invalide : " + e.getMessage());
                }
            }
        } catch (EntreeConsole.FinSaisie e) {
            out.println("\nFin de la saisie.");
        } finally {
            offres = List.of();
            derniereDemande = null;
            out.println("Agence fermee. Au revoir.");
            out.flush();
        }
    }

    private void chercher() {
        String ville = texte("Ville : ");
        LocalDate arrivee = date("Arrivee (AAAA-MM-JJ) : ");
        LocalDate depart = date("Depart (AAAA-MM-JJ) : ");
        int personnes = entier("Nombre de personnes : ", 1, 1000);
        int etoiles = entier("Etoiles minimum (0-5) : ", 0, 5);
        BigDecimal budget = saisir(() -> {
            try {
                BigDecimal valeur = new BigDecimal(entree.lire("Budget TOTAL maximal en EUR : ").replace(',', '.'));
                if (valeur.signum() <= 0) throw new IllegalArgumentException("Le budget doit etre positif.");
                return valeur;
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Saisissez un montant decimal."); }
        });
        ResultatRecherche resultat = service.chercher(new Recherche(ville, arrivee, depart, personnes, etoiles, budget));
        offres = resultat.offres();
        for (var incident : resultat.incidents()) {
            String nature = incident.nature() == ResultatRecherche.Nature.REFUS_METIER ? "Refus" : "Incident";
            out.println(nature + " - " + incident.hotel() + " : " + incident.message());
        }
        afficherOffres();
    }

    private void afficherOffres() {
        if (offres.isEmpty()) { out.println("Aucune offre disponible dans la liste actuelle."); return; }
        out.println("Offres triees par devise, prix total puis nom d'hotel :");
        for (int i = 0; i < offres.size(); i++) {
            Offre offre = offres.get(i);
            out.printf("%d. %s (%d etoiles) - %s - %d personnes max - %s au %s - TOTAL %s %s%n",
                    i + 1, offre.hotel(), offre.etoiles(), offre.chambre(), offre.capacite(),
                    offre.arrivee(), offre.depart(), offre.prixTotal().toPlainString(), offre.devise());
        }
    }

    private void choisirEtReserver() {
        if (offres.isEmpty()) { out.println("Effectuez d'abord une recherche."); return; }
        afficherOffres();
        int choix = entier("Numero d'offre (0 pour revenir) : ", 0, offres.size());
        if (choix == 0) return;
        Voyageur voyageur = new Voyageur(texte("Nom du voyageur : "), texte("Prenom du voyageur : "));
        derniereDemande = service.preparerReservation(offres.get(choix - 1), voyageur);
        envoyer(derniereDemande);
    }

    private void envoyer(Reservation demande) {
        out.println("Identifiant de demande : " + demande.identifiantDemande());
        out.println("Saisissez uniquement des coordonnees FICTIVES. Elles ne seront pas reaffichees ni enregistrees.");
        try (PaiementFictif paiement = saisir(this::paiement)) {
            var confirmation = service.reserver(demande, paiement);
            out.println("Reservation confirmee : " + confirmation.reference());
            out.println("Prix annonce = prix confirme : " + confirmation.prix().toPlainString() + " " + confirmation.devise());
            if (confirmation.rejeu()) out.println("Demande deja traitee : meme reservation, aucun doublon.");
        } catch (RefusMetier e) {
            out.println("Refus metier [" + e.code() + "] : " + e.getMessage());
        } catch (IncidentTechnique e) {
            out.println("Incident technique : " + e.getMessage());
            out.println("La reservation peut avoir ete effectuee. Le choix 3 reutilise le MEME identifiant de demande.");
        }
    }

    private PaiementFictif paiement() {
        char[] numero = entree.confidentiel("Numero de carte fictif (16 chiffres, saisie masquee en terminal) : ");
        char[] cvc = null;
        try {
            int mois = entier("Mois d'expiration (1-12) : ", 1, 12);
            int annee = entier("Annee d'expiration : ", 1, 9999);
            cvc = entree.confidentiel("CVC fictif (3 chiffres, saisie masquee en terminal) : ");
            return new PaiementFictif(numero, mois, annee, cvc);
        } finally {
            Arrays.fill(numero, '\0');
            if (cvc != null) Arrays.fill(cvc, '\0');
        }
    }

    private String texte(String invite) {
        return saisir(() -> {
            String valeur = entree.lire(invite);
            if (valeur.isBlank()) throw new IllegalArgumentException("Le champ est obligatoire.");
            return valeur;
        });
    }

    private LocalDate date(String invite) {
        return saisir(() -> {
            try { return LocalDate.parse(entree.lire(invite)); }
            catch (DateTimeParseException e) { throw new IllegalArgumentException("Utilisez une date valide au format AAAA-MM-JJ."); }
        });
    }

    private int entier(String invite, int min, int max) {
        return saisir(() -> {
            try {
                int valeur = Integer.parseInt(entree.lire(invite));
                if (valeur < min || valeur > max) throw new IllegalArgumentException("Choisissez un entier entre " + min + " et " + max + ".");
                return valeur;
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Saisissez un nombre entier."); }
        });
    }

    private <T> T saisir(Supplier<T> action) {
        while (true) {
            try { return action.get(); }
            catch (IllegalArgumentException e) { out.println("Saisie invalide : " + e.getMessage()); }
        }
    }
}
