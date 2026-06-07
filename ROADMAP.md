# WildTrack Roadmap

## Completed

| Phase | Feature |
|-------|---------|
| 1 | Backend infrastructure, CRUD API, three-layer test pyramid |
| 2 | Movebank ingestion pipeline using Java Virtual Threads |
| 3 | PostGIS spatial queries and geo-fence alert system |
| 4 | Claude AI natural language query interface |
| 5 | AWS deployment — ECS Fargate, RDS, ALB, Secrets Manager, Terraform, GitHub Actions CI/CD |
| 6 | React + TypeScript frontend with interactive Mapbox map, spatial query UI, and geo-fence draw tool |

---

## Phase 7 — AI Enhancements

Expand the existing Claude Haiku 4.5 natural language query interface with production-grade optimizations.

- Anthropic prompt caching on the system prompt — reduces input token cost by ~70% at scale since the system prompt is identical on every call
- Conversation memory — allow follow-up queries that retain context from the previous exchange (e.g. "now show me just 2015")
- Natural language result summarization — after returning results, generate a one-sentence summary of what was found

---

## Phase 8 — Spring Security

Add authentication and authorization to protect write endpoints.

- JWT-based authentication replacing the current API-key interceptor approach
- User registration and login endpoints
- Role-based access control — read endpoints remain public, write endpoints require authentication
- Geo-fences scoped to the owning user — users can only update or delete their own fences
- Spring Security integration with the existing GlobalExceptionHandler for standardized 401/403 responses

---

## Phase 9 — Live Data Ingestion

Replace the historical dataset with a live connection to Movebank, removing the need for the demo workaround.

- Support for multiple Movebank study IDs — researchers can register their own datasets via the API
- Incremental ingestion — fetch only records newer than the last ingestion timestamp instead of full dataset re-download
- Re-enable the geo-fence scheduler in production now that live movement events exist
- Remove the `DemoController` and its simulated data once the scheduler runs against real data
- Webhook or polling support for near-real-time updates from Movebank

---

## Backlog

| Item | Description |
|------|-------------|
| Async `updateDatabase` | `POST /api/v1/events/updateDatabase` currently runs synchronously and can exceed the ALB's 60s timeout on large datasets. Return a job ID immediately and process ingestion in the background. |
| Geo-fence cleanup job | Recruiters can create geo-fences via the API, which accumulate over time and increase the nightly scheduler's workload. Add a scheduled job to purge geo-fences older than a configurable threshold. |

---

## Notes

The geo-fence scheduled alert checker is currently disabled in the production environment because the ingested dataset (Magnificent Frigatebird GPS, BVI, 2014–2016) is historical and produces no new movement events. Phase 9 resolves this permanently. In the meantime, the `POST /api/v1/demo` endpoint demonstrates the full production alert pipeline end-to-end.
