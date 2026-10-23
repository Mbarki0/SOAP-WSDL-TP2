package agence.metier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgenceServiceTest {
    static Offre offre(String cle, String prix) {
        return new Offre(cle, "OFF-" + cle, "Hotel " + cle, "Montpellier", 3, "Double", 2,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 12), new BigDecimal(prix), "EUR");
    }
    static Recherche recherche() {
        return new Recherche("Montpellier", LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 12), 2, 3, new BigDecimal("500"));
    }
    static IdentiteAgence identite() { return new IdentiteAgence("AG-TEST", "secret-unitaire".toCharArray()); }
    static PaiementFictif paiement() { return new PaiementFictif("4242424242424242".toCharArray(), 12, LocalDate.now().getYear() + 2, "732".toCharArray()); }

    @Test void agregeEtTrieLesOffresDesDeuxHotels() {
        FauxHotel a = new FauxHotel("A"); a.offres = List.of(offre("A", "200"));
        FauxHotel b = new FauxHotel("B"); b.offres = List.of(offre("B", "150"));
        var resultat = new AgenceService(List.of(a, b), identite()).chercher(recherche());
        assertEquals(List.of("B", "A"), resultat.offres().stream().map(Offre::hotelCle).toList());
        assertTrue(resultat.incidents().isEmpty());
    }

    @Test void pannePartielleConserveLesOffresJoignables() {
        FauxHotel a = new FauxHotel("A"); a.panne = true;
        FauxHotel b = new FauxHotel("B"); b.offres = List.of(offre("B", "150"));
        var resultat = new AgenceService(List.of(a, b), identite()).chercher(recherche());
        assertEquals(1, resultat.offres().size());
        assertEquals(ResultatRecherche.Nature.PANNE_TECHNIQUE, resultat.incidents().get(0).nature());
    }

    @Test void refusDeRechercheEstDistinctDunePanne() {
        FauxHotel a = new FauxHotel("A");
        a.refusRecherche = new RefusMetier(RefusMetier.Code.AGENCE_INCONNUE, "Agence inconnue.");
        var resultat = new AgenceService(List.of(a), identite()).chercher(recherche());
        assertEquals(ResultatRecherche.Nature.REFUS_METIER, resultat.incidents().get(0).nature());
    }

    @Test void reservationEtRejeuGardentIdentifiantEtReference() throws Exception {
        FauxHotel a = new FauxHotel("A");
        var service = new AgenceService(List.of(a), identite());
        var demande = service.preparerReservation(offre("A", "200"), new Voyageur("Martin", "Alice"));
        try (var paiement = paiement()) {
            var premiere = service.reserver(demande, paiement);
            var rejeu = service.reserver(demande, paiement);
            assertFalse(premiere.rejeu()); assertTrue(rejeu.rejeu());
            assertEquals(premiere.reference(), rejeu.reference());
            assertEquals(a.demandes.get(0), a.demandes.get(1));
        }
        assertNotEquals(demande.identifiantDemande(), service.preparerReservation(demande.offre(), demande.voyageur()).identifiantDemande());
    }

    @ParameterizedTest @EnumSource(RefusMetier.Code.class)
    void conserveChaqueCategorieDeRefus(RefusMetier.Code code) {
        FauxHotel a = new FauxHotel("A"); a.refusReservation = new RefusMetier(code, "Refus attendu.");
        var service = new AgenceService(List.of(a), identite());
        var demande = service.preparerReservation(offre("A", "200"), new Voyageur("Martin", "Alice"));
        try (var paiement = paiement()) {
            assertEquals(code, assertThrows(RefusMetier.class, () -> service.reserver(demande, paiement)).code());
        }
    }

    @Test void confirmationAvecPrixDifferentEstSignalee() {
        FauxHotel a = new FauxHotel("A"); a.prixIncorrect = true;
        var service = new AgenceService(List.of(a), identite());
        var demande = service.preparerReservation(offre("A", "200"), new Voyageur("Martin", "Alice"));
        try (var paiement = paiement()) {
            assertThrows(IncidentTechnique.class, () -> service.reserver(demande, paiement));
        }
    }

    @Test void panneDeReservationNeDevientPasUnRefusMetier() {
        FauxHotel a = new FauxHotel("A"); a.panne = true;
        var service = new AgenceService(List.of(a), identite());
        var demande = service.preparerReservation(offre("A", "200"), new Voyageur("Martin", "Alice"));
        try (var paiement = paiement()) {
            assertThrows(IncidentTechnique.class, () -> service.reserver(demande, paiement));
        }
    }

    @Test void secretAbsentEstRefuseAvantAppel() {
        FauxHotel a = new FauxHotel("A");
        var service = new AgenceService(List.of(a), new IdentiteAgence("AG-TEST", null));
        var demande = service.preparerReservation(offre("A", "200"), new Voyageur("Martin", "Alice"));
        try (var paiement = paiement()) {
            assertThrows(RefusMetier.class, () -> service.reserver(demande, paiement));
            assertTrue(a.demandes.isEmpty());
        }
    }

    @Test void donneesConfidentiellesNeSontPasAfficheesEtSontEffacees() {
        var identite = identite(); var paiement = paiement();
        assertFalse(identite.toString().contains("secret-unitaire"));
        assertFalse(paiement.toString().contains("4242424242424242"));
        assertFalse(paiement.toString().contains("732"));
        paiement.close(); identite.close();
        assertEquals(0, paiement.copierNumero()[0]); assertEquals(0, paiement.copierCvc()[0]);
        assertEquals(0, identite.copierSecret()[0]);
    }

    @Test void valideRechercheVoyageurEtPaiementSansEchoDesValeurs() {
        assertThrows(IllegalArgumentException.class, () -> new Recherche("", LocalDate.now(), LocalDate.now(), 0, 9, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Voyageur("", "Alice"));
        String message = assertThrows(IllegalArgumentException.class,
                () -> new PaiementFictif("carte-invalide".toCharArray(), 12, 2030, "732".toCharArray())).getMessage();
        assertFalse(message.contains("carte-invalide"));
    }
}
