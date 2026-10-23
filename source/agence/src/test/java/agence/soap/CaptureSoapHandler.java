package agence.soap;

import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Capture reservee aux tests ; expurgation de la copie DOM AVANT toute ecriture. */
final class CaptureSoapHandler implements SOAPHandler<SOAPMessageContext> {
    private final Path dossier;
    private final String prefixe;

    CaptureSoapHandler(Path dossier, String prefixe) { this.dossier = dossier; this.prefixe = prefixe; }
    @Override public Set<QName> getHeaders() { return Set.of(); }
    @Override public boolean handleMessage(SOAPMessageContext context) { enregistrer(context); return true; }
    @Override public boolean handleFault(SOAPMessageContext context) { enregistrer(context); return true; }
    @Override public void close(MessageContext context) { }

    private void enregistrer(SOAPMessageContext contexte) {
        try {
            SOAPMessage message = contexte.getMessage();
            Document copie = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            copie.appendChild(copie.importNode(message.getSOAPPart().getDocumentElement(), true));
            var elements = copie.getElementsByTagName("*");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                if (Set.of("secretPresente", "numeroCarte", "cvc").contains(element.getLocalName()))
                    element.setTextContent("[EXPURGE]");
            }
            boolean sortant = Boolean.TRUE.equals(contexte.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY));
            String sens = sortant ? "requete" : "reponse";
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter texte = new StringWriter();
            transformer.transform(new DOMSource(copie), new StreamResult(texte));
            Files.createDirectories(dossier);
            Files.writeString(dossier.resolve(prefixe + "-" + sens + ".xml"), texte.toString(), StandardCharsets.UTF_8);
            if (!sortant) {
                Object statut = contexte.get(MessageContext.HTTP_RESPONSE_CODE);
                Files.writeString(dossier.resolve(prefixe + "-http.txt"),
                        "HTTP " + statut + "\nContent-Type: text/xml\nSOAP 1.1\n", StandardCharsets.UTF_8);
            }
        } catch (Exception e) { throw new IllegalStateException("Impossible d'ecrire la capture expurgee.", e); }
    }
}
