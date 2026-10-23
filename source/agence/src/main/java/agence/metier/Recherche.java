package agence.metier;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Recherche(String ville, LocalDate arrivee, LocalDate depart,
                        int personnes, int etoilesMinimum, BigDecimal prixMaximum) {
    public Recherche {
        if (ville == null || ville.isBlank()) throw new IllegalArgumentException("La ville est obligatoire.");
        ville = ville.trim();
        if (arrivee == null || depart == null || !depart.isAfter(arrivee))
            throw new IllegalArgumentException("Le depart doit suivre l'arrivee.");
        if (personnes < 1) throw new IllegalArgumentException("Il faut au moins une personne.");
        if (etoilesMinimum < 0 || etoilesMinimum > 5)
            throw new IllegalArgumentException("Les etoiles doivent etre comprises entre 0 et 5.");
        if (prixMaximum == null || prixMaximum.signum() <= 0)
            throw new IllegalArgumentException("Le budget total doit etre positif.");
    }
}
