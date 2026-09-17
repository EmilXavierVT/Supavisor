# Contributing

Dette dokument beskriver de fælles kodestandarder og arbejdsgange for projektet. Formålet er at sikre ensartet kode, tydelig struktur og en fælles proces for udvikling og review.

## Navngivning og formattering

Projektet følger almindelige Java-konventioner.

* Klasser og interfaces skrives i `PascalCase`.

  * Eksempel: `UserController`, `EvaluationService`, `ReportDTO`
* Metoder og variabler skrives i `camelCase`.

  * Eksempel: `findUserById()`, `evaluationResult`
* Konstanter/Enums skrives med store bogstaver og underscore.

  * Eksempel: `MAX_FILE_SIZE`
* Packages skrives med små bogstaver.

  * Eksempel: `dk.example.evaluator.service`
* Navne skal være beskrivende og afspejle deres ansvar.
* Forkortelser bør undgås, medmindre de er alment forståelige, eksempelvis `DTO`, `API` og `HTTP`.
* Boolean-variabler og metoder bør så vidt muligt navngives som spørgsmål.

  * Eksempel: `isValid`, `hasPermission`, `canEvaluate`

Kode formatteres ensartet efter IntelliJ IDEA's standard Java-formattering.

Indrykning foretages med 4 mellemrum.

Braces placeres på samme linje som deklarationen:

```java
public void evaluateReport() {
    // kode
}
```

Metoder bør have ét klart ansvar og holdes så korte og overskuelige som muligt.

## Package- og mappestruktur

Kode organiseres efter ansvar.

Eksempel:

```text
src/main/java/dk/example/evaluator/
├── controller/
├── service/
├── model/
├── dto/
├── exception/
├── config/
```

Ansvarsfordelingen er:

* `controller`

  * Modtager HTTP requests.
  * Validerer request-data på API-niveau.
  * Kalder relevante services.
  * Returnerer HTTP responses.

* `service`

  * Indeholder applikationens forretningslogik.
  * Controllers må ikke indeholde egentlig forretningslogik.


* `model`

  * Indeholder domæneobjekter og entities.

* `dto`

  * Indeholder objekter, der anvendes til data ind og ud af API'et.

* `exception`

  * Indeholder projektets egne exceptions samt eventuelle globale exception handlers.

* `config`

  * Indeholder konfiguration af eksempelvis database, security og eksterne services.


Klasser bør placeres efter deres primære ansvar. Forretningslogik bør eksempelvis ikke placeres i controllers eller DTO'er.

## Endpoints

API-endpoints skal følge REST-principper så vidt muligt.

Resources navngives som substantiver i flertal:

```text
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
```

Undgå verbs i endpoint-navne, hvis handlingen allerede kan beskrives gennem HTTP-metoden.

Foretræk eksempelvis:

```text
POST /api/reports
```

frem for:

```text
POST /api/createReport
```

HTTP-metoder anvendes efter deres formål:

* `GET` læser data.
* `POST` opretter en resource eller starter en operation.
* `PUT` opdaterer en eksisterende resource.
* `DELETE` sletter en resource.

Endpoints bør returnere relevante HTTP-statuskoder.

Eksempler:

```text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
418 This is a teapot
500 Internal Server Error
```

## DTO'er

API'et må som udgangspunkt ikke eksponere database-entities direkte.

DTO'er anvendes mellem API'et og klienten.


 DTO´er anvendes udgangspunktvis som suffix:

```text
UserDTO
EvaluationDTO
```

Projektet skal så vidt muligt anvende én konsekvent navngivningsstrategi.

Request-DTO'er må kun indeholde data, som klienten må sende.

Response-DTO'er må kun indeholde data, som klienten må modtage.

Sensitive oplysninger såsom passwords, API-nøgler og interne tokens må aldrig returneres i responses.

## Validering

Input fra klienten må aldrig antages at være korrekt.

Request-data skal valideres så tidligt som muligt.

Der bør eksempelvis valideres for:

* obligatoriske felter
* tomme værdier
* ugyldige ID'er
* ugyldige enum-værdier
* ugyldige datoer
* for lange tekstfelter
* ugyldige filtyper eller filstørrelser

Eksempel:

```java
if (request.name() == null || request.name().isBlank()) {
    throw new ValidationException("Name is required");
}
```

Validering, der vedrører request-formatet, placeres typisk tæt på controller/API-laget.

Validering, der vedrører forretningsregler, placeres i service-laget.

## Fejlrespons

