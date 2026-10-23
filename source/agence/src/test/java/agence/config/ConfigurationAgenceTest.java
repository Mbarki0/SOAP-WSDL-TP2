package agence.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationAgenceTest {
    @TempDir Path temporaire;

    @Test void chargeLaConfigurationEmbarqueeSansServeur() throws Exception {
        try (var configuration = ConfigurationAgence.charger("agence.properties")) {
            assertEquals("AG-ALPHA", configuration.identite().identifiant());
            assertEquals(2, configuration.hotels().size());
            assertEquals(1500, configuration.connexionMs());
        }
    }

    @Test void adressesEtAgencePeuventChangerSansCompilation() throws Exception {
        Path fichier = temporaire.resolve("agence.properties");
        Files.writeString(fichier, "agence.identifiant=AG-BETA\nhotels=autre\nhotel.autre.nom=Autre hotel\n"
                + "hotel.autre.url=http://localhost:9876/autre\ndelai.connexion.ms=123\ndelai.reponse.ms=456\n");
        try (var configuration = ConfigurationAgence.charger(fichier.toString())) {
            assertEquals("AG-BETA", configuration.identite().identifiant());
            assertEquals("http://localhost:9876/autre", configuration.hotels().get(0).adresse());
            assertEquals(123, configuration.connexionMs()); assertEquals(456, configuration.reponseMs());
        }
    }

    @Test void refuseUneConfigurationIncomplete() throws Exception {
        Path fichier = temporaire.resolve("incorrect.properties");
        Files.writeString(fichier, "agence.identifiant=AG-BETA\n");
        assertThrows(IllegalArgumentException.class, () -> ConfigurationAgence.charger(fichier.toString()));
    }
}
