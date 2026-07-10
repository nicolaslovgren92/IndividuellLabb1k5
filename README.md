# IndividuellLabb1k5

Small Spring Boot service that analyzes video games using an AI backend (OpenAI).

Key points
- Java: 21 (configured in `pom.xml`)
- Build: Maven (project contains the Maven wrapper)
- Main HTTP endpoint: POST /api/ai

Requirements
- Java 21
- An OpenAI API key set as an environment variable `OPENAI_API_KEY` or in `application.properties` as `openai.api-key`.

Quick start (Windows PowerShell)

```powershell
# from project root
# set API key in current shell (temporary)
$env:OPENAI_API_KEY = 'sk-...'

# build
.\mvnw.cmd -DskipTests package

# run the app
.\mvnw.cmd spring-boot:run
```

Simple curl examples

# Send plain text game name
curl -X POST http://localhost:8080/api/ai -H "Content-Type: text/plain" --data "Elden Ring"

# Or send a JSON string body
curl -X POST http://localhost:8080/api/ai -H "Content-Type: application/json" --data '"Elden Ring"'

Behavior
- The service sends a structured system prompt to the OpenAI Chat API and expects a raw JSON object matching the `AiResponseDto` schema.
- If the API key is missing the application will fail on startup.
- If the AI response fails validation or parsing, the controller/service returns a fallback response.

Relevant files
- `src/main/java/com/example/individuelllabb1k5/controller/AiController.java` — REST controller
- `src/main/java/com/example/individuelllabb1k5/service/AiClientService.java` — calls OpenAI
- `src/main/java/com/example/individuelllabb1k5/dto/AiResponseDto.java` — expected response schema
- `src/main/resources/application.properties` — configuration (see `openai.api-key` and `server.port`)

Notes
- The project uses Lombok for DTO getters/setters; IDE may require Lombok plugin.
- Model and timeouts are configured in `AiClientService`.

License
- No license specified — add LICENSE if you intend to publish.
test for github action