API-fejl skal returneres i et ensartet format.

Eksempel:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User with id 42 was not found"
}
```

En fejlrespons bør som minimum indeholde:

* HTTP-statuskode
* fejltype
* en forståelig fejlbesked

Interne implementation details eller stack traces må ikke returneres til klienten.

Eksempelvis må denne type information ikke eksponeres:

```text
NullPointerException at UserService.java:42
```

Interne fejl logges i stedet på serveren.

## Java exception handling

Exceptions skal anvendes til fejlsituationer og ikke som normal control flow.

Projektet bør anvende specifikke exceptions frem for generelle exceptions.

Foretræk:

```java
throw new UserNotFoundException(userId);
```

frem for:

```java
throw new RuntimeException("Something went wrong");
```

Det er tilladt at lade egne exceptions arve fra `RuntimeException`, når der er tale om applikationsfejl, som håndteres centralt.

Eksempel:

```java
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long id) {
        super("User with id " + id + " was not found");
    }
}
```

Exceptions bør så vidt muligt håndteres centralt.

Controllers bør derfor ikke være fyldt med gentagne `try/catch`-blokke.

Undgå:

```java
try {
    userService.findById(id);
} catch (Exception e) {
    // handle everything
}
```

Foretræk i stedet en global eller central exception handler, som oversætter exceptions til relevante HTTP-responses.

Exceptions må ikke ignoreres:

```java
try {
    doSomething();
} catch (Exception e) {
}

```

Hvis en exception håndteres, skal den enten:

* logges
* omsættes til en relevant anden exception
* håndteres på en måde, der løser fejlsituationen

Stack traces må ikke sendes direkte til klienten.

## Branch-navne

Der udvikles som udgangspunkt ikke direkte på `main`.

Hver opgave eller user story udvikles på sin egen branch.

Branch-navne skrives med små bogstaver og bindestreger.

Format:

```text
<type>/<issue-id>-kort-beskrivelse
```

Eksempler:

```text
feature/65-refresh-session
feature/4-custom-roles
fix/42-invalid-error-response
refactor/18-evaluation-service
docs/12-update-contributing
```

Anbefalede typer:

```text
feature/
fix/
refactor/
test/
docs/
chore/
```

Hvis arbejdet tager udgangspunkt i en GitHub Issue eller User Story, bør nummeret indgå i branch-navnet.

## Commits

Commits skal være små og beskrive én logisk ændring.

Commit-beskeder skrives kort og præcist.

Format:

```text
<type>: <kort beskrivelse>
```

Eksempler:

```text
feat: add refresh token handling
fix: return 404 when user is missing
test: add tests for role assignment
refactor: extract validation from controller
docs: update contributing guidelines
```

Anbefalede commit-typer:

```text
feat
fix
test
refactor
docs
chore
```

Undgå commit-beskeder som:

```text
update
changes
fix stuff
almost done
asdf
```

Commits bør så vidt muligt kunne forstås uden at læse hele diff'en.

## Pull Requests

Kode merges til `main` gennem en Pull Request.

En Pull Request skal som minimum indeholde:

* en kort beskrivelse af ændringen
* reference til relevant Issue eller User Story
* information om hvordan ændringen er testet

Eksempel:

```markdown
## Description

Implements refresh-token handling when a user's access token expires.

## Related issue

Closes #65

## Testing

- Tested successful token refresh
- Tested invalid refresh token
- Tested redirect to login when refresh fails
```

Pull Requests bør være fokuserede og kun omhandle én feature, bug eller opgave.

Store Pull Requests bør undgås, hvis ændringen med fordel kan opdeles i mindre dele.

Inden en Pull Request oprettes, skal udvikleren sikre:

* projektet kan compile
* eksisterende tests stadig består
* nye funktioner har relevante tests
* der ikke er unødvendig eller kommenteret kode
* der ikke er secrets, passwords eller API-nøgler i koden

En Pull Request bør som udgangspunkt reviewes af mindst ét andet gruppemedlem inden merge.

Kommentarer fra review skal enten løses eller diskuteres, før Pull Requesten merges.

## Definition of Done

En opgave betragtes ikke som færdig alene fordi koden virker lokalt.

Som minimum skal:

* acceptance criteria være opfyldt
* kode følge projektets kodestandard
* relevante tests være skrevet og bestå
* projektet kunne compile
* relevante fejlscenarier være håndteret
* Pull Request være oprettet og reviewet
* dokumentation være opdateret, hvis ændringen kræver det
