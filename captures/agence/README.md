# Captures agence - A7

Captures issues de vrais echanges avec le serveur hotel de A, observees le 23 octobre 2026.
Les tests lancent des JVM distinctes sur des ports libres ; les adresses des captures sont celles de cette session, pas les ports de demonstration fixes 8081/8082.

## Provenance et confidentialite

- Recherche : vrai serveur et vrais DTO issus du contrat de A.
- Reservations automatisees : copies temporaires de ses configurations, avec empreintes de secrets fictifs de test. Les sources et fichiers de configuration versionnes de A ne sont pas modifies.
- Les identifiants d'offres, demandes et reservations sont ceux des essais ; ils ne sont plus connus d'un serveur redemarre avec un depot en memoire vide.
- secretPresente, numeroCarte et cvc sont remplaces par [EXPURGE] dans une copie DOM AVANT ecriture du XML. Aucune capture brute contenant ces champs n'est enregistree.
- Les requetes expurgees illustrent la structure, mais ne sont pas directement rejouables. Pour generer le client, utiliser contract/hotel.wsdl et hotel.xsd, pas le WSDL capture dont l'adresse est ephemere.

## Lecture des fichiers

| Prefixe | Observation |
| --- | --- |
| recherche | Requete et reponse de recherche, HTTP 200 ; la derniere recherche capturee survient apres une reservation et peut donc contenir moins d'offres |
| reservation | Reservation acceptee, HTTP 200, reference et prix confirme |
| reservation-rejeu | Meme identifiant de demande, meme reference, rejeu=true |
| faute-authentification | Secret errone, HTTP 500 et detail AuthentificationRefusee |
| faute-agence-inconnue | Recherche par une agence non partenaire, HTTP 500 et detail AgenceInconnue |
| faute-offre-indisponible | Nouvelle demande sur une offre reservee, HTTP 500 et detail OffreIndisponible |
| faute-paiement | Paiement volontairement invalide envoye par un test SOAP bas niveau, HTTP 500 et detail PaiementInvalide |

Les fichiers *-http.txt indiquent le statut observe. hotel.wsdl et hotel.xsd sont recuperes sur le serveur actif.
parcours-console.txt est la sortie controlee du JAR final : saisies invalides, recherche, reservation, rejeu et refus d'une nouvelle reservation sur l'offre indisponible.
console-hotel-arrete.txt montre une autre execution du JAR, avec le Port arrete et les Arenes joignables.
validation.txt resume le parcours d'integration au niveau metier/acces SOAP.

## Points a expliquer

L'URI d'enveloppe http://schemas.xmlsoap.org/soap/envelope/ et text/xml identifient SOAP 1.1.
Les noms chercherOffres et reserver dans Body sont les elements enveloppes des operations.
Les prefixes XML sont arbitraires : leur URI compte. demande, offre et confirmation sont non qualifies ; leurs champs sont qualifies selon le XSD.
La valeur 248.40 EUR de la reservation correspond au devis de trois nuits aux Arenes pour AG-ALPHA : elle est conservee dans prixConfirme.
Le schema fixe xs:decimal pour ce prix, traduit en BigDecimal cote client. La classe engendree est independante de la classe Java du serveur.
Une faute metier possede un Fault et un detail type ; une connexion impossible n'est pas un tel refus et ne produit pas de reponse SOAP de l'hotel.
