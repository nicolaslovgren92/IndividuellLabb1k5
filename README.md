# IndividuellLabb1k5

AI-powered video game analyzer—a Spring Boot REST API that integrates with OpenAI's GPT models to provide structured game analysis and recommendations.

## Key Features

- **Secure API**: Authentication via `X-API-Key` header (Spring Security filter)
- **Robust AI Integration**: Structured prompt engineering with JSON schema enforcement, exponential backoff for rate limiting, and multi-layer validation
- **Security Scanning**: Automated OWASP Dependency-Check in CI/CD pipeline (fails build on CVE score ≥ 7)
- **Error Mitigation**: Timeouts (2s connect, 8s read), hallucination detection via Bean Validation, fallback responses
- **Production Ready**: Java 21, Spring Boot 4.1, comprehensive error handling

## Quick Start

### Requirements

- **Java 21**
- **OpenAI API key** – set as environment variable `OPENAI_API_KEY` (or in `application.properties` as `openai.api-key`)
- **Application API key** – set as environment variable `APP_API_KEY` (for endpoint authentication)

### Build & Run (Windows PowerShell)

```powershell
# Set API keys (temporary in current shell)
$env:OPENAI_API_KEY = 'sk-...'
$env:APP_API_KEY = 'your-secret-api-key'

# Build (skipping tests)
.\mvnw.cmd -DskipTests package

# Run the application
.\mvnw.cmd spring-boot:run
```

The app will fail fast on startup if `OPENAI_API_KEY` or `APP_API_KEY` is missing.

## Docker

### Build Docker Image

```powershell
# Build the image
docker build -t individuelllabb1k5:latest .
```

### Run in Docker Container

```powershell
# Run with environment variables
docker run -p 8080:8080 `
  -e OPENAI_API_KEY="sk-..." `
  -e APP_API_KEY="your-secret-api-key" `
  individuelllabb1k5:latest
```

The Dockerfile uses a **multi-stage build**:
- **Stage 1 (Builder)**: Maven 3.9 with Java 21 builds the JAR
- **Stage 2 (Runtime)**: Lightweight Alpine JRE runs the application

This approach minimizes the final image size by excluding build tools from the production container.

### Docker Compose

For convenient local development with environment variable management:

```bash
# Create a .env file (or set variables in your shell)
export OPENAI_API_KEY="sk-..."
export APP_API_KEY="your-secret-api-key"

# Start the application
docker-compose up

# Stop the application
docker-compose down
```

The `docker-compose.yml` includes:
- Auto-build from `Dockerfile`
- Environment variable injection
- Port mapping (8080:8080)
- Health check (validates container is running)
- Automatic restart policy

## API Usage

### Endpoint: POST `/api/ai`

**Required Headers:**
- `X-API-Key: <your-app-api-key>` — Application authentication
- `Content-Type: text/plain` or `application/json`

**Request Body:** Game name (string, max 2000 chars)

### curl Examples

```bash
# Plain text (must include X-API-Key header)
curl -X POST http://localhost:8080/api/ai \
  -H "X-API-Key: your-secret-api-key" \
  -H "Content-Type: text/plain" \
  --data "Elden Ring"

# JSON format
curl -X POST http://localhost:8080/api/ai \
  -H "X-API-Key: your-secret-api-key" \
  -H "Content-Type: application/json" \
  --data '"The Legend of Zelda: Tears of the Kingdom"'
```

### Response Format

On success (HTTP 200):
```json
{
  "gameName": "Elden Ring",
  "good": ["Open-world design", "Boss variety", "Challenging gameplay"],
  "bad": ["Occasional frame drops", "Steep learning curve"],
  "score": 95,
  "summary": "A masterpiece of open-world game design."
}
```

On error (HTTP 4xx/5xx):
- Invalid/missing `X-API-Key` → **401 Unauthorized**
- Input validation failure (too long, suspicious patterns) → **400 Bad Request**
- OpenAI integration failure → **503 Service Unavailable** (with fallback response)

## Architecture & Security

### Authentication & Authorization (A07)
- **API Key Filter**: All endpoints require a valid `X-API-Key` header
- **Spring Security**: Stateless session configuration, no CSRF needed
- **Environment Variables**: Keys are never hardcoded; injected via `APP_API_KEY`

### Dependency Security (A06)
- **OWASP Dependency-Check** Maven plugin scans all dependencies on each build
- CI/CD pipeline fails if CVE score ≥ 7 (High/Critical)
- Suppression file: `owasp-suppressions.xml` for false positive management

### Input Validation & Prompt Injection (A03)
- **Length limit**: 2000 characters max
- **Pattern blocking**: Detects known prompt injection phrases
- **System prompt design**: Injected user data goes into *user* role (less privileged)
- **Structural defense**: Explicit JSON schema + low temperature (0.1) reduce hallucinations

### Reliability & Resilience
- **Timeouts**: 2s connect, 8s read (prevents hanging connections)
- **Rate Limiting**: Exponential backoff on HTTP 429 (max 4 attempts)
- **Validation**: Two-layer defense against AI hallucinations
  1. Jackson JSON parsing
  2. Bean Validation constraints (@NotNull, @Min, @Max, @Size)
- **Fallback Response**: Malformed AI output returns safe default

## Project Structure

```
src/main/java/com/example/individuelllabb1k5/
├── IndividuellLabb1k5Application.java      # Spring Boot main class
├── config/
│   └── SecurityConfig.java                 # Spring Security configuration
├── controller/
│   └── AiController.java                   # REST endpoint, input validation
├── dto/
│   └── AiResponseDto.java                  # Bean Validation constraints
└── service/
    └── AiClientService.java                # OpenAI integration, timeouts, retries
```

## Configuration

**application.properties:**
```properties
server.port=8080
openai.api-key=${OPENAI_API_KEY}
```

**Environment Variables:**
```
OPENAI_API_KEY=sk-...              # OpenAI API secret
APP_API_KEY=your-secret-api-key    # Application authentication key
```

**pom.xml highlights:**
- Spring Security (authentication & authorization)
- Jackson (JSON serialization)
- OWASP Dependency-Check Maven plugin (security scanning)
- Lombok (code generation)

## Documentation

- **[security-report.md](security-report.md)** — Detailed security analysis, vulnerability assessments, and remediation strategies
- **[Tillförlitlighetsbedömning.md](Tillförlitlighetsbedömning.md)** — Reliability and robustness engineering (prompting strategy, error handling, LLM limitations)

## Testing

The project includes unit tests for the AI service:
```bash
# Run tests
.\mvnw.cmd test

# Run with coverage
.\mvnw.cmd jacoco:report
```

## Notes

- **Lombok Plugin**: IDE may require the Lombok plugin for annotation processing
- **Spring Boot 4.1**: Latest version as of this documentation; check `pom.xml` for current version
- **OpenAI Model**: Configured as `gpt-4o-mini` with temperature 0.1 (stable, deterministic output)
- **License**: No license specified — add `LICENSE` file if publishing


