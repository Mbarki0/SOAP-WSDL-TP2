package agence.soap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Verification manuelle reproductible du JAR final, apres mvn package. */
public final class VerificationJar {
    private VerificationJar() { }

    public static void main(String[] args) throws Exception {
        Path base = HotelProcess.BASE;
        Path jar = base.resolve("target/agence-soap-1.0-SNAPSHOT.jar");
        if (!Files.isRegularFile(jar)) throw new IllegalStateException("Executez mvn package avant cette verification.");
        Path dossier = base.resolve("target/verification-jar");
        try (HotelProcess arenes = new HotelProcess(dossier, "hotel-arenes.properties", true);
             HotelProcess port = new HotelProcess(dossier, "hotel-port.properties", true)) {
            Path configuration = dossier.resolve("agence.properties");
            Files.writeString(configuration, "agence.identifiant=AG-ALPHA\nhotels=arenes,port\n"
                    + "hotel.arenes.nom=Hotel des Arenes\nhotel.arenes.url=" + arenes.adresse + "\n"
                    + "hotel.port.nom=Hotel du Port\nhotel.port.url=" + port.adresse + "\n"
                    + "delai.connexion.ms=500\ndelai.reponse.ms=1000\n");
            LocalDate arrivee = LocalDate.now().plusDays(45);
            String recherche = "1\nMontpellier\n" + arrivee + "\n" + arrivee.plusDays(3) + "\n2\n3\n1000\n";
            String paiement = "4242424242424242\n12\n" + (LocalDate.now().getYear() + 2) + "\n732\n";
            String rechercheAvecErreurs = "1\n\nMontpellier\ndate-invalide\n" + arrivee + "\n"
                    + arrivee.plusDays(3) + "\npas-un-nombre\n2\n3\n1000\n";
            String saisie = rechercheAvecErreurs + "2\n99\n1\nMartin\nAlice\n" + paiement
                    + "3\n" + paiement + "2\n1\nMartin\nAlice\n" + paiement + "0\n";
            String sortie = lancer(jar, configuration, saisie);
            verifier(sortie.contains("Reservation confirmee") && sortie.contains("aucun doublon")
                    && sortie.contains("OFFRE_INDISPONIBLE") && sortie.contains("Saisie invalide") && sortie.contains("Au revoir"));
            Files.createDirectories(base.resolve("target/captures"));
            Files.writeString(base.resolve("target/captures/parcours-console.txt"), sortie);
            port.close();
            String partiel = lancer(jar, configuration, recherche + "0\n");
            verifier(partiel.contains("Incident - Hotel du Port") && partiel.contains("Hotel des Arenes") && partiel.contains("Au revoir"));
            Files.writeString(base.resolve("target/captures/console-hotel-arrete.txt"), partiel);
        }
        System.out.println("JAR verifie : deux hotels, CLI, saisies invalides, reservation, rejeu, refus et panne partielle.");
    }

    private static String lancer(Path jar, Path configuration, String saisie) throws Exception {
        String executableJava = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        ProcessBuilder builder = new ProcessBuilder(executableJava, "-jar", jar.toString(), configuration.toString()).redirectErrorStream(true);
        builder.environment().put("AGENCE_SECRET", HotelProcess.SECRET);
        Process process = builder.start();
        var lecteur = Executors.newSingleThreadExecutor();
        try {
            var resultat = lecteur.submit(() -> process.getInputStream().readAllBytes());
            try (var entree = process.getOutputStream()) { entree.write(saisie.getBytes(StandardCharsets.UTF_8)); }
            if (!process.waitFor(25, TimeUnit.SECONDS)) throw new IllegalStateException("Delai depasse pour la CLI.");
            String sortie = new String(resultat.get(5, TimeUnit.SECONDS), StandardCharsets.UTF_8);
            verifier(process.exitValue() == 0);
            // Aucune sortie n'est ecrite sur disque avant ce controle.
            boolean cvcAffiche = java.util.regex.Pattern.compile("(?<![A-Za-z0-9-])732(?![A-Za-z0-9-])").matcher(sortie).find();
            verifier(!sortie.contains("4242424242424242") && !cvcAffiche && !sortie.contains(HotelProcess.SECRET));
            return sortie;
        } finally { process.destroyForcibly(); lecteur.shutdownNow(); }
    }

    private static void verifier(boolean condition) {
        if (!condition) throw new IllegalStateException("Echec de la verification du JAR ; aucune donnee confidentielle n'est affichee.");
    }
}
