package agence.console;

import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;

public final class EntreeConsole {
    private final Reader reader;
    private final PrintWriter out;
    private final Console terminal;

    public EntreeConsole(Reader reader, PrintWriter out, Console terminal) {
        this.reader = reader;
        this.out = out;
        this.terminal = terminal;
    }

    public String lire(String invite) {
        char[] caracteres = lireCaracteres(invite, false);
        try { return new String(caracteres).trim(); }
        finally { Arrays.fill(caracteres, '\0'); }
    }

    public char[] confidentiel(String invite) { return lireCaracteres(invite, true); }

    private char[] lireCaracteres(String invite, boolean confidentiel) {
        if (confidentiel && terminal != null) {
            char[] valeur = terminal.readPassword("%s", invite);
            if (valeur == null) throw new FinSaisie();
            return valeur;
        }
        out.print(invite);
        out.flush();
        char[] tampon = new char[64];
        int taille = 0;
        try {
            int c;
            while ((c = reader.read()) != -1 && c != '\n') {
                if (c == '\r') continue;
                if (taille == 4096) throw new IllegalArgumentException("Saisie trop longue.");
                if (taille == tampon.length) {
                    char[] ancien = tampon;
                    tampon = Arrays.copyOf(tampon, tampon.length * 2);
                    Arrays.fill(ancien, '\0');
                }
                tampon[taille++] = (char) c;
            }
            if (c == -1 && taille == 0) throw new FinSaisie();
            return Arrays.copyOf(tampon, taille);
        } catch (IOException e) {
            throw new FinSaisie();
        } finally { Arrays.fill(tampon, '\0'); }
    }

    static final class FinSaisie extends RuntimeException { }
}
