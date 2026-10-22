# Notes Hôtel

## Périmètre

Travail réalisé uniquement dans la partie Hôtel : contrat SOAP, point d'accès, métier, dépôt mémoire, publication, configurations multi-instance, tests et captures. Aucun code du projet agence n'a été créé.

## Ressources consultées

- énoncé `HAI704I_TP2_SOAP_2026-2027.pdf` ;
- cours `HAI704I_CM03_SOAP_WSDL_2026-2027.pdf`.

Les ressources `date-demo`, `employe-demo` et les deux annexes SOAP propres au TP2 n'étaient pas présentes dans l'inventaire local du 22 octobre 2026. Elles n'ont pas été remplacées par les exemples RMI ou Spring génériques du dossier TP.

## Architecture

```text
message SOAP
    -> HotelEndpoint : DTO, adaptation et traduction des fautes
    -> HotelServiceMetier : validation, tarifs, disponibilité, authentification, idempotence
    -> HotelRepository : contrat d'accès aux données
    -> InMemoryHotelRepository : données propres au processus hôtel
```

Le métier n'importe aucune classe `jakarta.*` et ne manipule aucun DTO SOAP. Le point d'accès est le seul adaptateur entre les objets du contrat et les objets métier.

## Décisions

### Offre

Une offre est un devis et non un simple identifiant de chambre. Elle conserve l'agence, la chambre, les dates, le nombre de personnes et le prix total calculé au moment de la recherche. La réservation reprend ce prix sans le recalculer.

### Tarifs partenaires

Chaque agence possède un coefficient de tarif. Pour trois nuits à l'Hôtel du Port, la chambre à 84 EUR par nuit coûte 214,20 EUR pour `AG-ALPHA` et 231,84 EUR pour `AG-BETA`.

### Authentification

La configuration ne contient pas les secrets en clair. Elle contient le nombre d'itérations, le sel et l'empreinte PBKDF2-HMAC-SHA256. La comparaison utilise `MessageDigest.isEqual`. Le contrôle est effectué avant toute écriture.

### Paiement fictif

Le numéro doit contenir 16 chiffres, le mois être compris entre 1 et 12, l'année/mois ne pas être révolu et le CVC contenir 3 chiffres. Le paiement n'est pas enregistré. Les classes concernées n'ont pas de `toString()` personnalisé.

### Idempotence

La clé est le couple agence/identifiant de demande. Un rejeu de la même offre renvoie la même référence avec `rejeu=true`. Une nouvelle demande visant la chambre réservée reçoit `OffreIndisponible`.

## Vérifications exécutées

- `mvn clean test` : 7 tests, 0 échec, 0 erreur ;
- `mvn clean package` : construction réussie du JAR autonome ;
- `wsimport` Metro 4.0.5 sur le WSDL hors ligne : 20 classes Java engendrées, dont `HotelService`, `HotelPort` et les cinq fautes typées ;
- `GET /hotel?wsdl` et `GET /hotel?xsd=1` : réponses obtenues ;
- WSDL : opérations `chercherOffres` et `reserver`, binding document/literal, fautes déclarées ;
- recherche partenaire : HTTP 200 et deux offres ;
- agence inconnue : HTTP 500, `<S:Fault>` et `<detail>` typé `AgenceInconnue` ;
- réservation : HTTP 200, prix annoncé et confirmé égaux à 248,40 EUR ;
- secret faux : HTTP 500 et faute `AuthentificationRefusee` ;
- rejeu : HTTP 200, même référence et `rejeu=true` ;
- nouvelle demande sur la même offre : HTTP 500 et faute `OffreIndisponible` ;
- deux processus : Hôtel des Arènes sur 8081 et Hôtel du Port sur 8082 ;
- tarifs différents pour deux agences sur la même chambre.

## Lecture des captures

La version employée est SOAP 1.1 : l'enveloppe utilise `http://schemas.xmlsoap.org/soap/envelope/` et le type HTTP est `text/xml`.

Exemple de chemin d'une valeur :

```text
Offre.prixTotal : BigDecimal
    -> hotel.xsd : xs:decimal
    -> recherche-reponse.xml : <ns2:prixTotal>248.40</ns2:prixTotal>
    -> future classe engendrée côté agence : propriété issue du même xs:decimal
```

Le DTO de l'hôtel et la future classe du client ne partageront aucun code Java. Leur point commun sera uniquement la forme définie dans le XSD.

La liste est visible dans le schéma par `maxOccurs="unbounded"`. Les objets `voyageur` et `paiement` apparaissent comme types imbriqués dans la demande de réservation. Les fautes métier apparaissent à la fois dans le WSDL et sous `<detail>` sur le fil.

La requête de réservation archivée est volontairement expurgée. Le secret, le numéro complet et le CVC ont été remplacés ; la réponse réelle ne contient aucune de ces valeurs.

## Pièges de notation / hors-consigne

- Un service qui fonctionne avec `javax.*` reste hors consigne : seuls les imports `jakarta.*` sont utilisés.
- Retourner une liste vide pour une agence inconnue serait fonctionnellement ambigu : une faute typée est nécessaire.
- Utiliser directement les DTO dans le métier violerait la séparation A2.
- Recalculer le prix à la réservation pourrait produire un prix différent de celui annoncé.
- Vérifier l'idempotence après la disponibilité refuserait à tort un rejeu légitime.
- Retourner une confirmation avant d'authentifier le rejeu divulguerait des informations.
- Conserver le secret, le numéro complet ou le CVC, même dans un log ou un `toString()`, est interdit.
- Une nouvelle demande sur une offre réservée n'est pas un rejeu et doit être refusée.
- Les dossiers `target/` et les artefacts engendrés ne doivent pas être versionnés.

## Usage de l'IA

Codex a servi à analyser l'énoncé, discuter le contrat, produire une première implémentation, exécuter les tests et préparer les notes de passation. Le contrat public a été confirmé avant le codage. Les contrôles Maven et les échanges SOAP listés ci-dessus ont été exécutés dans l'environnement local. Le binôme reste responsable de relire, comprendre et expliquer chaque décision et chaque fichier avant la remise.
