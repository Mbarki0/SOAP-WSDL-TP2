package fr.umontpellier.hai704i.hotel.metier;

import fr.umontpellier.hai704i.hotel.depot.InMemoryHotelRepository;
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
import fr.umontpellier.hai704i.hotel.metier.modele.VoyageurPrincipal;
import fr.umontpellier.hai704i.hotel.securite.VerificateurSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotelServiceMetierTest {
    private static final String AGENCE = "AG-TEST";
    private static final String SECRET = "secret-test";

    private HotelServiceMetier service;
    private LocalDate arrivee;
    private LocalDate depart;

    @BeforeEach
    void preparer() throws Exception {
        arrivee = LocalDate.now().plusDays(30);
        depart = arrivee.plusDays(2);
        byte[] sel = "sel-test-16octets".getBytes(StandardCharsets.UTF_8);
        AgencePartenaire agence = new AgencePartenaire(
                AGENCE, 10_000, sel, empreinte(SECRET, sel), new BigDecimal("0.90"));
        InMemoryHotelRepository repository = new InMemoryHotelRepository(
                List.of(new Chambre("CH-1", 2, "Double", new BigDecimal("100.00"))),
                List.of(agence));
        service = new HotelServiceMetier(
                new IdentiteHotel("Hotel Test", "Montpellier", 4),
                repository,
                new VerificateurSecret());
    }

    @Test
    void rechercheAppliqueLeTarifPartenaireAuSejour() throws Exception {
        Devis devis = premiereOffre();

        assertEquals(new BigDecimal("180.00"), devis.prixTotal());
        assertEquals(AGENCE, devis.identifiantAgence());
        assertEquals(arrivee, devis.dateArrivee());
        assertEquals(depart, devis.dateDepart());
    }

    @Test
    void agenceNonPartenaireRecoitUnRefus() {
        CritereRecherche recherche = new CritereRecherche(
                "AG-INCONNUE", "Montpellier", arrivee, depart, 2, 3, new BigDecimal("500"));

        assertThrows(AgenceNonPartenaireException.class, () -> service.chercherOffres(recherche));
    }

    @Test
    void prixConfirmeEgaleLePrixAnnonce() throws Exception {
        Devis devis = premiereOffre();

        Confirmation confirmation = service.reserver(demande(
                devis.identifiantOffre(), "REQ-1", SECRET, paiementValide()));

        assertEquals(devis.prixTotal(), confirmation.prixConfirme());
        assertFalse(confirmation.rejeu());
    }

    @Test
    void secretFauxNeReserveRien() throws Exception {
        Devis devis = premiereOffre();

        assertThrows(AuthentificationMetierException.class, () -> service.reserver(demande(
                devis.identifiantOffre(), "REQ-SECRET", "mauvais-secret", paiementValide())));

        Confirmation confirmation = service.reserver(demande(
                devis.identifiantOffre(), "REQ-VALIDE", SECRET, paiementValide()));
        assertFalse(confirmation.rejeu());
    }

    @Test
    void paiementInvalideNeReserveRien() throws Exception {
        Devis devis = premiereOffre();
        CoordonneesPaiement paiementInvalide = new CoordonneesPaiement(
                "123", 1, LocalDate.now().getYear() + 1, "123");

        assertThrows(PaiementMetierInvalideException.class, () -> service.reserver(demande(
                devis.identifiantOffre(), "REQ-PAY", SECRET, paiementInvalide)));

        Confirmation confirmation = service.reserver(demande(
                devis.identifiantOffre(), "REQ-APRES-PAY", SECRET, paiementValide()));
        assertFalse(confirmation.rejeu());
    }

    @Test
    void rejeuRetourneLaMemeReservation() throws Exception {
        Devis devis = premiereOffre();
        Confirmation premiere = service.reserver(demande(
                devis.identifiantOffre(), "REQ-REJEU", SECRET, paiementValide()));

        Confirmation rejeu = service.reserver(demande(
                devis.identifiantOffre(), "REQ-REJEU", SECRET, paiementValide()));

        assertEquals(premiere.referenceReservation(), rejeu.referenceReservation());
        assertTrue(rejeu.rejeu());
    }

    @Test
    void nouvelleDemandeSurOffreReserveeEstRefusee() throws Exception {
        Devis devis = premiereOffre();
        service.reserver(demande(devis.identifiantOffre(), "REQ-A", SECRET, paiementValide()));

        assertThrows(OffreMetierIndisponibleException.class, () -> service.reserver(demande(
                devis.identifiantOffre(), "REQ-B", SECRET, paiementValide())));
    }

    private Devis premiereOffre()
            throws AgenceNonPartenaireException, DemandeMetierInvalideException {
        CritereRecherche recherche = new CritereRecherche(
                AGENCE, "Montpellier", arrivee, depart, 2, 3, new BigDecimal("500"));
        return service.chercherOffres(recherche).get(0);
    }

    private DemandeReservationMetier demande(
            String identifiantOffre,
            String identifiantDemande,
            String secret,
            CoordonneesPaiement paiement) {
        return new DemandeReservationMetier(
                AGENCE,
                secret.toCharArray(),
                identifiantOffre,
                new VoyageurPrincipal("Martin", "Alice"),
                paiement,
                identifiantDemande);
    }

    private CoordonneesPaiement paiementValide() {
        return new CoordonneesPaiement(
                "4242424242424242",
                12,
                LocalDate.now().getYear() + 1,
                "123");
    }

    private byte[] empreinte(String secret, byte[] sel) throws Exception {
        PBEKeySpec specification = new PBEKeySpec(secret.toCharArray(), sel, 10_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification)
                    .getEncoded();
        } finally {
            specification.clearPassword();
        }
    }
}
