# Contrat d'intégration Hôtel - Agence

Version : `contract-v1`

## Identification du service

- Namespace : `http://hotel.hai704i.umontpellier.fr/`
- Service : `HotelService`
- PortType : `HotelPort`
- Port : `HotelSoapPort`
- Binding : SOAP 1.1, document/literal wrapped
- WSDL hors ligne : `contract/hotel.wsdl`
- Schéma : `contract/hotel.xsd`

Le `schemaLocation` du WSDL versionné vaut `hotel.xsd`. Le contrat peut donc être donné à `wsimport` sans serveur actif.

## Opérations

### `chercherOffres`

Entrée `demande` de type `rechercheOffres` :

- `identifiantAgence` ;
- `ville` ;
- `dateArrivee` et `dateDepart` en `xs:date` ;
- `nombrePersonnes` ;
- `nombreEtoilesMinimum` ;
- `prixMaximum`, qui désigne le prix total maximal du séjour.

Résultat : zéro ou plusieurs éléments `offre`. Une offre contient son identifiant, l'hôtel, la ville, les étoiles, la description et la capacité de la chambre, les dates, le prix total et la devise.

Fautes :

- `AgenceInconnue` ;
- `DemandeInvalide`.

### `reserver`

Entrée `demande` de type `demandeReservation` :

- identifiant d'agence et `secretPresente` ;
- identifiant de l'offre ;
- voyageur imbriqué : nom et prénom ;
- paiement fictif imbriqué : numéro, mois, année et CVC ;
- identifiant de demande créé par l'agence avant l'appel.

Résultat `confirmation` : référence de réservation, identifiants de demande et d'offre, hôtel, dates, prix confirmé, devise et indicateur `rejeu`.

Fautes :

- `AuthentificationRefusee` ;
- `PaiementInvalide` ;
- `OffreIndisponible` ;
- `DemandeInvalide`.

## Règles partagées

Une offre est un devis associé à une agence, une chambre, des dates, un nombre de personnes et un prix total. Le prix confirmé est repris du devis : il est identique au prix annoncé.

L'hôtel authentifie l'agence avant toute écriture. Le secret présenté circule dans la demande, tandis que l'hôtel conserve uniquement une empreinte PBKDF2-HMAC-SHA256 salée. Le CVC et le numéro complet ne sont jamais conservés.

L'identifiant de demande est la clé d'idempotence pour une agence :

- même agence, même identifiant et même offre : la confirmation initiale est renvoyée avec `rejeu=true` ;
- même identifiant utilisé pour une autre offre : `DemandeInvalide` ;
- nouvel identifiant visant une chambre désormais réservée : `OffreIndisponible`.

## Instances de démonstration

- Hôtel des Arènes : `http://localhost:8081/hotel`
- Hôtel du Port : `http://localhost:8082/hotel`

Les deux instances utilisent le même programme et deux fichiers de configuration différents. Les identifiants partenaires de démonstration sont `AG-ALPHA` et `AG-BETA`. Leurs secrets fictifs doivent être transmis au membre du binôme hors des captures SOAP.

## Construction et lancement de l'hôtel

Depuis `source/hotel` :

```powershell
mvn clean package
java -jar target/hotel-soap-1.0-SNAPSHOT.jar hotel-arenes.properties
```

Dans un second terminal :

```powershell
java -jar target/hotel-soap-1.0-SNAPSHOT.jar hotel-port.properties
```

Les fichiers de configuration sont inclus dans le JAR. Un chemin vers un fichier `.properties` externe peut aussi être fourni.

## Régénération du client

Le projet agence doit utiliser `com.sun.xml.ws:jaxws-maven-plugin:4.0.5` et prendre `contract/hotel.wsdl` comme source. Les classes obtenues doivent rester sous `target/generated-sources/` et ne doivent pas être copiées dans les sources.

Après génération, il faut vérifier les noms réellement produits par `wsimport` avant d'écrire l'adaptateur SOAP du client.

Sous Windows, Metro peut mal résoudre l'import `hotel.xsd` lorsque le chemin du projet contient un caractère accentué. Si l'avertissement `Failed to read schema document 'hotel.xsd'` apparaît, copier temporairement les deux fichiers du dossier `contract` dans un chemin sans accent pour lancer `wsimport`. Le WSDL lui-même reste portable et conserve l'import relatif `hotel.xsd`.

## Passation au Codex de l'étudiant 2

Contexte à fournir avec le dépôt :

> Travaille uniquement dans `source/agence/**`, `captures/agence/**` et `documentation/agence-notes.md`. Le contrat figé est `contract-v1`. Lis `contract/CONTRAT_INTEGRATION.md`, `contract/hotel.wsdl` et `contract/hotel.xsd` avant toute implémentation. Génère le client avec le plugin Metro 4.0.5 ; ne référence et ne copie aucune classe de `source/hotel`. Vérifie les vrais noms des classes engendrées. Si Metro résout mal `hotel.xsd` à cause des accents du chemin Windows, fais la génération depuis une copie temporaire du dossier `contract` placée dans un chemin sans accent. Les adresses de test sont `http://localhost:8081/hotel` et `http://localhost:8082/hotel` et doivent rester configurables à l'exécution.

