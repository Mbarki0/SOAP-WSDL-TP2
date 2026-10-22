package hotel.contrat;

import jakarta.xml.ws.WebFault;

@WebFault(name = "DemandeInvalide", targetNamespace = HotelPort.NAMESPACE)
public class DemandeInvalide extends Exception {
    private final FauteHotel faultInfo;

    public DemandeInvalide(String message, FauteHotel faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public DemandeInvalide(String message, FauteHotel faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public FauteHotel getFaultInfo() {
        return faultInfo;
    }
}

