package hotel.contrat;

import jakarta.xml.ws.WebFault;

@WebFault(name = "PaiementInvalide", targetNamespace = HotelPort.NAMESPACE)
public class PaiementInvalide extends Exception {
    private final FauteHotel faultInfo;

    public PaiementInvalide(String message, FauteHotel faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public PaiementInvalide(String message, FauteHotel faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public FauteHotel getFaultInfo() {
        return faultInfo;
    }
}

