package hotel.securite;

import hotel.metier.modele.AgencePartenaire;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public class VerificateurSecret {
    private static final int TAILLE_CLE_BITS = 256;

    public boolean verifier(char[] secretPresente, AgencePartenaire agence) {
        if (secretPresente == null) {
            return false;
        }

        PBEKeySpec specification = new PBEKeySpec(
                secretPresente,
                agence.sel(),
                agence.iterations(),
                TAILLE_CLE_BITS);
        try {
            SecretKeyFactory fabrique = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] empreintePresentee = fabrique.generateSecret(specification).getEncoded();
            return MessageDigest.isEqual(agence.empreinteSecret(), empreintePresentee);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Impossible de verifier le secret", e);
        } finally {
            specification.clearPassword();
        }
    }
}

