package agence.metier;

import java.util.Arrays;

/** Le secret reste en memoire le temps de la session, sans affichage ni persistance. */
public final class IdentiteAgence implements AutoCloseable {
    private final String identifiant;
    private final char[] secret;

    public IdentiteAgence(String identifiant, char[] secret) {
        if (identifiant == null || identifiant.isBlank())
            throw new IllegalArgumentException("Identifiant d'agence absent.");
        this.identifiant = identifiant.trim();
        this.secret = secret == null ? new char[0] : secret.clone();
    }

    public String identifiant() { return identifiant; }
    public char[] copierSecret() { return secret.clone(); }
    public boolean possedeSecret() { return secret.length > 0; }
    @Override public void close() { Arrays.fill(secret, '\0'); }
    @Override public String toString() { return "IdentiteAgence[" + identifiant + "]"; }
}
