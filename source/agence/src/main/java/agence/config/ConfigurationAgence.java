package agence.config;

import agence.metier.IdentiteAgence;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

public record ConfigurationAgence(IdentiteAgence identite, List<Hotel> hotels,
                                  int connexionMs, int reponseMs) implements AutoCloseable {
    public record Hotel(String cle, String nom, String adresse) { }

    public ConfigurationAgence {
        hotels = List.copyOf(hotels);
        if (connexionMs < 1 || reponseMs < 1)
            throw new IllegalArgumentException("Les delais doivent etre strictement positifs.");
    }

    public static ConfigurationAgence charger(String fichier) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = ouvrir(fichier)) { properties.load(reader); }
        String id = obligatoire(properties, "agence.identifiant");
        List<Hotel> hotels = new ArrayList<>();
        var cles = new HashSet<String>();
        for (String partie : obligatoire(properties, "hotels").split(",", -1)) {
            String cle = partie.trim();
            if (cle.isEmpty() || !cles.add(cle))
                throw new IllegalArgumentException("La liste des hotels contient une cle vide ou dupliquee.");
            hotels.add(new Hotel(cle, obligatoire(properties, "hotel." + cle + ".nom"),
                    obligatoire(properties, "hotel." + cle + ".url")));
        }
        int connexion = entier(properties, "delai.connexion.ms", "1500");
        int reponse = entier(properties, "delai.reponse.ms", "2500");
        if (connexion < 1 || reponse < 1)
            throw new IllegalArgumentException("Les delais doivent etre strictement positifs.");
        String secret = System.getenv("AGENCE_SECRET");
        if (secret == null || secret.isEmpty()) secret = properties.getProperty("agence.secret", "");
        char[] caracteres = secret.toCharArray();
        properties.remove("agence.secret");
        try { return new ConfigurationAgence(new IdentiteAgence(id, caracteres), hotels, connexion, reponse); }
        finally { Arrays.fill(caracteres, '\0'); }
    }

    private static Reader ouvrir(String fichier) throws IOException {
        Path path = Path.of(fichier);
        if (Files.isRegularFile(path)) return Files.newBufferedReader(path, StandardCharsets.UTF_8);
        var stream = ConfigurationAgence.class.getClassLoader().getResourceAsStream(fichier);
        if (stream == null) throw new IOException("Fichier de configuration introuvable.");
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    private static String obligatoire(Properties properties, String cle) {
        String valeur = properties.getProperty(cle);
        if (valeur == null || valeur.isBlank()) throw new IllegalArgumentException("Propriete manquante : " + cle);
        return valeur.trim();
    }

    private static int entier(Properties properties, String cle, String defaut) {
        try { return Integer.parseInt(properties.getProperty(cle, defaut).trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Entier invalide pour " + cle); }
    }

    @Override public void close() { identite.close(); }
}
