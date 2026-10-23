# Notes agence - role B

## Perimetre et sources

Application cliente de la partie A uniquement. Hotel, contrat et documentation de A inchanges.
Sources : enonce HAI704I_TP2_SOAP_2026-2027.pdf (16 pages), cours CM03 SOAP/WSDL (XML, SOAP, WSDL, execution, code-first/contract-first et pile Java), contract/CONTRAT_INTEGRATION.md, hotel.wsdl et hotel.xsd.
Contrat documentaire contract-v1, recu sur le commit 63a2f0c. Aucun tag local contract-v1 constate ; ce nom n'est pas presente comme un tag effectivement cree.
date-demo, employe-demo et annexes SOAP absents des supports locaux. A0 et les livrables humains communs ne sont pas artificiellement declares termines.

## Checklist des exigences

### A4 - Client engendre et independant

- [x] Projet Maven source/agence autonome, Java release 17, Metro 4.0.5, imports Jakarta pour SOAP.
- [x] wsimport depuis le WSDL/XSD local, 20 fichiers sous target/generated-sources/wsimport.
- [x] Aucun import, copie de classe ou dependance Maven vers hotel.
- [x] WSDL/XSD embarques comme ressources, aucune recuperation distante au demarrage.
- [x] Adresses configurables par fichier externe, surcharge ENDPOINT_ADDRESS_PROPERTY.
- [x] mvn clean puis regeneration et compilation effectivement executes.

### A5 - Reservation cote agence

- [x] Construction avec les vrais types engendres : identite agence, secret presente, offre, voyageur, paiement imbrique et identifiant de demande.
- [x] UUID cree avant l'appel ; le choix 3 reutilise le meme objet Reservation et donc le meme identifiant.
- [x] Nouvelle action de reservation : nouvel UUID. Aucun reessai automatique aveugle.
- [x] Prix confirme compare au prix annonce ; identifiants, dates et devise verifies.
- [x] Cinq refus types distincts de l'incident technique.
- [x] Secret externalise, aucune donnee reelle de paiement, aucun affichage/persistance du numero complet ou CVC.
- [x] Paiement demande de nouveau a chaque envoi puis tableaux effaces et DTO nettoyes.
- [x] Tests : succes, secret faux sans effet, paiement refuse, offre indisponible, rejeu et panne.

### A6 - Composition et defaillance partielle

- [x] Deux hotels depuis la configuration, agregation et tri par devise/prix total/nom.
- [x] Connexion et lecture bornees par des entiers positifs en millisecondes.
- [x] Un hotel arrete laisse les offres des autres disponibles et produit un incident nomme.
- [x] Hotel lent et adresse erronee traites comme problemes techniques.
- [x] Tarifs AG-ALPHA / AG-BETA verifies sur le vrai serveur : trois nuits au Port, 214.20 / 231.84 EUR.

### A7 - Contrat, message et code

- [x] Captures reelles du WSDL/XSD, recherche, reservation, rejeu et fautes.
- [x] Expurgation DOM avant toute ecriture de secretPresente, numeroCarte et cvc.
- [x] SOAP 1.1 reconnu par l'URI http://schemas.xmlsoap.org/soap/envelope/ et text/xml.
- [x] Elements enveloppes : chercherOffres, chercherOffresResponse, reserver et reserverResponse, dans le namespace du contrat.
- [x] Trace de prixTotal decrite ci-dessous ; DTO serveur et DTO client distincts.

### A8 - Interface utilisable

- [x] Menu rechercher, revoir/comparer, choisir, reserver, afficher reference et quitter.
- [x] Console sans URL, proxy, classe engendree ni appel SOAP.
- [x] Champs vides, dates mal formees, nombres invalides, choix hors liste : message et poursuite.
- [x] Refus metier, delai, panne et adresse erronee sans trace d'exception brute dans le flux normal.
- [x] Fermeture de chaque proxy apres l'appel ; pas de threads de fond crees par l'agence ; identite nettoyee en sortie normale.
- [x] Verification du vrai JAR dans un processus client distinct de deux processus hotel.

## Architecture et choix

