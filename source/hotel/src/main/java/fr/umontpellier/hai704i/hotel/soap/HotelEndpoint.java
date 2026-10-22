package fr.umontpellier.hai704i.hotel.soap;

import fr.umontpellier.hai704i.hotel.contrat.AgenceInconnue;
import fr.umontpellier.hai704i.hotel.contrat.AuthentificationRefusee;
import fr.umontpellier.hai704i.hotel.contrat.ConfirmationReservation;
import fr.umontpellier.hai704i.hotel.contrat.DemandeInvalide;
import fr.umontpellier.hai704i.hotel.contrat.DemandeReservation;
import fr.umontpellier.hai704i.hotel.contrat.FauteHotel;
import fr.umontpellier.hai704i.hotel.contrat.HotelPort;
import fr.umontpellier.hai704i.hotel.contrat.Offre;
import fr.umontpellier.hai704i.hotel.contrat.OffreIndisponible;
import fr.umontpellier.hai704i.hotel.contrat.PaiementInvalide;
import fr.umontpellier.hai704i.hotel.contrat.RechercheOffres;
import fr.umontpellier.hai704i.hotel.metier.HotelServiceMetier;
import fr.umontpellier.hai704i.hotel.metier.exception.AgenceNonPartenaireException;
import fr.umontpellier.hai704i.hotel.metier.exception.AuthentificationMetierException;
import fr.umontpellier.hai704i.hotel.metier.exception.DemandeMetierInvalideException;
import fr.umontpellier.hai704i.hotel.metier.exception.OffreMetierIndisponibleException;
import fr.umontpellier.hai704i.hotel.metier.exception.PaiementMetierInvalideException;
import fr.umontpellier.hai704i.hotel.metier.modele.Confirmation;
import fr.umontpellier.hai704i.hotel.metier.modele.CoordonneesPaiement;
import fr.umontpellier.hai704i.hotel.metier.modele.CritereRecherche;
import fr.umontpellier.hai704i.hotel.metier.modele.DemandeReservationMetier;
import fr.umontpellier.hai704i.hotel.metier.modele.Devis;
import fr.umontpellier.hai704i.hotel.metier.modele.VoyageurPrincipal;
import jakarta.jws.WebService;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.List;

@WebService(
        endpointInterface = "fr.umontpellier.hai704i.hotel.contrat.HotelPort",
        serviceName = "HotelService",
        portName = "HotelSoapPort",
        targetNamespace = HotelPort.NAMESPACE)
public class HotelEndpoint implements HotelPort {
    private final HotelServiceMetier service;
    private final DatatypeFactory typesXml;

    public HotelEndpoint(HotelServiceMetier service) {
        this.service = service;
        try {
            this.typesXml = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Initialisation XML impossible", e);
        }
    }

    @Override
    public List<Offre> chercherOffres(RechercheOffres demande)
            throws AgenceInconnue, DemandeInvalide {
        try {
            CritereRecherche critere = new CritereRecherche(
                    demande == null ? null : demande.getIdentifiantAgence(),
                    demande == null ? null : demande.getVille(),
                    demande == null ? null : versDate(demande.getDateArrivee()),
                    demande == null ? null : versDate(demande.getDateDepart()),
                    demande == null ? 0 : demande.getNombrePersonnes(),
                    demande == null ? 0 : demande.getNombreEtoilesMinimum(),
                    demande == null ? null : demande.getPrixMaximum());
            return service.chercherOffres(critere).stream().map(this::versContrat).toList();
        } catch (AgenceNonPartenaireException e) {
            throw new AgenceInconnue(e.getMessage(), faute("AGENCE_INCONNUE", e));
        } catch (DemandeMetierInvalideException e) {
            throw new DemandeInvalide(e.getMessage(), faute("DEMANDE_INVALIDE", e));
        }
    }

    @Override
    public ConfirmationReservation reserver(DemandeReservation demande)
            throws AuthentificationRefusee, PaiementInvalide, OffreIndisponible, DemandeInvalide {
        try {
            DemandeReservationMetier demandeMetier = new DemandeReservationMetier(
                    demande == null ? null : demande.getIdentifiantAgence(),
                    demande == null || demande.getSecretPresente() == null
                            ? null : demande.getSecretPresente().toCharArray(),
                    demande == null ? null : demande.getIdentifiantOffre(),
                    demande == null || demande.getVoyageur() == null ? null : new VoyageurPrincipal(
                            demande.getVoyageur().getNom(), demande.getVoyageur().getPrenom()),
                    demande == null || demande.getPaiement() == null ? null : new CoordonneesPaiement(
                            demande.getPaiement().getNumeroCarte(),
                            demande.getPaiement().getMoisExpiration(),
                            demande.getPaiement().getAnneeExpiration(),
                            demande.getPaiement().getCvc()),
                    demande == null ? null : demande.getIdentifiantDemande());
            return versContrat(service.reserver(demandeMetier));
        } catch (AuthentificationMetierException e) {
            throw new AuthentificationRefusee(e.getMessage(), faute("AUTHENTIFICATION_REFUSEE", e));
        } catch (PaiementMetierInvalideException e) {
            throw new PaiementInvalide(e.getMessage(), faute("PAIEMENT_INVALIDE", e));
        } catch (OffreMetierIndisponibleException e) {
            throw new OffreIndisponible(e.getMessage(), faute("OFFRE_INDISPONIBLE", e));
        } catch (DemandeMetierInvalideException e) {
            throw new DemandeInvalide(e.getMessage(), faute("DEMANDE_INVALIDE", e));
        }
    }

    private FauteHotel faute(String code, Exception cause) {
        return new FauteHotel(code, cause.getMessage());
    }

    private LocalDate versDate(XMLGregorianCalendar date) {
        return date == null ? null : LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    private XMLGregorianCalendar versXml(LocalDate date) {
        return typesXml.newXMLGregorianCalendar(date.toString());
    }

    private Offre versContrat(Devis devis) {
        Offre offre = new Offre();
        offre.setIdentifiantOffre(devis.identifiantOffre());
        offre.setNomHotel(devis.nomHotel());
        offre.setVille(devis.ville());
        offre.setNombreEtoiles(devis.nombreEtoiles());
        offre.setDescriptionChambre(devis.descriptionChambre());
        offre.setCapacite(devis.capacite());
        offre.setDateArrivee(versXml(devis.dateArrivee()));
        offre.setDateDepart(versXml(devis.dateDepart()));
        offre.setPrixTotal(devis.prixTotal());
        offre.setDevise(devis.devise());
        return offre;
    }

    private ConfirmationReservation versContrat(Confirmation confirmation) {
        ConfirmationReservation resultat = new ConfirmationReservation();
        resultat.setReferenceReservation(confirmation.referenceReservation());
        resultat.setIdentifiantDemande(confirmation.identifiantDemande());
        resultat.setIdentifiantOffre(confirmation.identifiantOffre());
        resultat.setNomHotel(confirmation.nomHotel());
        resultat.setDateArrivee(versXml(confirmation.dateArrivee()));
        resultat.setDateDepart(versXml(confirmation.dateDepart()));
        resultat.setPrixConfirme(confirmation.prixConfirme());
        resultat.setDevise(confirmation.devise());
        resultat.setRejeu(confirmation.rejeu());
        return resultat;
    }
}

