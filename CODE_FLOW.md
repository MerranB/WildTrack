# WildTrack Code Flow

Traces every API call through the class layers. Use this to understand what code runs when an HTTP request comes in.

**Layer order:** Controller → Service → Repository / External API → Response

---

## MovebankController  `BASE: /api/v1/events`

```mermaid
flowchart TD

    subgraph getAll["GET /events"]
        A1(["Request"]) --> A2["MovebankEventService.findAll"]
        A2 --> A3[("MovebankEventRepository.findAll")]
        A3 --> A4["MovebankEventMapper.toDto per record"]
        A4 --> A5(["Page of MovebankEventDto"])
    end

    subgraph getById["GET /events/{id}"]
        B1(["Request"]) --> B2["MovebankEventService.findById"]
        B2 --> B3[("MovebankEventRepository.findById")]
        B3 --> B4{"Found?"}
        B4 -->|Yes| B5["MovebankEventMapper.toDto"]
        B4 -->|No| B6(["404 ResourceNotFoundException"])
        B5 --> B7(["MovebankEventDto"])
    end

    subgraph updateDb["POST /events/updateDatabase"]
        C1(["Request"]) --> C2["MovebankEventService.updateDatabase"]
        C2 --> C3["MovebankClient.getData — study ID 19186107"]
        C3 --> C4(["Movebank External API"])
        C4 --> C5["Parse CSV with OpenCSV"]
        C5 --> C6["Check null fields — warn log per record"]
        C6 --> C7["Split into 20 batches — Java Virtual Threads"]
        C7 --> C8{"Duplicate? existsByTimestamp\nAndLocationAndIndividualId..."}
        C8 -->|New record| C9[("MovebankEventRepository.save")]
        C8 -->|Already exists| C10["Skip — increment dup counter"]
        C9 & C10 --> C11{"Batch result"}
        C11 -->|0 failures| C12(["200 FULL_SUCCESS"])
        C11 -->|80% or more saved| C13(["207 PARTIAL_SUCCESS"])
        C11 -->|Under 80% saved| C14(["500 FAILURE"])
    end

    subgraph byBox["GET /events/allDataPointsByBox"]
        D1(["Request: minLat, maxLat, minLon, maxLon"]) --> D2["MovebankEventService.allDataPointsByBox"]
        D2 --> D3[("PostGIS: ST_Within + ST_MakeEnvelope")]
        D3 --> D4["MovebankEventMapper.toDto per record"]
        D4 --> D5(["Page of MovebankEventDto"])
    end

    subgraph byRange["GET /events/allDataPointsByRange"]
        E1(["Request: lat, lon, range"]) --> E2["MovebankEventService.allDataPointsByRange"]
        E2 --> E3[("PostGIS: ST_DWithin")]
        E3 --> E4["MovebankEventMapper.toDto per record"]
        E4 --> E5(["Page of MovebankEventDto"])
    end
```

---

## GeoFenceController  `BASE: /api/v1/geoFence`

```mermaid
flowchart TD

    subgraph gfGetAll["GET /geoFence"]
        A1(["Request"]) --> A2["GeoFenceService.findAll"]
        A2 --> A3[("GeoFenceRepository.findAll")]
        A3 --> A4["GeoFenceMapper.toDto per record"]
        A4 --> A5(["Page of GeoFenceDto"])
    end

    subgraph gfGetById["GET /geoFence/{id}"]
        B1(["Request"]) --> B2["GeoFenceService.findById"]
        B2 --> B3[("GeoFenceRepository.findById")]
        B3 --> B4{"Found?"}
        B4 -->|Yes| B5["GeoFenceMapper.toDto"]
        B4 -->|No| B6(["404 ResourceNotFoundException"])
        B5 --> B7(["GeoFenceDto"])
    end

    subgraph gfCreate["POST /geoFence"]
        C1(["Request: GeoFenceDto"]) --> C2["SanitizationUtils.sanitizeString — name + username"]
        C2 --> C3["GeoFenceService.create"]
        C3 --> C4["GeoFenceMapper.toEntity — coordinate list converted to PostGIS Polygon"]
        C4 --> C5[("GeoFenceRepository.save")]
        C5 --> C6["GeoFenceMapper.toDto"]
        C6 --> C7(["201 Created — GeoFenceDto"])
    end

    subgraph gfUpdate["PUT /geoFence/{id}"]
        D1(["Request: id + GeoFenceDto"]) --> D2["SanitizationUtils.sanitizeString — name + username"]
        D2 --> D3["GeoFenceService.update"]
        D3 --> D4[("GeoFenceRepository.findById")]
        D4 --> D5{"Found?"}
        D5 -->|Yes| D6["GeoFenceMapper.updateEntityFromDto"]
        D5 -->|No| D7(["404 ResourceNotFoundException"])
        D6 --> D8(["200 OK — GeoFenceDto"])
    end

    subgraph gfDelete["DELETE /geoFence/{id}"]
        E1(["Request: id"]) --> E2["GeoFenceService.delete"]
        E2 --> E3[("GeoFenceRepository.existsById")]
        E3 --> E4{"Found?"}
        E4 -->|Yes| E5[("GeoFenceRepository.deleteById")]
        E4 -->|No| E6(["404 ResourceNotFoundException"])
        E5 --> E7(["204 No Content"])
    end
```

