# Contextual News Data Retrieval System

A Spring Boot backend that ingests a static news dataset (~2000 articles), exposes REST
endpoints to retrieve/rank articles by category, source, relevance score, text search, and
geographic proximity, and layers a natural-language "smart query" interface on top of the
same retrieval logic. A bonus location-aware trending feed (simulated user events + in-memory
caching) is also included. Every retrieval strategy enriches its results with an LLM-generated
summary per article, computed concurrently rather than one request at a time.

## Tech stack & rationale

| Choice | Why |
|---|---|
| Java 17 / Spring Boot 3.3 | Stated constraint; stable LTS combination. |
| Spring Web + Spring Data JPA | Standard REST + persistence stack, minimal boilerplate. |
| H2 (in-memory) | Zero external setup, resets cleanly between runs, sufficient for a ~2000-row dataset. |
| Caffeine (`spring-boot-starter-cache`) | In-memory cache for the trending feed. Well-known, battle-tested library — cleaner than a hand-rolled `ConcurrentHashMap` + TTL, while still satisfying the "no Redis" constraint. |
| Strategy Pattern (`NewsRetrievalStrategy`) | The 5 retrieval intents (category/source/score/search/nearby) share one interface, so both the direct REST endpoints and the LLM-driven `/query` endpoint reuse identical ranking logic — no duplicated code paths. |
| `LlmService` interface with Mock + OpenAI implementations | The app runs fully offline by default (`LocalLlmService`, keyword/regex-based). A real `OpenAiLlmService` is wired in only when `llm.provider=openai` is set, so grading/demoing never requires an API key. |
| Dedicated `llmExecutor` thread pool (`ThreadPoolTaskExecutor`) | Every strategy summarizes its result set via `CompletableFuture.runAsync(..., llmExecutor)` fanned out in parallel and joined before returning, instead of summarizing articles one HTTP call at a time — this turns an O(N) sequential LLM-latency tax per request into a single parallel batch bounded by the slowest call. |

## Architecture

```
Client
  │
  ├─ GET  /api/v1/news/category|source|score|search|nearby   → NewsController
  ├─ POST /api/v1/news/query                                 → QueryController
  ├─ POST /api/v1/news/events, /events/simulate               → EventController
  └─ GET  /api/v1/news/trending                                → TrendingController
        │
        ▼
  Service layer: NewsQueryService (query), NewsImportService (import),
                 TrendingService (trending)
        │                                          │
        ▼                                          ▼
  NewsStrategyFactory → 5x NewsRetrievalStrategy   LlmService (Local | OpenAI)
  (Category/Source/Score/Search/Nearby)                  understandQuery() / summarizeArticle()
        │                                                       ▲
        │            each strategy fans out summarizeArticle()  │
        │            per article via CompletableFuture.runAsync │
        │            on the shared llmExecutor thread pool, ────┘
        │            then joins all futures before returning
        ▼
  NewsArticleRepository (Spring Data JPA) → H2 (in-memory, DB_CLOSE_DELAY=-1)
        │
  DistanceService (Haversine) — shared by Nearby strategy + Trending proximity

TrendingService → UserArticleEventRepository (H2) + TrendingCacheService (Caffeine, TTL 5 min, bucketed by lat/lon)
```

- Every `NewsRetrievalStrategy` implementation follows the same shape: fetch/rank articles from
  the repository → map to `ArticleResponse` → fan out one `summarizeArticle()` call per article
  as a `CompletableFuture` on the `llmExecutor` bean (`config/AsyncConfig`) → `CompletableFuture
  .allOf(...).join()` before returning, so the LLM summaries for a whole result page are computed
  concurrently rather than serially.
- LLM output is never trusted blindly: an unrecognized `intent` string falls back to `SEARCH`,
  and a `nearby` intent without coordinates fails fast with a `400` rather than silently
  returning an unrelated result set.
- DTOs sit at every API boundary — entities are never serialized directly. `RetrievalCriteria`
  (in `dto/`) is the single shared input contract every strategy consumes, built either from
  direct REST params or from LLM-derived entities.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) produces one consistent error shape for
  validation failures, missing/invalid params, not-found, and generic 500s.

## Running locally

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. The H2 console is available at
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:newsdb`, user `sa`, empty password).

**Data is loaded automatically on startup** — `news_data.json` is imported into H2 before the
app finishes starting, and each inserted article's id/title is logged to the console. No manual
step is required before hitting the retrieval endpoints. To re-import without restarting (e.g.
after wiping rows via the H2 console), you can still call:

```bash
curl -X POST localhost:8080/api/v1/news/import
```

## API reference

All endpoints are under `/api/v1/news`. Successful responses share this envelope:

```json
{
  "metadata": { "intent": "category", "count": 5, "message": "OK" },
  "articles": [ { "id": "...", "title": "...", "llmSummary": "...", "...": "..." } ]
}
```

Errors share this shape:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "..." }
```

