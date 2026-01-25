# StellarMIND

**Chat-to-SQL with pgvector.**  
A Spring Boot MCP server (StellarMIND) that turns natural-language questions into **read-only SQL**, retrieves schema context via **pgvector**, and returns results.

---

## Overview
- **Natural language → SQL** (LLM via Spring AI; provider-agnostic)
- **Retrieval-augmented** using `gtw.knowledge_chunks` (pgvector)
- **Supports both static embeddings & dynamic schema retrieval**
- **Read-only queries only** (`SELECT`, `WITH`)
- **JDBC execution** with structured logging

**Architecture Flow:**  
**User → MCP Client → MCP Server (MCP Tool: `executeDataQuery`) → pgvector context → LLM → Safe SQL → DB results**


---

## Prerequisites
- **JDK 17+** (Recommended: OpenJDK 21)
- **Docker & Docker Compose**
- **Maven 3.8+**
- **Redis Server** (Default port: 6389 as per client config)
- **OpenAI API Key** (Required for embeddings and reasoning)

---

## Getting Started

### 1. Environment Configuration
Set your OpenAI API key in your terminal:
```bash
export OPENAI_API_KEY="your_api_key_here"
```

### 2. Start Infrastructure (PostgreSQL & Vector Store)
Use Docker Compose to spin up the database with the `pgvector` extension:
```bash
# From the project root
docker compose up -d
```
> [!NOTE]
> Ensure your local Redis server is running on port `6389` or update `stellarmind-client/src/main/resources/application.yml`.

### 3. Run the Backend Services
Open two terminal windows to run the MCP Server and Client:

**Terminal 1: NexusConnect MCP Server**
```bash
cd stellarmind-server
./mvnw spring-boot:run
```
*The server will start on port `8082`.*

**Terminal 2: StellarMIND Client (UI)**
```bash
cd stellarmind-client
./mvnw spring-boot:run
```
*The client will start on port `8084`.*

---

## 🖥 Web Interface
Once both services are running, access the Chain of Thought (CoT) chat interface at:
**[http://localhost:8084/cot](http://localhost:8084/cot)**

---

## 🧪 Testing & Verification
A dedicated Postman collection is provided for automated testing of the streaming API.

```bash
cd postman
./run_tests.sh
```
This script uses **Newman** (via `npx`) to verify 4 core scenarios: Normal queries, DB commands, Error handling, and Context persistence.

---

## Project Structure
- `stellarmind-server`: The MCP server exposing database and vehicle operation tools.
- `stellarmind-client`: The Spring AI client and web interface.
- `postman/`: API test collection and automation scripts.
- `FEATURES.md`: Full functional breakdown of the project capabilities.
