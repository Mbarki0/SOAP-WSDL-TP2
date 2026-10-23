package agence.soap;

import agence.config.ConfigurationAgence;
import agence.metier.*;
import agence.soap.genere.*;
import com.sun.net.httpserver.HttpServer;
import jakarta.xml.ws.BindingProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "integration.hotel", matches = "true")
@Timeout(60)
class HotelIntegrationTest {
    private static final Path SORTIE = HotelProcess.BASE.resolve("target/captures");

    private Recherche recherche() {
        LocalDate arrivee = LocalDate.now().plusDays(30);
        return new Recherche("Montpellier", arrivee, arrivee.plusDays(3), 2, 3, new BigDecimal("1000"));
    }
    private IdentiteAgence identite(String id) { return new IdentiteAgence(id, HotelProcess.SECRET.toCharArray()); }
    private PaiementFictif paiement() {
        return new PaiementFictif("4242424242424242".toCharArray(), 12, LocalDate.now().getYear() + 2, "732".toCharArray());
    }
    private SoapHotelGateway gateway(String cle, String adresse, String capture) {
        return new SoapHotelGateway(new ConfigurationAgence.Hotel(cle, "Hotel " + cle, adresse), 500, 1000,
                provider -> {
                    if (capture != null) provider.getBinding().setHandlerChain(List.of(new CaptureSoapHandler(SORTIE, capture)));
                });
    }

    @Test void parcoursReelDeuxProcessusReservationsEtPannePartielle() throws Exception {
        Path temporaire = HotelProcess.BASE.resolve("target/hotels/parcours");
        try (HotelProcess arenes = new HotelProcess(temporaire, "hotel-arenes.properties", true);
             HotelProcess port = new HotelProcess(temporaire, "hotel-port.properties", true);
             IdentiteAgence alpha = identite("AG-ALPHA"); IdentiteAgence beta = identite("AG-BETA")) {
            var ga = gateway("arenes", arenes.adresse, "recherche");
            var gp = gateway("port", port.adresse, null);
            var service = new AgenceService(List.of(ga, gp), alpha);
            var resultat = service.chercher(recherche());
            assertEquals(4, resultat.offres().size()); assertTrue(resultat.incidents().isEmpty());
            assertEquals(new BigDecimal("214.20"), resultat.offres().get(0).prixTotal());
            var tarifsBeta = new AgenceService(List.of(gp), beta).chercher(recherche());
            assertEquals(new BigDecimal("231.84"), tarifsBeta.offres().get(0).prixTotal());

            var choix = resultat.offres().stream().filter(o -> o.hotelCle().equals("arenes")).findFirst().orElseThrow();
            var demande = service.preparerReservation(choix, new agence.metier.Voyageur("Martin", "Alice"));
            try (IdentiteAgence fausse = new IdentiteAgence("AG-ALPHA", "secret-errone".toCharArray()); var paiement = paiement()) {
                var refus = assertThrows(RefusMetier.class,
                        () -> gateway("arenes", arenes.adresse, "faute-authentification").reserver(demande, fausse, paiement));
                assertEquals(RefusMetier.Code.AUTHENTIFICATION_REFUSEE, refus.code());
            }
            Reservation.Confirmation initiale;
            try (var paiement = paiement()) {
                initiale = new AgenceService(List.of(gateway("arenes", arenes.adresse, "reservation")), alpha).reserver(demande, paiement);
                assertFalse(initiale.rejeu()); assertEquals(choix.prixTotal(), initiale.prix());
                var rejeu = new AgenceService(List.of(gateway("arenes", arenes.adresse, "reservation-rejeu")), alpha).reserver(demande, paiement);
                assertTrue(rejeu.rejeu()); assertEquals(initiale.reference(), rejeu.reference());
                var nouvelle = service.preparerReservation(choix, demande.voyageur());
                assertEquals(RefusMetier.Code.OFFRE_INDISPONIBLE, assertThrows(RefusMetier.class,
                        () -> gateway("arenes", arenes.adresse, "faute-offre-indisponible").reserver(nouvelle, alpha, paiement)).code());
            }
            try (IdentiteAgence inconnue = identite("AG-INCONNUE")) {
                assertEquals(RefusMetier.Code.AGENCE_INCONNUE, assertThrows(RefusMetier.class,
                        () -> gateway("arenes", arenes.adresse, "faute-agence-inconnue").chercher(recherche(), inconnue)).code());
            }
            capturerContrat(arenes.adresse);
            port.close();
            var partiel = service.chercher(recherche());
            assertFalse(partiel.offres().isEmpty());
            assertTrue(partiel.offres().stream().allMatch(o -> o.hotelCle().equals("arenes")));
            assertEquals(ResultatRecherche.Nature.PANNE_TECHNIQUE, partiel.incidents().get(0).nature());
            Files.writeString(SORTIE.resolve("validation.txt"), "Deux JVM hotel reelles ; quatre offres agregees avant reservation.\n"
                    + "Trois nuits : AG-ALPHA / Hotel du Port = 214.20 EUR ; AG-BETA = 231.84 EUR.\n"
                    + "Authentification refusee sans reservation ; succes ; prix identique ; rejeu meme reference ; offre indisponible.\n"
                    + "Hotel du Port arrete : offres des Arenes conservees et incident signale.\n"
                    + "Secrets des configurations temporaires remplaces par des empreintes de test ; sources de A inchangees.\n");
        }
    }

