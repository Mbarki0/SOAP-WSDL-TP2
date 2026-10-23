package agence.metier;

public final class RefusMetier extends Exception {
    public enum Code { AGENCE_INCONNUE, AUTHENTIFICATION_REFUSEE, PAIEMENT_INVALIDE, OFFRE_INDISPONIBLE, DEMANDE_INVALIDE }
    private final Code code;

    public RefusMetier(Code code, String message) { super(message); this.code = code; }
    public Code code() { return code; }
}
