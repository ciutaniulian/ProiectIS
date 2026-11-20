# Plan de implementare pentru grupurile Expense Splitter

**Stare** — aprobat și implementat.

## Obiectiv și limite

După aprobare, implementează fluxul complet în același ciclu de lucru. Un utilizator creează un grup, invită persoane, discută cu membrii și poate gestiona grupul până la arhivare, restaurare sau închidere. Livrează împreună datele, API-ul, interfața, testele și actualizarea specificației. Cheltuielile, soldurile, datoriile, plățile și rapoartele financiare sunt în afara acestui plan.

Pornește de la modificările deja prezente în directorul de lucru. Refolosește modelul de grup, autentificarea Spring Security, serviciile de chat, clientul STOMP și suita Playwright. Păstrează modificările locale și datele existente.

## Reguli de produs

| Acțiune | Regulă |
| --- | --- |
| Creare și listare | Orice cont autentificat poate crea un grup cu nume obligatoriu de cel mult 100 de caractere și descriere opțională de cel mult 500. Creatorul devine membru în aceeași tranzacție. Lista arată doar grupurile proprii și numărul real de membri, cu căutare după nume și filtre Activ / Arhivat. |
| Detalii și editare | Orice membru poate consulta pagina grupului, membrii și activitatea. Orice membru al unui grup activ poate edita numele și descrierea. Grupul are o adresă stabilă, care funcționează la reîncărcare. |
| Invitații | Orice membru al unui grup activ invită prin adresă exactă de e-mail. Destinatarul vede invitația în cont și poate accepta sau refuza. Pentru cineva fără cont, membrul poate copia un link și îl poate distribui. Linkul se poate folosi după autentificare cu adresa invitată. Trimiterea automată de e-mail este în afara planului. |
| Gestionarea invitațiilor | O invitație expiră după 7 zile. Există cel mult una în așteptare pentru aceeași combinație grup–adresă. Un membru poate revoca invitația, iar după expirare, refuz sau revocare poate genera una nouă. Destinatarul devine membru doar la acceptare. Crearea invitației are același răspuns indiferent dacă adresa are cont. |
| Eliminare și ieșire | Orice membru al unui grup activ poate elimina alt membru, inclusiv creatorul, cu confirmare. Orice membru poate părăsi grupul, inclusiv când este arhivat. Apartenența și accesul încetează, iar mesajele rămân în istoric. Revenirea se face prin invitație nouă într-un grup activ. |
| Arhivare și restaurare | Orice membru poate arhiva grupul cu confirmare și îl poate restaura. În arhivă se pot consulta detaliile, membrii, activitatea și istoricul chatului. Editarea, eliminarea altor membri, operațiile cu invitații și trimiterea mesajelor sunt suspendate. Termenul invitațiilor continuă să curgă. |
| Închidere | Dacă pleacă ultimul membru, acesta confirmă explicit închiderea. Grupul trece în starea „Închis”, invitațiile rămase sunt revocate și datele sunt păstrate. Închiderea este definitivă în interfața utilizatorilor. |
| Chat și activitate | În grupurile active, membrii trimit mesaje text în timp real. Încarcă istoricul și recuperează mesajele lipsă după reconectare. Activitatea afișează autorul, data și acțiunea pentru creare, editare, aderare, ieșire, eliminare, arhivare și restaurare. |
| Permisiuni | Drepturile în grup provin din apartenență. Creatorul are aceleași drepturi ca ceilalți membri. Administratorul global urmează aceleași reguli pentru grupuri și chat. Invitații pot consulta doar informația minimă necesară pentru a decide dacă acceptă. |

## Ordinea de implementare

### 1. Stabilirea situației de pornire și migrarea datelor

- Inventariază modificările locale și rulează suitele existente. Identifică testele care reflectă vechea regulă rezervată creatorului.
- Extinde grupul persistent cu descriere și stările Activ, Arhivat, Închis. Grupele existente pornesc ca active. Orice referință istorică validă la creator se păstrează ca metadată. Apartenența rămâne unică pentru perechea grup–utilizator.
- Adaugă date persistente pentru invitații și activitate. Leagă evenimentele de grup, autor, moment și, unde este cazul, persoana afectată. Înregistrează evenimentele în tranzacția acțiunii. Istoricul de dinaintea migrării reflectă doar datele cunoscute.
- Verifică grupurile de chat care au apartenențe sau mesaje, dar nu au încă o înregistrare în `expense_groups`. Păstrează ID-urile și accesul membrilor existenți, creând înregistrări pentru acele ID-uri cu numele cunoscut sau cu un nume de rezervă. Păstrează privat orice istoric fără membri. Verifică generatorul de ID-uri și repetarea migrării pe scheme vechi și noi în H2 și MySQL.

### 2. API, apartenență și permisiuni

