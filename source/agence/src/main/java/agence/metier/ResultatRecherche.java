package agence.metier;

import java.util.List;

public record ResultatRecherche(List<Offre> offres, List<Incident> incidents) {
    public ResultatRecherche {
        offres = List.copyOf(offres);
        incidents = List.copyOf(incidents);
    }

    public enum Nature { REFUS_METIER, PANNE_TECHNIQUE }
    public record Incident(String hotel, Nature nature, String message) { }
}
