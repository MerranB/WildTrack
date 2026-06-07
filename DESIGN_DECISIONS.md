# Design Decisions

## Rate Limiting — Bucket4j

Bucket4j was chosen over the two obvious alternatives:

- **AWS WAF** — overkill and costly for a single-instance portfolio project. WAF is designed for enterprise-scale DDoS mitigation, not simple per-IP request caps.
- **Resilience4j** — built for outbound call protection (circuit breaking, retries). It doesn't fit the use case of limiting inbound requests from clients.

Bucket4j is lightweight, requires no external infrastructure, and implements the token bucket algorithm which naturally handles burst traffic.

## In-Memory Rate Limiting

The rate limiter stores buckets in a `ConcurrentHashMap` with no eviction. This is acceptable because:

- The application runs as a single ECS task — there is no distributed state to synchronize across instances.
- The expected number of unique visitors is small enough that the map will never grow to a problematic size.

If the application ever scales to multiple instances, the buckets would need to move to a distributed store such as Redis.

## Rolling 24-Hour Window

Rate limits reset on a rolling 24-hour window rather than at midnight. A midnight reset would allow a user to exhaust their limit at 11:59 PM and immediately get a full reset one minute later. The rolling window ensures a consistent experience regardless of time of day.

## @Transactional(readOnly = true) at Class Level

The service classes are annotated with `@Transactional(readOnly = true)` at the class level, with individual write methods overriding to `@Transactional`. This minimizes database lock contention — read-only transactions allow the database to skip acquiring write locks and can use read replicas where available. Write methods explicitly opt in to a full transaction only when needed.

## ReportingPolicy.IGNORE in Mappers

MapStruct mappers use `ReportingPolicy.IGNORE` to suppress warnings about unmapped fields. DTOs intentionally expose a subset of entity fields — not every field on an entity belongs in the API response. Without this policy, MapStruct would emit compile-time warnings for every unmapped field, which would be noisy and misleading since the omissions are deliberate.

## NullValuePropertyMappingStrategy.IGNORE in Update Mapper

The update mapper uses `NullValuePropertyMappingStrategy.IGNORE` so that null fields in an incoming DTO do not overwrite existing values on the entity. This supports partial updates — a caller can send only the fields they want to change without having to send the full object. Fields absent from the request are left untouched.

## Virtual Threads for Ingestion

The Movebank ingestion pipeline uses Java Virtual Threads (`spring.threads.virtual.enabled=true`) rather than a fixed thread pool. CSV parsing and database writes are I/O-bound operations. A fixed pool would block platform threads during I/O, limiting throughput under large batch counts. Virtual threads are cheap to create and park during I/O, so the pipeline can process many records concurrently without exhausting the thread pool.

## PARTIAL_SUCCESS vs FAILURE Threshold (80%)

The `updateDatabase` method returns `FULL_SUCCESS`, `PARTIAL_SUCCESS`, or `FAILURE` based on what percentage of records were saved successfully:

- Below 80% success → `FAILURE`
- 80% or above → `PARTIAL_SUCCESS`

The 80% threshold reflects the assumption that isolated record failures (malformed data, duplicate detection) are expected and acceptable. If more than 20% of records fail, it likely indicates a systemic issue — a schema mismatch, API contract change, or data quality regression — and the result should be treated as a failure rather than a partial success.

## 3-Day Geo-fence Alert Cooldown

The geo-fence alert service enforces a 3-day cooldown between alerts for the same fence. The scheduler runs nightly, so without a cooldown an animal that remains inside a fence would trigger an alert every night. Three days was chosen as a balance — long enough to avoid alert fatigue, short enough to still catch meaningful movement changes.

## @ConditionalOnProperty Instead of @Profile for Schedulers

Schedulers are toggled via `@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true")` rather than `@Profile`. Profiles are coarse-grained — switching profiles changes many things at once. A property flag allows the scheduler to be enabled or disabled independently of the active profile, which is useful for disabling scheduled jobs in production when the dataset is historical without changing any other prod configuration.

## Test Method Naming Convention

Test methods use `snake_case` (e.g. `resource_not_found_exception_returns404`) rather than the Java-standard `camelCase`. Snake_case improves readability of test names in CI output and test reports — long descriptive names like `checkGeoFences_doesNotSendEmail_whenCooldownNotExpired` are easier to scan at a glance when underscores clearly separate the method, condition, and expected outcome. The choice is intentional and consistent across all test classes.
