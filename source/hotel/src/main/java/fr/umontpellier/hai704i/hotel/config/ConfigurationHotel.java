package fr.umontpellier.hai704i.hotel.config;

import fr.umontpellier.hai704i.hotel.metier.modele.AgencePartenaire;
import fr.umontpellier.hai704i.hotel.metier.modele.Chambre;
import fr.umontpellier.hai704i.hotel.metier.modele.IdentiteHotel;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public record ConfigurationHotel(
        IdentiteHotel identite,
        int port,
        String chemin,
        List<Chambre> chambres,
        List<AgencePartenaire> agences) {

    public ConfigurationHotel {
        chambres = List.copyOf(chambres);
        agences = List.copyOf(agences);
    }

    public static ConfigurationHotel charger(String emplacement) throws IOException {
        Properties proprietes = new Properties();
        try (InputStream entree = ouvrir(emplacement)) {
            proprietes.load(entree);
        }

        IdentiteHotel identite = new IdentiteHotel(
                obligatoire(proprietes, "hotel.nom"),
                obligatoire(proprietes, "hotel.ville"),
                entier(proprietes, "hotel.etoiles"));

        return new ConfigurationHotel(
                identite,
                entier(proprietes, "hotel.port"),
                normaliserChemin(obligatoire(proprietes, "hotel.chemin")),
                lireChambres(obligatoire(proprietes, "hotel.chambres")),
                lireAgences(obligatoire(proprietes, "hotel.agences")));
    }

    private static InputStream ouvrir(String emplacement) throws IOException {
        Path chemin = Path.of(emplacement);
        if (Files.isRegularFile(chemin)) {
            return Files.newInputStream(chemin);
        }

        InputStream ressource = ConfigurationHotel.class.getClassLoader().getResourceAsStream(emplacement);
        if (ressource == null) {
            throw new IOException("Configuration introuvable : " + emplacement);
        }
        return ressource;
    }

    private static List<Chambre> lireChambres(String valeur) {
        List<Chambre> chambres = new ArrayList<>();
        for (String entree : valeur.split(",")) {
            String[] parties = entree.trim().split(":", 4);
            if (parties.length != 4) {
                throw new IllegalArgumentException("Configuration de chambre invalide");
            }
            chambres.add(new Chambre(
                    parties[0].trim(),
                    Integer.parseInt(parties[1].trim()),
                    parties[2].trim(),
                    new BigDecimal(parties[3].trim())));
        }
        return chambres;
    }

    private static List<AgencePartenaire> lireAgences(String valeur) {
        List<AgencePartenaire> agences = new ArrayList<>();
        Base64.Decoder decodeur = Base64.getDecoder();
        for (String entree : valeur.split(",")) {
            String[] parties = entree.trim().split(":", 5);
            if (parties.length != 5) {
                throw new IllegalArgumentException("Configuration d'agence invalide");
            }
            agences.add(new AgencePartenaire(
                    parties[0].trim(),
                    Integer.parseInt(parties[1].trim()),
                    decodeur.decode(parties[2].trim()),
                    decodeur.decode(parties[3].trim()),
                    new BigDecimal(parties[4].trim())));
        }
        return agences;
    }

    private static String obligatoire(Properties proprietes, String cle) {
        String valeur = proprietes.getProperty(cle);
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Propriete manquante : " + cle);
        }
        return valeur.trim();
    }

    private static int entier(Properties proprietes, String cle) {
        return Integer.parseInt(obligatoire(proprietes, cle));
    }

    private static String normaliserChemin(String chemin) {
        return chemin.startsWith("/") ? chemin : "/" + chemin;
    }

    public String adresse() {
        return "http://localhost:" + port + chemin;
    }
}