    @Test void configurationsOriginalesDeARepondentALaRecherche() throws Exception {
        try (HotelProcess arenes = new HotelProcess(HotelProcess.BASE.resolve("target/hotels/originaux"), "hotel-arenes.properties", false);
             HotelProcess port = new HotelProcess(HotelProcess.BASE.resolve("target/hotels/originaux"), "hotel-port.properties", false);
             IdentiteAgence alpha = identite("AG-ALPHA")) {
            var resultat = new AgenceService(List.of(gateway("arenes", arenes.adresse, null), gateway("port", port.adresse, null)), alpha).chercher(recherche());
            assertEquals(4, resultat.offres().size()); assertTrue(resultat.incidents().isEmpty());
        }
    }

    @Test void paiementInvalideEstUneFauteDuVraiServeur() throws Exception {
        try (HotelProcess hotel = new HotelProcess(HotelProcess.BASE.resolve("target/hotels/paiement"), "hotel-arenes.properties", true);
             IdentiteAgence alpha = identite("AG-ALPHA")) {
            var offre = gateway("arenes", hotel.adresse, null).chercher(recherche(), alpha).get(0);
            HotelPort proxy = new HotelService().getHotelSoapPort();
            BindingProvider provider = (BindingProvider) proxy;
            provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, hotel.adresse);
            provider.getBinding().setHandlerChain(List.of(new CaptureSoapHandler(SORTIE, "faute-paiement")));
            DemandeReservation demande = new DemandeReservation();
            demande.setIdentifiantAgence("AG-ALPHA"); demande.setSecretPresente(HotelProcess.SECRET);
            demande.setIdentifiantOffre(offre.identifiant()); demande.setIdentifiantDemande("REQ-PAIEMENT-INVALIDE");
            agence.soap.genere.Voyageur voyageur = new agence.soap.genere.Voyageur();
            voyageur.setNom("Martin"); voyageur.setPrenom("Alice"); demande.setVoyageur(voyageur);
            Paiement paiement = new Paiement(); paiement.setNumeroCarte("123"); paiement.setMoisExpiration(12);
            paiement.setAnneeExpiration(LocalDate.now().getYear() + 2); paiement.setCvc("732"); demande.setPaiement(paiement);
            try { assertThrows(PaiementInvalide.class, () -> proxy.reserver(demande)); }
            finally {
                demande.setSecretPresente(null); paiement.setNumeroCarte(null); paiement.setCvc(null);
                ((com.sun.xml.ws.Closeable) proxy).close();
            }
        }
    }

    @Test void delaiDeReponseEtAdresseErroneeNeBloquentPasLaRecherche() throws Exception {
        HttpServer lent = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        lent.createContext("/hotel", exchange -> {
            try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        lent.start();
        try (IdentiteAgence alpha = identite("AG-ALPHA")) {
            var gateway = new SoapHotelGateway(new ConfigurationAgence.Hotel("lent", "Hotel lent",
                    "http://localhost:" + lent.getAddress().getPort() + "/hotel"), 100, 100);
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertThrows(IncidentTechnique.class, () -> gateway.chercher(recherche(), alpha)));
            assertThrows(IncidentTechnique.class, () -> gateway("invalide", "adresse invalide", null).chercher(recherche(), alpha));
        } finally { lent.stop(0); }
    }

    private void capturerContrat(String adresse) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        for (String extension : List.of("wsdl", "xsd=1")) {
            var response = client.send(HttpRequest.newBuilder(URI.create(adresse + "?" + extension)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            Files.writeString(SORTIE.resolve(extension.equals("wsdl") ? "hotel.wsdl" : "hotel.xsd"), response.body(), StandardCharsets.UTF_8);
        }
    }
}
