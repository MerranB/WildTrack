🦅 WildTrack: AI-Powered Bio-Intelligence Platform

WildTrack is a cloud-native, full-stack application that transforms raw wildlife telemetry data from Movebank.org into actionable conservation insights. By leveraging Java 21 Virtual Threads, PostGIS spatial analysis, and Generative AI, WildTrack provides researchers with a real-time window into global migration patterns.
🎯 The Problem

Researchers are often overwhelmed by "data noise." Movebank hosts millions of data points, but analyzing specific behaviors—like deviations due to weather or entry into protected "Geo-fenced" zones—remains a manual, time-consuming task.
🚀 Key Features (9-Month Vision)

    High-Concurrency Ingestion: Utilizes Java 21's Virtual Threads to fetch and process hundreds of animal tracks simultaneously without I/O blocking.

    Spatial "Geo-Fencing": Real-time alerts using PostGIS when a tracked animal enters a user-defined conservation boundary.

    AI Bio-Assistant: A "Natural Language to SQL" interface (via Spring AI) that allows researchers to ask: "Which birds crossed the Atlantic in under 48 hours?"

    Interactive 3D Mapping: A React-based visualization engine using Mapbox GL to render thousands of points smoothly.

🏗️ System Architecture
Tech Stack

    Backend: Java 25, Spring Boot 4.0, Spring AI

    Database: PostgreSQL + PostGIS (Spatial Data)

    Frontend: React 19, TypeScript, Mapbox GL JS

    Cloud/DevOps: AWS (ECS/RDS), Docker, Terraform, GitHub Actions

📊 Data Schema (The "Golden Path" MVP)

To get the project off the ground, we focus on three core entities:
Entity	Description	Key Fields
Study	The Movebank research group.	study_id, name, species
Animal	The individual being tracked.	animal_id, nickname, taxon
Event	A single GPS ping.	timestamp, geom (Point), altitude
🏁 Minimum Viable Product (MVP) Roadmap

    Phase 1: Build the IngestionService to fetch JSON from Movebank and log it.

    Phase 2: Persist coordinates into PostGIS.

    Phase 3: Create a /api/v1/tracks/{animalId} endpoint.

    Phase 4: Build a React map that calls that endpoint and draws the "Path."

🛠️ Local Development Setup

    Prerequisites: Docker, Java 21+, Node.js 20+.

    Database: docker run --name wildtrack-db -e POSTGRES_PASSWORD=pass -p 5432:5432 -d postgis/postgis

    Backend: ./mvnw spring-boot:run

    Frontend: npm install && npm start
