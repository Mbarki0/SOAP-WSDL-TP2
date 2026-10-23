package agence.metier;

import java.time.YearMonth;
import java.util.Arrays;

/** Donnees ephemeres : jamais de record/toString contenant la carte ou le CVC. */
public final class PaiementFictif implements AutoCloseable {
    private final char[] numero;
    private final char[] cvc;
    private final int mois;
    private final int annee;

    public PaiementFictif(char[] numero, int mois, int annee, char[] cvc) {
        if (!chiffres(numero, 16)) throw new IllegalArgumentException("Le numero fictif doit contenir 16 chiffres.");
        if (mois < 1 || mois > 12 || annee < 1 || annee > 9999)
            throw new IllegalArgumentException("La date d'expiration est invalide.");
        if (YearMonth.of(annee, mois).isBefore(YearMonth.now()))
            throw new IllegalArgumentException("La carte fictive est expiree.");
        if (!chiffres(cvc, 3)) throw new IllegalArgumentException("Le CVC fictif doit contenir 3 chiffres.");
        this.numero = numero.clone();
        this.cvc = cvc.clone();
        this.mois = mois;
        this.annee = annee;
    }

    private static boolean chiffres(char[] valeur, int taille) {
        if (valeur == null || valeur.length != taille) return false;
        for (char c : valeur) if (c < '0' || c > '9') return false;
        return true;
    }

    public char[] copierNumero() { return numero.clone(); }
    public char[] copierCvc() { return cvc.clone(); }
    public int mois() { return mois; }
    public int annee() { return annee; }
    @Override public void close() { Arrays.fill(numero, '\0'); Arrays.fill(cvc, '\0'); }
    @Override public String toString() { return "PaiementFictif[donnees masquees]"; }
}
