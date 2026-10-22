package hotel.contrat;

import jakarta.xml.ws.WebFault;

@WebFault(name = "AgenceInconnue", targetNamespace = HotelPort.NAMESPACE)
public class AgenceInconnue extends Exception {
    private final FauteHotel faultInfo;

    public AgenceInconnue(String message, FauteHotel faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public AgenceInconnue(String message, FauteHotel faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public FauteHotel getFaultInfo() {
        return faultInfo;
    }
}

