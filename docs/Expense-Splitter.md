# Expense Splitter

> Aplicație pentru gestionarea și împărțirea cheltuielilor în grupuri

## 1. Ideea proiectului

**Expense Splitter** este o aplicație web care ajută grupurile de persoane să își gestioneze cheltuielile comune.

Aplicația poate fi folosită în situații precum:

- excursii cu prietenii;
- colegi de apartament;
- comenzi de grup;
- ieșiri în oraș;
- vacanțe;
- cheltuieli comune recurente.

Un utilizator poate crea un grup și poate adăuga cheltuieli în cadrul acestuia.

Pentru fiecare cheltuială se poate specifica:

- **cine a plătit**;
- **suma totală**;
- **pentru cine s-a plătit**;
- **cum se împarte suma** între participanți.

Aplicația calculează automat soldurile dintre membrii grupului și determină cine trebuie să plătească cui pentru a echilibra cheltuielile.

Un obiectiv important al aplicației este și **simplificarea datoriilor**, astfel încât numărul de plăți necesare între membrii grupului să fie cât mai mic.

## 2. Exemplu simplu de utilizare

Un grup are trei membri: **Andrei, Mihai și Ana**.

Andrei plătește **150 lei** pentru cină pentru toate cele trei persoane. Fiecare persoană trebuie să suporte **50 lei**.

| Membru | Situație |
| --- | --- |
| Andrei | A plătit cu 100 lei mai mult decât partea lui. |
| Mihai | Îi datorează 50 lei lui Andrei. |
| Ana | Îi datorează 50 lei lui Andrei. |

Aplicația trebuie să calculeze automat aceste solduri.

## 3. Utilizatorul aplicației

Un utilizator poate:

- să își creeze un cont;
- să se autentifice;
- să creeze un grup;
- să invite sau să adauge membri într-un grup;
- să vadă grupurile din care face parte;
- să adauge cheltuieli;
- să specifice cine a plătit o cheltuială;
- să aleagă cine participă la cheltuială;
- să aleagă modul în care se împarte suma;
- să vadă istoricul cheltuielilor;
- să vadă soldul propriu;
- să vadă cine îi datorează bani;
- să vadă cui îi datorează bani;
- să marcheze o datorie ca fiind achitată.

## 4. Cerințe funcționale de bază

Aplicația trebuie să permită:

1. Autentificarea utilizatorilor.
2. Crearea unui grup de cheltuieli.
3. Adăugarea sau invitarea membrilor într-un grup.
4. Vizualizarea membrilor unui grup.
5. Adăugarea unei cheltuieli.
6. Specificarea utilizatorului care a plătit cheltuiala.
7. Selectarea persoanelor pentru care a fost făcută cheltuiala.
8. Împărțirea unei cheltuieli:
   - în mod egal;
   - după sume stabilite manual;
   - opțional, după procente.
9. Calcularea automată a soldului fiecărui membru.
10. Calcularea relațiilor de datorie:
    - cine trebuie să plătească;
    - cui trebuie să plătească;
    - ce sumă trebuie plătită.
11. Vizualizarea istoricului cheltuielilor.
12. Înregistrarea unei plăți între doi membri.
13. Actualizarea soldurilor după efectuarea unei plăți.
14. Vizualizarea situației financiare a întregului grup.
15. Existența unuia sau mai multor administratori la nivelul aplicației.
16. Ștergerea unei cheltuieli exclusiv de către un administrator al aplicației.
17. Protejarea accesului și a operațiilor prin Spring Security.

## 5. Reguli de business

- O cheltuială aparține unui grup.
- Utilizatorii obișnuiți pot vedea doar cheltuielile grupurilor din care fac parte. Administratorii aplicației le pot consulta în interfața de administrare pentru gestionarea cheltuielilor.
- Persoana care a plătit o cheltuială trebuie să fie membră a grupului.
- Persoanele între care se împarte cheltuiala trebuie să fie membre ale grupului.
- Suma împărțită între membri trebuie să fie egală cu valoarea totală a cheltuielii.
- Soldurile membrilor trebuie actualizate după fiecare cheltuială nouă.
- Soldurile trebuie actualizate după fiecare plată realizată între membri.
- Un utilizator nu poate achita o datorie mai mare decât suma datorată, dacă aplicația folosește plata directă a unei datorii.
- Istoricul cheltuielilor trebuie păstrat, inclusiv evidența cheltuielilor șterse.
- Doar un administrator al aplicației poate șterge o cheltuială; utilizatorii obișnuiți nu pot șterge nici cheltuielile adăugate de ei.
- Toți membrii unui grup au aceleași permisiuni. Creatorul grupului nu primește drepturi suplimentare.
- Rolul de administrator este global și nu depinde de apartenența la un grup.
- Ștergerea sau modificarea unei cheltuieli trebuie să ducă la recalcularea soldurilor.