Console -> AgenceService -> HotelGateway <- SoapHotelGateway -> classes agence.soap.genere -> reseau.
Les modeles agence.metier sont propres a l'agence. Seul SoapHotelGateway importe agence.soap.genere.
Application est le point d'assemblage ; la configuration des adresses ne traverse pas la frontiere de la console.
Le stockage de l'origine hotelCle avec chaque offre permet de reserver aupres du bon hotel apres agregation.

Une offre reste le devis choisi par A : l'agence ne recalcule pas son prix. prixMaximum est un budget TOTAL de sejour.
Les appels sont sequentiels, avec delais explicites : choix simple, sans fuite d'executors, adapte a deux hotels. Le temps cumule augmente avec le nombre d'hotels ; une parallelisation bornee serait une amelioration possible.
Les differentes devises sont regroupees avant le tri par prix ; aucune conversion monetaire n'est inventee.

Un refus signifie que l'hotel a repondu avec une faute du contrat. Une panne signifie que le transport ou la reponse technique ne permet pas de conclure.
Lors d'une reservation sans reponse, le statut peut etre inconnu. Le choix 3 reutilise la cle deja creee, jamais une nouvelle cle implicite.
Le paiement n'est pas conserve pour permettre ce reessai : il est ressaisi. Seuls les metadonnees de la demande, l'offre et le voyageur subsistent dans la console.

## Correspondance constatee apres generation

- HotelService / getHotelSoapPort() : service et proxy.
- HotelPort.chercherOffres(RechercheOffres) : List<agence.soap.genere.Offre>.
- HotelPort.reserver(DemandeReservation) : ConfirmationReservation.
- Recherche : AgenceInconnue et DemandeInvalide.
- Reservation : AuthentificationRefusee, PaiementInvalide, OffreIndisponible et DemandeInvalide.
- xs:decimal -> BigDecimal ; xs:date -> XMLGregorianCalendar ; xs:boolean -> boolean.

Contrairement a certains exemples d'autres versions, les exceptions engendrees ici ne portent pas le suffixe _Exception. Les vrais fichiers ont ete inspectes avant l'adaptateur.

## Suivre une valeur et lire les captures

Chemin de prixTotal : hotel.contrat.Offre (BigDecimal, cote A) -> hotel.xsd, type offre, element prixTotal en xs:decimal -> element ns2:prixTotal de la reponse SOAP -> agence.soap.genere.Offre.getPrixTotal() (BigDecimal) -> SoapHotelGateway.offre -> agence.metier.Offre.prixTotal -> affichage de la console.
La reservation lit ensuite prixConfirme et le compare avec BigDecimal.compareTo, sans imposer une echelle decimale identique.
Dans la demonstration capturee, le prix annonce/confirme aux Arenes pour trois nuits est 248.40 EUR.

Les prefixes S, SOAP-ENV et ns2 sont des alias ; leur URI determine l'identite XML.
Dans les enveloppes d'operation, demande, offre et confirmation sont non qualifies, conformement a form="unqualified" dans le XSD ; leurs champs metier sont qualifies.
Les DTO cote hotel et agence partagent une forme XSD, pas du code ni l'identite d'un objet Java. Modifier l'offre locale ne change pas le serveur.
maxOccurs="unbounded" porte la liste d'offres ; les fault de WSDL deviennent detail dans Fault et exceptions Java typees ; la demande contient deux types imbriques voyageur et paiement.
Le header vide visible dans certaines captures est ajoute par la pile SOAP ; la charge metier demeure dans Body.

## Validations observees le 23 octobre 2026