| Method | Path | Description | Example |
|---|---|---|---|
| POST | `/import` | Loads `news_data.json` from the classpath into H2, skipping duplicate IDs. | `curl -X POST localhost:8080/api/v1/news/import` |
| GET | `/category` | Articles in a category, newest first. | `curl 'localhost:8080/api/v1/news/category?category=technology&limit=5'` |
| GET | `/source` | Articles from a source, newest first. | `curl 'localhost:8080/api/v1/news/source?source=Reuters&limit=5'` |
| GET | `/score` | Articles at/above a minimum relevance score, highest first. | `curl 'localhost:8080/api/v1/news/score?minScore=0.9&limit=5'` |
| GET | `/search` | Free-text search, ranked by `relevanceScore*0.6 + textMatchScore*0.4`. | `curl 'localhost:8080/api/v1/news/search?query=election&limit=5'` |
| GET | `/nearby` | Articles within `radius` km of a point, closest first. | `curl 'localhost:8080/api/v1/news/nearby?lat=28.6139&lon=77.2090&radius=100&limit=5'` |
| POST | `/query` | Natural-language query → LLM intent detection → same strategies, enriched with an `llmSummary` per article. | see below |
| POST | `/events` | Records a single user interaction event. | see below |
| POST | `/events/simulate?count=` | Generates `count` random VIEW/CLICK events against existing articles. | `curl -X POST 'localhost:8080/api/v1/news/events/simulate?count=500'` |
| GET | `/trending` | Cached, geo+recency+volume weighted trending feed. | `curl 'localhost:8080/api/v1/news/trending?lat=28.6139&lon=77.2090&limit=10'` |

Every list-returning endpoint above — not just `/query` — populates `llmSummary` on each
article, since summarization now happens inside every `NewsRetrievalStrategy`, not only the
smart-query path.

### Smart query example

```bash
curl -X POST localhost:8080/api/v1/news/query \
  -H 'Content-Type: application/json' \
  -d '{"query": "Show me the latest technology news near me", "latitude": 28.6139, "longitude": 77.2090, "limit": 5}'
```

### Record an event

```bash
curl -X POST localhost:8080/api/v1/news/events \
  -H 'Content-Type: application/json' \
  -d '{"userId": "u1", "articleId": "<an-id-from-/category>", "eventType": "CLICK", "latitude": 28.6139, "longitude": 77.2090}'
```

## LLM query understanding

`LlmService` is a plain interface with two implementations, selected via
`llm.provider` (`application.properties`):

- **`LocalLlmService`** (default, `llm.provider=mock`) — zero external dependencies. Uses
  keyword/regex rules to map free text to `{intent, entities, searchQuery}`:
  - `near` / `nearby` / `close to` → `nearby`
  - `from <source>` → `source`, with the matched source name as an entity
  - a known category name (world, national, business, technology, sports, entertainment,
    health, politics, science) present in the text → `category`
  - `top rated` / `highest rated` / `most relevant` / `best` → `score`
  - anything else → `search`, with the original query as `searchQuery`
- **`OpenAiLlmService`** (`llm.provider=openai`, requires `llm.openai.api-key`) — calls the
  OpenAI Chat Completions API with a strict-JSON system prompt matching the same contract. A
  malformed or failed response falls back to a `search` intent rather than propagating an
  error, so the smart query endpoint degrades gracefully.

### Concurrent summarization

Every strategy calls `llmService.summarizeArticle(title, description)` once per article in its
result set, but never sequentially. Each call is submitted as a `CompletableFuture.runAsync(...,
llmExecutor)`, where `llmExecutor` is a dedicated `ThreadPoolTaskExecutor` bean
(`config/AsyncConfig`, core pool 5 / max pool 10 / queue capacity 50) separate from the HTTP
request-handling thread pool. The strategy then blocks once on
`CompletableFuture.allOf(futures).join()` before returning, so total summarization latency for a
page of N articles is bounded by the slowest individual call, not the sum of all N calls.

## Trending feed (bonus)

`TrendingService` computes, per article, the sum over its recent (last 48h) events of:

```
eventScore = weight(eventType) * recencyMultiplier(now - event.createdAt) * proximityMultiplier(distance)
```

- `weight`: `CLICK = 3.0`, `VIEW = 1.0`
- `recencyMultiplier`: `1.0` within the first hour, linearly decaying to `0.0` at 48h
- `proximityMultiplier`: `1.0` at the query point, linearly decaying to `0.1` at 100km+

Results are cached via `TrendingCacheService` (Caffeine), keyed by rounded lat/lon buckets
(`0.5°` grid) + limit, with a 5-minute TTL. Any new event (via `/events` or `/events/simulate`)
invalidates the entire cache so trending numbers never serve stale data after new activity.

## Tests

```bash
./mvnw test
```

- **Unit**: `DistanceServiceTest`, `TextMatchUtilTest`, `LocalLlmServiceTest`,
  `TrendingServiceTest` (verifies the scoring formula favors closer/more-recent events),
  `TrendingCacheServiceTest` (bucketing + invalidation).
- **Integration** (`NewsControllerIntegrationTest`, `@SpringBootTest` + `MockMvc`): import,
  category, search, nearby, smart query, and trending happy paths, plus validation failure
  cases (missing/blank required params).