## 6. Simplificarea datoriilor

Aceasta poate fi una dintre funcționalitățile importante ale proiectului.

**Exemplu:**

- Andrei îi datorează lui Mihai **50 lei**.
- Mihai îi datorează Anei **50 lei**.

### Înainte de simplificare

| Cine plătește | Cui plătește | Sumă |
| --- | --- | ---: |
| Andrei | Mihai | 50 lei |
| Mihai | Ana | 50 lei |

### După simplificare

| Cine plătește | Cui plătește | Sumă |
| --- | --- | ---: |
| Andrei | Ana | 50 lei |

Scopul este **reducerea numărului de tranzacții** necesare pentru stingerea tuturor datoriilor dintr-un grup.

## 7. Tipuri de împărțire a unei cheltuieli

### Varianta 1 — Împărțire egală

**Exemplu:** 120 lei împărțiți între 3 persoane.

Fiecare persoană plătește **40 lei**.

### Varianta 2 — Sume diferite

**Cheltuială totală: 120 lei.**

| Membru | Sumă |
| --- | ---: |
| Andrei | 50 lei |
| Mihai | 40 lei |
| Ana | 30 lei |

### Varianta 3 — Procente *(opțional)*

**Cheltuială totală: 200 lei.**

| Membru | Procent | Sumă rezultată |
| --- | ---: | ---: |
| Andrei | 50% | 100 lei |
| Mihai | 30% | 60 lei |
| Ana | 20% | 40 lei |

## 8. Funcționalități opționale

Aceste funcționalități **nu sunt obligatorii pentru prima versiune**. Pot fi adăugate ulterior:

- categorii pentru cheltuieli: food, transport, accommodation, utilities etc.;
- descriere sau notițe pentru fiecare cheltuială;
- atașarea unei poze cu bonul;
- cheltuieli recurente;
- notificări pentru datorii neachitate;
- notificări când cineva adaugă o cheltuială;
- statistici despre cheltuielile grupului;
- grafice cu cheltuielile pe categorii;
- raport lunar;
- export al cheltuielilor;
- mai multe monede;
- conversie valutară;
- posibilitatea de a arhiva un grup;
- posibilitatea de a părăsi un grup;
- comentarii la o cheltuială;
- căutare și filtrare în istoricul cheltuielilor.

## 9. Ce face proiectul interesant

Proiectul nu constă doar în operații simple de tip **CRUD**.

Aplicația trebuie să rezolve logică reală de business:

- împărțirea corectă a sumelor;
- calcularea soldurilor;
- determinarea datoriilor dintre utilizatori;
- actualizarea soldurilor după plăți;
- recalcularea după modificarea unei cheltuieli;
- simplificarea datoriilor;
- validarea sumelor și a participanților.

Acest lucru oferă proiectului suficientă complexitate pentru un proiect universitar realizat în echipă.

## 10. Varianta de bază / MVP

Pentru prima versiune, proiectul poate conține doar:

- autentificare și autorizare cu Spring Security;
- roluri globale USER și ADMIN, cu unul sau mai mulți administratori ai aplicației;
- interfață de administrare și ștergerea cheltuielilor de către administratorii aplicației;
- utilizatori;
- grupuri;
- membri;
- cheltuieli;
- împărțirea egală sau manuală;
- calcularea soldurilor;
- vizualizarea datoriilor;
- înregistrarea unei plăți;
- istoric.

Funcționalitățile mai complexe pot fi adăugate doar dacă există suficient timp.

## 11. Formulare scurtă pentru prezentarea ideii

> **Expense Splitter** este o aplicație web pentru gestionarea și împărțirea cheltuielilor în grupuri.
>
> Utilizatorii pot crea grupuri, adăuga cheltuieli și specifica cine a plătit și între cine trebuie împărțită suma.
>
> Aplicația calculează automat soldurile fiecărui membru și determină cine trebuie să plătească cui.
>
> În plus, sistemul poate simplifica datoriile astfel încât numărul de tranzacții necesare pentru echilibrarea cheltuielilor să fie cât mai mic.

## 12. Direcția proiectului

Pentru moment, proiectul trebuie privit ca o aplicație de gestionare a cheltuielilor comune.

**Prioritatea este stabilirea clară a cerințelor și a regulilor aplicației.**

**Spring Security va fi folosit pentru autentificare și controlul accesului**, conform secțiunii 14.

Celelalte detalii tehnice precum:

- arhitectura;
- baza de date;
- Java;
- Spring Boot;
- API-urile;
- împărțirea proiectului între membrii echipei;

vor fi stabilite după ce ideea și cerințele sunt finalizate.

## 13. Administratorii aplicației și permisiunile

Aplicația are două roluri globale: **USER** (utilizator obișnuit) și **ADMIN** (administrator al aplicației). Pot exista **unul sau mai mulți administratori**.

**Toți membrii unui grup au aceleași permisiuni**, inclusiv persoana care a creat grupul. Apartenența la un grup nu conferă rolul de administrator. Egalitatea permisiunilor nu impune împărțirea egală a sumelor: metodele de împărțire din secțiunea 7 rămân disponibile.

Administratorul gestionează cheltuielile la nivelul întregii aplicații, printr-o interfață de administrare, și poate șterge un bill din orice grup fără să fie membru al acelui grup.

### Permisiuni

| Acțiune | USER | ADMIN |
| --- | --- | --- |
| Crearea unui grup | Da | Da |
| Vizualizarea datelor și adăugarea cheltuielilor în propriile grupuri | Da | Da, ca membru |
| Invitarea sau adăugarea membrilor în propriile grupuri | Da | Da, ca membru |
| Înregistrarea unei plăți proprii către alt membru | Da | Da, ca membru |
| Consultarea cheltuielilor din toate grupurile în interfața de administrare | Nu | Da |
| Ștergerea unei cheltuieli (bill) din orice grup | Nu | Da |
| Consultarea jurnalului ștergerilor administrative | Nu | Da |

Rolul ADMIN nu permite înregistrarea de plăți în numele altor utilizatori. Pentru operațiile obișnuite de participare la un grup, administratorul respectă aceleași reguli ca orice membru.

### Interfața de administrare

Administratorii au acces la o pagină dedicată în care pot:

- vedea lista cheltuielilor din aplicație și grupul fiecăreia;
- căuta sau filtra cheltuielile după grup, dată și utilizatorul care le-a adăugat;
- consulta detaliile unui bill înainte de a interveni;
- șterge un bill, introducând motivul și confirmând operația;
- consulta istoricul ștergerilor, inclusiv administratorul, motivul și data.

### Atribuirea rolurilor

- Conturile create prin înregistrarea publică primesc automat rolul USER.
- Conturile ADMIN sunt configurate printr-un proces controlat de operatorul aplicației, de exemplu prin inițializare sau administrarea securizată a bazei de date.
- Un utilizator nu își poate acorda singur rolul ADMIN prin formularul de înregistrare sau printr-o cerere către API.
- Fiecare administrator folosește un cont propriu, pentru a putea identifica autorul fiecărei intervenții.

### Ștergerea unei cheltuieli

1. Administratorul selectează bill-ul în interfața de administrare, introduce motivul și confirmă ștergerea.
2. Serverul verifică rolul global ADMIN și existența cheltuielii.
3. Cheltuiala este marcată ca ștearsă, păstrându-se datele originale, administratorul, motivul și momentul operației.
4. Cheltuiala nu mai este inclusă în calculele active. Soldurile și datoriile grupului se recalculează pe baza cheltuielilor active și a plăților deja înregistrate.
5. Actualizarea cheltuielii, evidența intervenției și recalcularea se execută într-o singură tranzacție, pentru a evita o situație financiară inconsistentă.

**Plățile deja efectuate rămân în istoric.** Ștergerea unei cheltuieli poate schimba soldurile și poate genera o sumă de restituit.

Un utilizator obișnuit nu poate șterge o cheltuială, chiar dacă a adăugat-o, a plătit-o sau a creat grupul. Ascunderea butonului în interfață trebuie completată de verificarea permisiunii pe server.

## 14. Securitatea aplicației — Spring Security

Aplicația va folosi **Spring Security** pentru autentificare și autorizare. Autentificarea stabilește cine este utilizatorul, iar autorizarea stabilește ce operații poate efectua. Framework-ul permite reguli atât pentru cererile HTTP, cât și pentru metodele din aplicație. [Documentație oficială: autorizare](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html).

### 14.1. Conturi și autentificare

