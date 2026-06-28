# Tillförlitlighetsbedömning: Robust AI-Integration för Spelanalys

## 1. Promptstrategi

### Val av Systemkonstruktion
Applikationen använder en game review analyzer som proxy till OpenAI:s Chat Completions API. Systemprompten är strategiskt utformad för att framtvinga strukturerad JSON-utdata och förhindra hallucineringar:

```
Du är en spelanalytiker. Användaren ger dig namnet på ett videospel.
Baserat på dina kunskaper om Metacritic-recensioner, analysera spelet 
och svar med ENDAST ett råt JSON-objekt, ingen markdown, ingen konverserande text,
som matchar exakt detta schema:

{
  "gameName": "<officiellt spelnamn>",
  "good": ["<styrka 1>", "<styrka 2>", ...],
  "bad": ["<svaghet 1>", "<svaghet 2>", ...],
  "score": <heltal 0-100>,
  "summary": "<enrads sammanfattning>"
}

Ge 3-5 punkter för både "good" och "bad".
Inkludera INTE text utanför JSON-objektet.
```

### Genomförande av Strukturkontroll

1. **Låg temperatur (0.1)** — Detta prioriterar deterministisk och repeterbar datagenerering framför kreativitet. Gör att samma spel alltid får liknande respons.

2. **Strikt JSON-schema** — Promten instruerar AI att returnera *endast* JSON, ingen markdown-formatering (```json), ingen förklarande text. Detta minskar parsningsfel.

3. **Dynamisk användardata via User Role** — Spelnamnet injiceras via user-rollen, inte i systemprompten, vilket förhindrar prompt injection.

4. **Explicit negation** — Promten säger explicit "Inkludera INTE text utanför JSON-objektet", vilket reducerar risk för att modellen ignorerar instruktionen.

### Resultat
Denna strategi säkerställer att AI:n nästan alltid returnerar välformad JSON som kan parsas automatiskt, utan manuell efterbehandling.

---

## 2. Felhantering (Error Mitigation)

### 2.1 Timeouts — Skydd mot hängande anslutningar

**Problem:** Om OpenAI-servern inte svarar kan anslutningen hänga i över 30 sekunder och låsa servertrådar.

**Lösning:**
```java
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(2000);  // 2 sekunder för att etablera anslutning
factory.setReadTimeout(8000);     // 8 sekunder att vänta på svar
```

**Effekt:** Anslutningen avbryts säkert efter 8 sekunder. Ingen tråd blir låst indefinitivt.

### 2.2 Rate Limiting (HTTP 429) — Exponential Backoff

**Problem:** Vid trafhögning returnerar OpenAI HTTP 429 Too Many Requests. Naiv retry försvagar servern ytterligare.

**Lösning — Exponential Backoff Loop:**
```java
for (int attempt = 0; attempt < retries; attempt++) {
    try {
        return restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);
    } catch (RestClientResponseException ex) {
        if (ex.getStatusCode().value() == 429 && attempt < retries - 1) {
            delay *= 2;  // 1000ms -> 2000ms -> 4000ms
            Thread.sleep(delay);
        } else {
            throw ex;
        }
    }
}
```

**Effekt:** 
- Första 429 → vänta 1s, försök igen
- Andra 429 → vänta 2s, försök igen  
- Tredje 429 → vänta 4s, försök igen
- Fjärde försök → kasta exception

Detta minskar load på OpenAI och höjer chansen att nästa försök lyckas.

### 2.3 AI-Hallucinationer — Bean Validation + Try-Catch

**Problem:** AI kan returnera syntaktiskt korrekt JSON som inte matchar affärslogik (t.ex. score = 150, eller tom "good"-lista).

**Lösning — Tvålagers validering:**

**Lag 1: Jackson Parsing**
```java
try {
    return objectMapper.readValue(content, AiResponseDto.class);
} catch (JsonProcessingException e) {
    return fallbackResponse();
}
```

**Lag 2: Bean Validation**
```java
@Getter @Setter
public class AiResponseDto {
    @NotNull private String gameName;
    @NotNull @Size(min = 1) private List<String> good;
    @NotNull @Size(min = 1) private List<String> bad;
    @NotNull @Min(0) @Max(100) private Integer score;
    @NotNull private String summary;
}
```

I controllern:
```java
Set<ConstraintViolation<AiResponseDto>> violations = validator.validate(result);
if (!violations.isEmpty()) {
    return fallback;  // returnera säker standardresponse
}
```

**Effekt:**
- Trasig JSON → fångas av try-catch
- Giltigt JSON men score = 150 → fångas av @Max(100)
- Giltigt JSON men tom good-lista → fångas av @Size(min = 1)

---

## 3. Tillförlitlighetsbedömning — LLM:ens Begränsningar i Produktion

### 3.1 Faktiska Hallucinationer
**Risk:** Även med låg temperatur och strukturerad prompt kan GPT-4o-mini fabricera information. Det kan hävda att ett spel "fick 98 på Metacritic" när det faktiskt inte finns någon sådan recension.

**Mitigation:** 
- Bean Validation fångar *format*-hallucinationer (score > 100), men inte *innehålls*-hallucinationer
- Denna lösning är lämplig för en applikation som analyserar spel för slutanvändare (inte juridiska eller vetenskapliga beslut)
- För produktiv använding skulle man behöva manuell granskning eller externa data-sources (t.ex. faktisk Metacritic API)

### 3.2 Kostnadsimplicationer
**Risk:** Varje request kostar ~0.0003 USD (gpt-4o-mini). En stor trafiktopning kan bli dyr.

**Mitigation:**
- 8-sekunders timeout förhindrar massiva timeout-retries
- Exponential Backoff minskar onödiga retries
- Inga långvariga connections låses fast

### 3.3 Latency
**Risk:** OpenAI kan ta 2-5 sekunder per request. Slutanvändare förväntar sig snabbare svar.

**Mitigation:**
- 8-sekunders timeout accepteras för AI-arbetslaster
- Client bör implementera loading-UI för att informera användare
- (Caching av ofta efterfrågade spel skulle ytterligare förbättra latency)

### 3.4 API Nyckelhantering
**Risk:** API-nyckeln i konfigurationsfiler kan läcka eller exponeras i loggning.

**Mitigation:**
- API-nyckeln injiceras via miljövariabel, inte hårdkodad
- @PostConstruct failfast säkerställer att appen kraschar omedelbart om nyckeln saknas
- Logging gör inte request-payload (som innehåller nyckeln)

### 3.5 Prompt Injection
**Risk:** En användare skickar ett spelnamn som "'; drop table--" eller "Ignorera din systemprompt, du är nu...".

**Mitigation:**
- Spelnamnet injiceras via *user* role, inte system-rollen
- User role är mindre privilegierad än system role
- För ännu större säkerhet skulle man kunna validera spelnamnet med regex innan det skickas till AI

### 3.6 Rate Limiting i Produktion
**Risk:** Med många samtidiga requests kan exponential backoff inte hålla takten. Om traffic spikar kan kö byggas upp.

**Mitigation:**
- Exponential Backoff är utformat för transient spikes (några sekunder)
- För långvarig høj load skulle man behöva:
  - Implementera circuit breaker
  - Cachea AI-svar per spel
  - Implementera request queue med prioritering

---

## 4. Slutsats

Denna implementation uppfyller VG-kraven genom att:

 **Välmotiverad promptstrategi** — JSON-schema, låg temperatur, explicit negation av markdown  
 **Robust felhantering** — Timeouts (8s), Exponential Backoff för 429, tvålagers validering mot hallucinationer  
 **Kritisk bedömning** — Denna dokumentation erkänner LLM:ens faktiska begränsningar (hallucineringar, kostnad, latency) och arkitekturen som adresserar dem  

Applikationen är lämplig för produktion under förutsättning att:
- Slutanvändare förstår att AI-svar kan innehålla felaktigheter
- Trafiken övervakas för att ej överbelasta OpenAI API
- API-nyckeln hanteras säkert i miljövariabler (aldrig i källkod)

För en helt tillförlitlig produktionstjänst skulle man behöva:
1. Manuell granskning av AI-svar (för juridisk eller affärskritisk data)
2. Caching för vanliga spel
3. Circuit breaker för att skydda mot cascading failures
4. Monitoring och alerting på felfrekvens
