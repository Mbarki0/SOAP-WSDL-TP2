package fr.umontpellier.hai704i.hotel;

import fr.umontpellier.hai704i.hotel.config.ConfigurationHotel;
import fr.umontpellier.hai704i.hotel.depot.InMemoryHotelRepository;
import fr.umontpellier.hai704i.hotel.metier.HotelServiceMetier;
import fr.umontpellier.hai704i.hotel.securite.VerificateurSecret;
import fr.umontpellier.hai704i.hotel.soap.HotelEndpoint;
import jakarta.xml.ws.Endpoint;

import java.util.concurrent.CountDownLatch;

public final class ServeurHotel {
    private ServeurHotel() {
    }

    public static void main(String[] args) throws Exception {
        String emplacement = args.length == 0 ? "hotel-arenes.properties" : args[0];
        ConfigurationHotel configuration = ConfigurationHotel.charger(emplacement);
        InMemoryHotelRepository repository = new InMemoryHotelRepository(
                configuration.chambres(), configuration.agences());
        HotelServiceMetier service = new HotelServiceMetier(
                configuration.identite(), repository, new VerificateurSecret());

        Endpoint endpoint = Endpoint.publish(configuration.adresse(), new HotelEndpoint(service));
        CountDownLatch arret = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            endpoint.stop();
            arret.countDown();
        }));

        System.out.println(configuration.identite().nom() + " publie sur " + configuration.adresse());
        System.out.println("WSDL : " + configuration.adresse() + "?wsdl");
        arret.await();
    }
}
