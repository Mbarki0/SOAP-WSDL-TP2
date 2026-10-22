package hotel.metier.modele;

import java.math.BigDecimal;

public record AgencePartenaire(
        String identifiant,
        int iterations,
        byte[] sel,
        byte[] empreinteSecret,
        BigDecimal coefficientTarif) {

    public AgencePartenaire {
        sel = sel.clone();
        empreinteSecret = empreinteSecret.clone();
    }

    @Override
    public byte[] sel() {
        return sel.clone();
    }

    @Override
    public byte[] empreinteSecret() {
        return empreinteSecret.clone();
    }
}

