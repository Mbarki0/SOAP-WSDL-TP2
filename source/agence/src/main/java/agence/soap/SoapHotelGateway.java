package agence.soap;

import agence.config.ConfigurationAgence;
import agence.metier.*;
import agence.soap.genere.*;
import com.sun.xml.ws.developer.JAXWSProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Seul adaptateur connaissant les classes issues de wsimport. */
public final class SoapHotelGateway implements HotelGateway {
    private final ConfigurationAgence.Hotel hotel;
    private final int connexionMs;
    private final int reponseMs;
    private final Consumer<BindingProvider> personnaliser;

    public SoapHotelGateway(ConfigurationAgence.Hotel hotel, int connexionMs, int reponseMs) {
        this(hotel, connexionMs, reponseMs, provider -> { });
    }

    SoapHotelGateway(ConfigurationAgence.Hotel hotel, int connexionMs, int reponseMs,
                     Consumer<BindingProvider> personnaliser) {
        if (connexionMs < 1 || reponseMs < 1) throw new IllegalArgumentException("Delai invalide.");
        this.hotel = hotel;
        this.connexionMs = connexionMs;
        this.reponseMs = reponseMs;
        this.personnaliser = personnaliser;
    }

    @Override public String cle() { return hotel.cle(); }
    @Override public String nom() { return hotel.nom(); }

    private HotelPort ouvrir() throws IncidentTechnique {
        try {
            URI uri = URI.create(hotel.adresse());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null)
                throw new IllegalArgumentException();
            HotelPort port = new HotelService().getHotelSoapPort();
            BindingProvider provider = (BindingProvider) port;
            var contexte = provider.getRequestContext();
            contexte.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, hotel.adresse());
            contexte.put(JAXWSProperties.CONNECT_TIMEOUT, connexionMs);
            contexte.put(JAXWSProperties.REQUEST_TIMEOUT, reponseMs);
            personnaliser.accept(provider);
            return port;
        } catch (IllegalArgumentException e) {
            throw new IncidentTechnique("Adresse de service invalide ; verifiez la configuration de cet hotel.");
        } catch (WebServiceException e) {
            throw new IncidentTechnique("Initialisation du client impossible ; verifiez le contrat et la configuration.");
        }
    }

    @Override public List<agence.metier.Offre> chercher(Recherche recherche, IdentiteAgence agence)
            throws RefusMetier, IncidentTechnique {
        HotelPort port = ouvrir();
        try {
            RechercheOffres demande = new RechercheOffres();
            demande.setIdentifiantAgence(agence.identifiant());
            demande.setVille(recherche.ville());
            demande.setDateArrivee(xml(recherche.arrivee()));
            demande.setDateDepart(xml(recherche.depart()));
            demande.setNombrePersonnes(recherche.personnes());
            demande.setNombreEtoilesMinimum(recherche.etoilesMinimum());
            demande.setPrixMaximum(recherche.prixMaximum());
            return port.chercherOffres(demande).stream().map(this::offre).toList();
        } catch (AgenceInconnue e) {
            throw new RefusMetier(RefusMetier.Code.AGENCE_INCONNUE, "Cette agence n'est pas partenaire de l'hotel.");
        } catch (DemandeInvalide e) {
            throw new RefusMetier(RefusMetier.Code.DEMANDE_INVALIDE, "L'hotel refuse les criteres de recherche.");
        } catch (WebServiceException e) {
            throw panne();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IncidentTechnique("Reponse de recherche non conforme au contrat.");
        } finally { fermer(port); }
    }

    @Override public Reservation.Confirmation reserver(Reservation reservation, IdentiteAgence agence,
                                                        PaiementFictif paiement)
            throws RefusMetier, IncidentTechnique {
        HotelPort port = ouvrir();
        DemandeReservation demande = new DemandeReservation();
        Paiement donneesPaiement = new Paiement();
        char[] secret = agence.copierSecret();
        char[] numero = paiement.copierNumero();
        char[] cvc = paiement.copierCvc();
        try {
            demande.setIdentifiantAgence(agence.identifiant());
            demande.setSecretPresente(new String(secret));
            demande.setIdentifiantOffre(reservation.offre().identifiant());
            demande.setIdentifiantDemande(reservation.identifiantDemande());
            agence.soap.genere.Voyageur voyageur = new agence.soap.genere.Voyageur();
            voyageur.setNom(reservation.voyageur().nom());
            voyageur.setPrenom(reservation.voyageur().prenom());
            demande.setVoyageur(voyageur);
            donneesPaiement.setNumeroCarte(new String(numero));
            donneesPaiement.setMoisExpiration(paiement.mois());
            donneesPaiement.setAnneeExpiration(paiement.annee());
            donneesPaiement.setCvc(new String(cvc));
            demande.setPaiement(donneesPaiement);
            ConfirmationReservation c = port.reserver(demande);
            return new Reservation.Confirmation(c.getReferenceReservation(), c.getIdentifiantDemande(),
                    c.getIdentifiantOffre(), c.getNomHotel(), date(c.getDateArrivee()), date(c.getDateDepart()),
                    c.getPrixConfirme(), c.getDevise(), c.isRejeu());
        } catch (AuthentificationRefusee e) {
            throw new RefusMetier(RefusMetier.Code.AUTHENTIFICATION_REFUSEE, "L'hotel refuse l'identite ou le secret de l'agence.");
        } catch (PaiementInvalide e) {
            throw new RefusMetier(RefusMetier.Code.PAIEMENT_INVALIDE, "L'hotel refuse les coordonnees de paiement fictives.");
        } catch (OffreIndisponible e) {
            throw new RefusMetier(RefusMetier.Code.OFFRE_INDISPONIBLE, "L'offre n'est plus disponible. Effectuez une nouvelle recherche.");
        } catch (DemandeInvalide e) {
            throw new RefusMetier(RefusMetier.Code.DEMANDE_INVALIDE, "L'hotel refuse la demande de reservation.");
        } catch (WebServiceException e) {
            throw panne();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IncidentTechnique("Confirmation de reservation non conforme au contrat.");
        } finally {
            Arrays.fill(secret, '\0'); Arrays.fill(numero, '\0'); Arrays.fill(cvc, '\0');
            demande.setSecretPresente(null);
            donneesPaiement.setNumeroCarte(null); donneesPaiement.setCvc(null);
            demande.setPaiement(null);
            fermer(port);
        }
    }

    private agence.metier.Offre offre(agence.soap.genere.Offre o) {
        return new agence.metier.Offre(cle(), o.getIdentifiantOffre(), o.getNomHotel(), o.getVille(),
                o.getNombreEtoiles(), o.getDescriptionChambre(), o.getCapacite(), date(o.getDateArrivee()),
                date(o.getDateDepart()), o.getPrixTotal(), o.getDevise());
    }

    private static XMLGregorianCalendar xml(LocalDate date) {
        try { return DatatypeFactory.newInstance().newXMLGregorianCalendar(date.toString()); }
        catch (DatatypeConfigurationException e) { throw new IllegalStateException("Initialisation XML impossible.", e); }
    }

    private static LocalDate date(XMLGregorianCalendar date) {
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    private static IncidentTechnique panne() {
        return new IncidentTechnique("Hotel injoignable, delai depasse ou reponse technique invalide.");
    }

    private static void fermer(HotelPort port) {
        if (port instanceof com.sun.xml.ws.Closeable closeable) {
            // Une erreur de fermeture ne doit pas masquer une confirmation deja recue.
            try { closeable.close(); } catch (WebServiceException ignored) { }
        }
    }
}
