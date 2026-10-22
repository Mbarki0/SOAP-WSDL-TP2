package fr.umontpellier.hai704i.hotel.contrat;

import jakarta.xml.ws.WebFault;

@WebFault(name = "OffreIndisponible", targetNamespace = HotelPort.NAMESPACE)
public class OffreIndisponible extends Exception {
    private final FauteHotel faultInfo;

    public OffreIndisponible(String message, FauteHotel faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public OffreIndisponible(String message, FauteHotel faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public FauteHotel getFaultInfo() {
        return faultInfo;
    }
}