- Utilizatorul se înregistrează cu o adresă de e-mail unică și o parolă.
- Parolele se stochează sub formă de hash prin `PasswordEncoder`, folosind un algoritm adaptiv precum BCrypt; nu se păstrează în clar. [Documentație oficială: stocarea parolelor](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
- Serverul preia identitatea utilizatorului din contextul de securitate, fără să accepte un identificator trimis de client drept dovadă a identității.
- Mesajele de autentificare eșuată nu dezvăluie dacă o adresă de e-mail există în sistem.
- Autentificarea este protejată prin limitarea încercărilor repetate, implementată în aplicație sau în infrastructură.

### 14.2. Sesiuni și protecția cererilor

Pentru MVP, soluția propusă este autentificarea cu **sesiune pe server** și cookie de sesiune.

- În producție, comunicația se face prin HTTPS, iar cookie-ul de sesiune folosește atributele `HttpOnly`, `Secure` și o politică `SameSite` potrivită interfeței.
- Sesiunea expiră după o perioadă de inactivitate și este invalidată la deconectare.
- Protecția CSRF rămâne activă pentru operațiile care modifică date, inclusiv adăugarea și ștergerea cheltuielilor și înregistrarea plăților.
- Dacă interfața și serverul au origini diferite, CORS permite explicit doar originile necesare. CORS nu înlocuiește autorizarea.

### 14.3. Roluri globale și acces la grupuri

Prin `SecurityFilterChain`, rutele de înregistrare și autentificare sunt publice. Rutele obișnuite pentru grupuri, cheltuieli, solduri și plăți necesită autentificare, iar rutele administrative, de exemplu `/api/admin/**`, necesită rolul ADMIN. [Documentație oficială: reguli pentru cereri HTTP](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html).

Serverul aplică următoarele reguli:

- Rolurile USER și ADMIN sunt asociate contului la nivelul aplicației; apartenența la grup nu conține un rol de administrator.
- Pentru operațiile obișnuite, serverul verifică apartenența la grup. Toți membrii beneficiază de aceleași permisiuni.
- Consultarea cheltuielilor pentru administrare și ștergerea unui bill sunt permise conturilor ADMIN, inclusiv când administratorul nu face parte din grup.
- Ștergerea este verificată și în stratul de servicii, de exemplu prin `@PreAuthorize("hasRole('ADMIN')")`, cu method security activat prin `@EnableMethodSecurity`.
- În convenția Spring Security, `hasRole('ADMIN')` verifică autoritatea `ROLE_ADMIN`; rolurile stocate trebuie mapate consecvent la autorități.
- Orice altă rută care ar permite ștergerea trebuie să respecte aceeași regulă; o rută obișnuită de grup nu poate ocoli controlul ADMIN.
- Serverul verifică legătura dintre bill și grup atunci când ambele identificatoare apar în cerere, pentru a recalcula grupul corect.
- Identitatea și rolurile sunt preluate din contextul de securitate, nu din valori furnizate de client. Clientul nu poate acorda roluri sau înregistra plăți în numele altcuiva.

Autorizarea metodelor completează regulile pentru cererile HTTP. [Documentație oficială: method security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html).

### 14.4. Validarea datelor și evidența operațiilor

- Sumele trebuie să fie pozitive, iar totalul părților trebuie să coincidă cu valoarea cheltuielii.
- Persoana care a plătit și toți participanții trebuie să aparțină grupului.
- Serverul validează datele independent de verificările din interfață.
- Operațiile importante, în special ștergerea cheltuielilor, păstrează utilizatorul, data și obiectul afectat.
- Parolele, cookie-urile de sesiune și alte date de autentificare nu se includ în loguri.

### 14.5. Scenarii de verificare

| Scenariu | Rezultat așteptat |
| --- | --- |
| Utilizator neautentificat încearcă să acceseze datele unui grup prin API | Cererea este respinsă cu `401 Unauthorized`. |
| Membru încearcă să șteargă un bill, inclusiv unul creat de el | Cererea este respinsă cu `403 Forbidden`; datele rămân intacte. |
| Creatorul unui grup, cu rol USER, încearcă să șteargă un bill | Cererea este respinsă cu `403 Forbidden`; creatorul are aceleași permisiuni ca ceilalți membri. |
| Utilizator schimbă identificatorul grupului sau al bill-ului pentru a accesa date străine | Cererea este respinsă fără a expune datele grupului. |
| Administrator al aplicației șterge un bill dintr-un grup în care nu este membru | Operația este permisă; bill-ul este marcat ca șters, intervenția este înregistrată, iar soldurile grupului sunt recalculate. |
| Utilizator cu rol USER încearcă să acceseze interfața sau API-ul de administrare | Accesul este respins cu `403 Forbidden`. |
| Utilizator trimite rolul ADMIN la înregistrare | Contul primește doar rolul USER; clientul nu poate acorda drepturi administrative. |
| Doi administratori diferiți șterg cheltuieli | Fiecare intervenție este asociată contului administratorului care a executat-o. |
| O cerere care modifică date nu are un token CSRF valid | Cererea este respinsă. |
| Utilizator se deconectează și reutilizează sesiunea veche | Accesul la datele protejate este respins. |

