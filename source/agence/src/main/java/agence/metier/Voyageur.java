package agence.metier;

public record Voyageur(String nom, String prenom) {
    public Voyageur {
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank())
            throw new IllegalArgumentException("Le nom et le prenom sont obligatoires.");
        nom = nom.trim();
        prenom = prenom.trim();
    }
}
