package agence.soap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Lance le VRAI serveur compile de A dans une JVM separee ; aucune classe serveur importee. */
final class HotelProcess implements AutoCloseable {
    static final Path BASE = Path.of(System.getProperty("basedir", ".")).toAbsolutePath();
    static final String SECRET = "secret-integration-fictif";
    private final Process processus;
    final String adresse;

    HotelProcess(Path temporaire, String configuration, boolean secretsDeTest) throws Exception {
        Path hotel = BASE.resolve("../hotel").normalize();
        Path classes = Path.of(System.getProperty("hotel.classes", hotel.resolve("target/classes").toString()));
        if (!Files.isRegularFile(classes.resolve("hotel/ServeurHotel.class")))
            throw new IllegalStateException("Compilez d'abord source/hotel avec mvn compile.");
        Properties p = new Properties();
        try (var entree = Files.newInputStream(hotel.resolve("src/main/resources/" + configuration))) { p.load(entree); }
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        p.setProperty("hotel.port", Integer.toString(port));
        if (secretsDeTest) {
            byte[] sel = "sel-integration-2026".getBytes(StandardCharsets.UTF_8);
            var specification = new PBEKeySpec(SECRET.toCharArray(), sel, 10000, 256);
            byte[] empreinte;
            try { empreinte = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).getEncoded(); }
            finally { specification.clearPassword(); }
            String preuve = "10000:" + Base64.getEncoder().encodeToString(sel) + ":" + Base64.getEncoder().encodeToString(empreinte);
            String alpha = configuration.contains("arenes") ? "0.90" : "0.85";
            String beta = configuration.contains("arenes") ? "0.95" : "0.92";
            p.setProperty("hotel.agences", "AG-ALPHA:" + preuve + ":" + alpha + ",AG-BETA:" + preuve + ":" + beta);
        }
        Files.createDirectories(temporaire);
        Path fichier = temporaire.resolve("hotel-" + port + ".properties");
        try (var sortie = Files.newOutputStream(fichier)) { p.store(sortie, "Configuration ephemere des tests"); }
        String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        String classpath = classes + File.pathSeparator + System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        processus = new ProcessBuilder(java, "-cp", classpath, "hotel.ServeurHotel", fichier.toString())
                .redirectErrorStream(true).redirectOutput(temporaire.resolve("hotel-" + port + ".log").toFile()).start();
        adresse = "http://localhost:" + port + "/hotel";
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).build();
            long limite = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (System.nanoTime() < limite) {
                if (!processus.isAlive()) throw new IOException("Le serveur hotel s'est arrete ; voir target/hotels/*.log.");
                try {
                    var reponse = client.send(HttpRequest.newBuilder(URI.create(adresse + "?wsdl"))
                            .timeout(Duration.ofMillis(500)).GET().build(), HttpResponse.BodyHandlers.discarding());
                    if (reponse.statusCode() == 200) return;
                } catch (IOException ignored) { }
                Thread.sleep(100);
            }
            throw new IOException("Le serveur hotel n'est pas devenu disponible.");
        } catch (Exception e) { close(); throw e; }
    }

    @Override public void close() {
        processus.destroy();
        try { if (!processus.waitFor(3, TimeUnit.SECONDS)) processus.destroyForcibly().waitFor(3, TimeUnit.SECONDS); }
        catch (InterruptedException e) { processus.destroyForcibly(); Thread.currentThread().interrupt(); }
    }
}
