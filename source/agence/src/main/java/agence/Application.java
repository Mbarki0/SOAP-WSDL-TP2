package agence;

import agence.config.ConfigurationAgence;
import agence.console.AgenceConsole;
import agence.console.EntreeConsole;
import agence.metier.AgenceService;
import agence.metier.HotelGateway;
import agence.soap.SoapHotelGateway;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Assemblage de l'application ; la console reste independante de SOAP et de la configuration. */
public final class Application {
    private Application() { }

    public static void main(String[] args) {
        PrintWriter out = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        if (args.length > 1 || (args.length == 1 && "--help".equals(args[0]))) {
            out.println("Usage : java -jar agence-soap-1.0-SNAPSHOT.jar [agence.properties]");
            out.println("Secret fictif : variable AGENCE_SECRET ou propriete agence.secret d'un fichier local non versionne.");
            return;
        }
        try (ConfigurationAgence configuration = ConfigurationAgence.charger(args.length == 0 ? "agence.properties" : args[0])) {
            List<HotelGateway> hotels = configuration.hotels().stream()
                    .<HotelGateway>map(h -> new SoapHotelGateway(h, configuration.connexionMs(), configuration.reponseMs())).toList();
            AgenceService service = new AgenceService(hotels, configuration.identite());
            if (!configuration.identite().possedeSecret())
                out.println("Secret non configure : recherche disponible ; reservation impossible avant configuration.");
            var terminal = System.console();
            var reader = terminal == null ? new InputStreamReader(System.in, StandardCharsets.UTF_8) : terminal.reader();
            new AgenceConsole(service, new EntreeConsole(reader, out, terminal), out).executer();
        } catch (IOException | IllegalArgumentException e) {
            out.println("Configuration invalide : " + e.getMessage());
        }
    }
}
