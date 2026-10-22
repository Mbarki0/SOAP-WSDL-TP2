package hotel.contrat;

import jakarta.xml.ws.WebFault;

@WebFault(name = "AuthentificationRefusee", targetNamespace = HotelPort.NAMESPACE)
public class AuthentificationRefusee extends Exception {
    private final FauteHotel faultInfo;

    public AuthentificationRefusee(String message, FauteHotel faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public AuthentificationRefusee(String message, FauteHotel faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public FauteHotel getFaultInfo() {
        return faultInfo;
    }
}