- JDK 23, Maven 3.9.14, release de compilation 17.
- Generation reussie sans serveur actif depuis contract/hotel.wsdl et hotel.xsd.
- 21 tests independants (configuration, metier et console) ; 4 tests d'integration : total 25, aucun echec ni erreur.
- Deux vrais processus hotel distincts : quatre offres, tri, tarifs partenaires, refus agence inconnue.
- Reservation : secret errone refuse avant succes, prix confirme identique, rejeu meme reference, nouvelle demande refusee sur offre indisponible.
- Paiement invalide envoye volontairement par le test SOAP bas niveau : faute typee du vrai hotel, capture expurgee.
- Lecture bornee : serveur HTTP de test lent et delai de 100 ms ; erreur technique obtenue sous la borne de 3 s.
- Configuration originale de A : recherche reelle sur les deux hotels ; seule l'adresse/port ephemere du processus de test est adaptee.
- Reservation automatisee : vrais serveurs de A avec copies temporaires des configurations et empreintes de secrets fictifs de test. Aucune preuve n'est revendiquee pour un secret d'origine non transmis.
- mvn clean package avec integration.hotel=true : target efface, client regenere, tests reussis, JAR autonome produit.
- VerificationJar : JAR final reel, un processus agence + deux processus hotel ; saisies invalides, succes, rejeu, indisponibilite et panne partielle verifies. Transcriptions controlees avant ecriture.
- Contrat et sources de A non modifies ; target et classes engendrees ignores par Git.

Les commandes de verification ont utilise -Dmaven.repo.local=C:\Users\MTechno\Desktop\tp2\.verification\m2 pour garder le cache de travail hors du depot ; cette option n'est pas necessaire chez le correcteur.
Le premier essai de connexions locales dans le bac a sable a ete refuse par l'environnement Windows. Les tests ont ensuite ete executes avec l'autorisation de connexion locale ; cet echec d'environnement n'est pas un echec fonctionnel masque.

## Pieges de notation / hors-consigne

- Ne pas ajouter hotel comme dependance Maven pour eviter wsimport.
- Ne pas versionner target, le JAR ou les 20 fichiers engendres.
- javax.xml.datatype et javax.xml.namespace restent des API Java SE valides ; aucun javax.jws ou javax.xml.ws n'est utilise.
- Ne pas confondre liste vide, refus et panne ; ne pas affirmer qu'une reservation a echoue sur simple timeout.
- Ne pas creer un nouvel UUID pour le reessai du meme acte utilisateur.
- Ne pas conserver le paiement pour faciliter le rejeu ; ne pas activer de dump HTTP brut.
- Ne pas presenter VerificationJar, qui est un test, comme le remplacement de la CLI interactive.
- Aucun bonus ne commence tant que la partie A et la validation commune ne sont pas terminees.

## Limites et suite commune

Recuperer les secrets fictifs d'origine de A pour la demonstration avec les configurations versionnees, puis figer le commit du contrat explicitement avec le binome.
Le rapport d'ingenierie commun, les informations administratives (numeros etudiants/parcours) et la video restent a realiser a deux ; les notes et README fournissent une base factuelle.
A0 ne peut pas etre annonce entierement valide sans les ressources de demonstration absentes. Aucun bonus B/C/D n'a ete engage.
Le stockage hotel en memoire est celui de la partie A : apres redemarrage, une offre ou reservation precedente n'est plus connue.
Les String de JAXB ne peuvent pas etre ecrasees en place ; les references DTO sont liberees, les tableaux nettoyes, mais aucun effacement absolu de la JVM n'est promis.

## Usage des outils d'IA generative

Codex a analyse les supports et le contrat, implemente l'agence, produit les tests, les captures expurgees et la documentation, puis execute les verifications decrites ici.
La generation wsimport est celle de Metro depuis le contrat de A ; elle n'a pas ete remplacee par des classes ecrites a la main.
Les validations automatisees sont listees ci-dessus. La comprehension et la relecture humaines de l'ensemble ne sont pas presumees : les deux etudiants doivent les effectuer et expliquer leurs choix dans le rapport.

## References techniques

- Cours CM03 et enonce du TP2 fournis dans support/.
- Contrat et notes d'integration du binome sous contract/.
- Plugin officiel : https://eclipse-ee4j.github.io/metro-jax-ws/jaxws-maven-plugin/wsimport-mojo.html
- Delais Metro : https://eclipse-ee4j.github.io/metro-wsit/4.0.0/guide/ch05.html#http-timeouts
- Constantes verifiees directement dans le JAR jaxws-rt 4.0.5 avec javap -constants : JAXWSProperties.CONNECT_TIMEOUT et REQUEST_TIMEOUT.