- Extinde `/api/groups` pentru listare, creare, detalii, editare, membri și activitate paginată. Adaugă operații explicite de ieșire, arhivare și restaurare. Elimină dependența operațiilor de `canManage` bazat pe creator.
- Înlocuiește adăugarea directă cu invitații pentru grup, lista invitațiilor primite ale contului și operații de acceptare, refuz și revocare. Normalizează adresa ca la autentificare. Generează un token aleatoriu pentru link, stochează un hash și verifică adresa contului la folosire. Testează stările în așteptare, acceptată, refuzată, revocată și expirată.
- Centralizează verificarea apartenenței și a stării grupului în serviciile comune. Folosește identitatea sesiunii și păstrează protecția CSRF pentru mutații. Returnează erori distincte pentru date invalide și conflicte de stare, fără a expune detaliile grupului către conturi din afară.
- Serializează operațiile concurente care afectează același grup. Acceptarea creează o singură apartenență. Ieșirea ultimului membru, arhivarea și acceptarea simultană produc o stare coerentă, iar jurnalul conține doar acțiuni confirmate.

### 3. Chat în timp real și istoric

- Refolosește endpointul REST de istoric și conexiunea STOMP. Verifică apartenența la citire, abonare, trimitere și livrare. Blocarea trimiterii într-un grup arhivat se aplică pe server. Publică mesajele după confirmarea tranzacției.
- La plecare sau eliminare, utilizatorul pierde accesul la istoricul REST și la mesajele noi, inclusiv printr-un abonament anterior. La reîntoarcere, clientul se abonează din nou. Interfața tratează pierderea accesului și starea de arhivă în timp ce chatul este deschis.
- Folosește ordonare stabilă și paginare după cursor pentru mesaje. Îmbină istoricul și mesajele live după ID. La reconectare, încarcă intervalul lipsă chiar dacă depășește prima pagină. Păstrează textul introdus când trimiterea eșuează, afișează starea conexiunii și confirmă rularea pe două sesiuni distincte.

### 4. Interfață React

- Înlocuiește îmbinarea grupurilor demo cu lista reală. Folosește API-ul ca sursă pentru lista, numărătorile, starea grupului și apartenență. Adaugă căutare, filtre, creare și stări de încărcare, eroare și listă goală.
- Adaugă pagina grupului și navigarea pentru detalii, membri, invitații, activitate și chat. Verifică reîncărcarea, istoricul de navigare și revenirea dintr-un link de invitație după autentificare sau înregistrare.
- Adaugă lista invitațiilor primite și acțiunile de acceptare, refuz, revocare și copiere a linkului. Actualizează listele după fiecare operație și la revenirea în interfață. Pentru eliminare, ieșire, arhivare și închidere, confirmarea indică persoana sau grupul afectat și consecința acțiunii.
- Verifică fluxul la desktop, pe telefon și cu tastatura. După pierderea accesului, închide vizualizarea protejată, actualizează lista și afișează un mesaj clar.

### 5. Verificare și livrare

| Verificare | Criteriu de acceptare |
| --- | --- |
| Conturi separate | A creează grupul, B acceptă o invitație și vede grupul după reîncărcare. B poate edita grupul și gestiona membri cu aceleași drepturi ca A. C, ca utilizator autentificat sau administrator global din afara grupului, nu primește detaliile, lista membrilor sau istoricul chatului. |
| Invitații | Testează destinatar înregistrat și destinatar nou, adresă greșită, acceptări simultane, invitație duplicată, revocare, refuz și expirare. O invitație legitimă adaugă exact un membru. |
| Chat | Două sesiuni văd mesajele în timp real. Istoricul se poate pagina. Reconectarea recuperează mesajele ratate fără duplicate. După eliminarea unui membru cu o conexiune deschisă, acesta nu poate citi, posta sau primi mesaje noi. |
| Ciclu de viață | Arhivarea permite lectura și respinge scrierile. Restaurarea reactivează grupul. Ieșirea și eliminarea actualizează lista și numărul membrilor. Închiderea ultimului membru păstrează datele și încheie accesul prin invitații. |
| Compatibilitate | Membrii și mesajele din datele existente rămân accesibile utilizatorilor autorizați, cu aceleași ID-uri. Migrarea repetată păstrează starea. |

Rulează `./mvnw.cmd test` la rădăcină și `npm test`, `npm run lint`, `npm run build` și `npm run test:e2e` din `frontend`. Rulează scenariul de migrare și testele de persistență și pe MySQL, într-o bază izolată. Actualizează `docs/Expense-Splitter.md` cu regulile aprobate. Raportează rezultatele reale și orice verificare blocată de mediu.

Funcționalitatea este gata când scenariile de acceptare trec și fiecare acțiune din acest plan poate fi parcursă din interfață. După aprobare, etapele se execută succesiv într-o singură sarcină, cu un raport final comun.
