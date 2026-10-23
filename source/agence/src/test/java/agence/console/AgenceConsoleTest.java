package agence.console;

import agence.metier.*;
import org.junit.jupiter.api.Test;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgenceConsoleTest {
    private String executer(FauxHotel hotel, String saisie) {
        StringWriter sortie = new StringWriter();
        PrintWriter out = new PrintWriter(sortie, true);
        try (var identite = new IdentiteAgence("AG-TEST", "secret-console".toCharArray())) {
            new AgenceConsole(new AgenceService(List.of(hotel), identite),
                    new EntreeConsole(new StringReader(saisie), out, null), out).executer();
        }
        return sortie.toString();
    }

    private FauxHotel hotel() {
        FauxHotel hotel = new FauxHotel("A");
        hotel.offres = List.of(new Offre("A", "OFF-1", "Hotel Test", "Montpellier", 3, "Double", 2,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 12), new BigDecimal("180"), "EUR"));
        return hotel;
    }

    @Test void parcoursInteractifAvecErreursReservationEtRejeu() {
        FauxHotel hotel = hotel();
        String annee = Integer.toString(LocalDate.now().getYear() + 2);
        String carte = "4242424242424242\n12\n" + annee + "\n732\n";
        String saisie = "inconnu\n1\n\nMontpellier\n31/02/2027\n2027-05-10\n2027-05-12\nabc\n2\n3\n500\n"
                + "2\n9\n1\nMartin\nAlice\n" + carte + "3\n" + carte + "0\n";
        String sortie = executer(hotel, saisie);
        assertTrue(sortie.contains("Saisie invalide"));
        assertTrue(sortie.contains("Reservation confirmee"));
        assertTrue(sortie.contains("aucun doublon"));
        assertTrue(sortie.contains("Au revoir"));
        assertFalse(sortie.contains("4242424242424242"));
        assertFalse(java.util.regex.Pattern.compile("(?<![A-Za-z0-9-])732(?![A-Za-z0-9-])").matcher(sortie).find());
        assertFalse(sortie.contains("secret-console"));
        assertEquals(2, hotel.demandes.size()); assertEquals(hotel.demandes.get(0), hotel.demandes.get(1));
    }

    @Test void panneRechercheEstLisibleEtMenuContinue() {
        FauxHotel hotel = hotel(); hotel.panne = true;
        String sortie = executer(hotel, "1\nMontpellier\n2027-05-10\n2027-05-12\n2\n3\n500\n0\n");
        assertTrue(sortie.contains("Incident - Hotel A")); assertTrue(sortie.contains("Au revoir"));
        assertFalse(sortie.contains("Exception"));
    }

    @Test void finDeFluxEtListeVideNeTerminentPasAnormalement() {
        String sortie = executer(hotel(), "2\n3\n4\n");
        assertTrue(sortie.contains("Effectuez d'abord une recherche"));
        assertTrue(sortie.contains("Aucune demande")); assertTrue(sortie.contains("Fin de la saisie"));
        assertTrue(sortie.contains("Au revoir"));
    }

    @Test void refusMetierEstLisibleSansTraceBrute() {
        FauxHotel hotel = hotel(); hotel.refusReservation = new RefusMetier(RefusMetier.Code.OFFRE_INDISPONIBLE, "Offre deja reservee.");
        String sortie = executer(hotel, "1\nMontpellier\n2027-05-10\n2027-05-12\n2\n3\n500\n2\n1\nMartin\nAlice\n4242424242424242\n12\n"
                + (LocalDate.now().getYear() + 2) + "\n732\n0\n");
        assertTrue(sortie.contains("Refus metier [OFFRE_INDISPONIBLE]"));
        assertTrue(sortie.contains("Au revoir")); assertFalse(sortie.contains("Exception"));
    }
}
