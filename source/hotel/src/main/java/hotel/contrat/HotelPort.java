package hotel.contrat;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

import java.util.List;

@WebService(name = "HotelPort", targetNamespace = HotelPort.NAMESPACE)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public interface HotelPort {
    String NAMESPACE = "http://hotel.hai704i.umontpellier.fr/";

    @WebMethod(operationName = "chercherOffres")
    @WebResult(name = "offre")
    List<Offre> chercherOffres(@WebParam(name = "demande") RechercheOffres demande)
            throws AgenceInconnue, DemandeInvalide;

    @WebMethod(operationName = "reserver")
    @WebResult(name = "confirmation")
    ConfirmationReservation reserver(@WebParam(name = "demande") DemandeReservation demande)
            throws AuthentificationRefusee, PaiementInvalide, OffreIndisponible, DemandeInvalide;
}