---

## NaturalLanguageQueryController  `BASE: /api/v1/analysis`

```mermaid
flowchart TD
    A(["GET /analysis/query — userPrompt string"]) --> B["SanitizationUtils.sanitizeUserPrompt\nStrips HTML, limits to 500 chars"]
    B --> C["Build Prompt: SystemMessage with JSON schema rules + UserMessage"]
    C --> D["Spring AI ChatModel.call"]
    D --> E(["Claude Haiku 4.5 — Anthropic External API"])
    E --> F["Extract JSON block from response text"]
    F --> G{"Response valid JSON?"}
    G -->|No JSON found| H(["500 NaturalLanguageQueryException"])
    G -->|Contains error field| I(["422 NaturalLanguageQueryException"])
    G -->|Valid| J["Jackson — deserialize to SpatialQueryParams\nlat, lon, range, startDate, endDate"]
    J --> K{"Date parameters\nprovided?"}
    K -->|Both dates| L[("PostGIS: ST_DWithin + date range filter")]
    K -->|Start date only| M[("PostGIS: ST_DWithin + startDate to 2016-12-31")]
    K -->|End date only| N[("PostGIS: ST_DWithin + 2014-01-01 to endDate")]
    K -->|No dates| O[("PostGIS: ST_DWithin — no date filter")]
    L & M & N & O --> P["MovebankEventMapper.toDto per record"]
    P --> Q(["Page of MovebankEventDto"])
```

---

## DemoController  `BASE: /api/v1/demo`

```mermaid
flowchart TD
    A(["POST /demo — GeoFenceDto"]) --> B["DemoService.testGeoFenceDemo"]
    B --> C["GeoFenceService.create — persist fence to DB"]
    C --> D["Set lastAlertSent to 4 days ago\nBypasses the 3-day alert cooldown"]
    D --> E["GeoFenceService.update — save the backdated timestamp"]
    E --> F["Calculate centroid of fence coordinates\nAverage of all lat/lon values"]
    F --> G[("MovebankEventRepository.save\nInsert simulated animal event at centroid")]
    G --> H["GeoFenceScheduler.geoFenceDemo\nSchedule production scheduler to run in 2 minutes"]
    H --> I(["200 OK — email will arrive in ~2 minutes"])

    H -.->|"2 minutes later"| J

    subgraph async ["GeoFenceScheduler — production code path"]
        J["Load geo-fence from DB"] --> K[("PostGIS: ST_Within\nCount distinct animals inside fence boundary\nDISTINCT ON individual_id — latest position per animal")]
        K --> L{"Animal count changed\nfrom lastAnimalCount?"}
        L -->|No change| M(["Do nothing"])
        L -->|Changed| N{"Last alert sent\nmore than 3 days ago?"}
        N -->|No — cooldown active| O(["Do nothing"])
        N -->|Yes| P["Spring Mail — send alert email"]
        P --> Q[("Update lastAnimalCount + lastAlertSent")]
        Q --> R[("Cleanup — delete demo fence + demo animal event")]
    end
```

---

## Background Schedulers

These run independently of HTTP requests on a nightly cron schedule. Disabled in production since the dataset (2014–2016) is historical and produces no new events.

| Scheduler | Trigger | What it does |
|-----------|---------|--------------|
| `UpdateDatabaseScheduler` | Nightly cron | Calls `MovebankEventService.updateDatabase()` — identical flow to `POST /events/updateDatabase` above |
| `GeoFenceScheduler` | Nightly cron | Checks all geo-fences via PostGIS, fires email alerts on count changes — the same scheduler the demo endpoint triggers manually |
