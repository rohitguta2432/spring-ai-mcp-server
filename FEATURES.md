# StellarMIND: Project Features & Capabilities

StellarMIND is an intelligent agentic system designed for Stellantis vehicle gateways, transforming natural language into actionable insights and operations.

## 🚀 Core Features

### 1. Conversational Text-to-SQL
Turn natural language questions into structured database queries tailored for vehicle gateway data.
*   **Chain of Thought (CoT) Reasoning**: Analyzes user intent and explains reasoning steps before execution.
*   **Safe Read-Only Execution**: Restricted to `SELECT` and `WITH` statements to ensure data integrity.
*   **PostgreSQL + pgvector**: Optimized for high-performance querying and specialized vector searches.

### 2. Model Context Protocol (MCP) Integration
Built on a standardized protocol for AI-to-tool communication.
*   **Data Query Tool (`executeDataQuery`)**: Securely executes generated SQL and returns tabular results.
*   **Device Management Tool (`create_dm_operation`)**: Triggers NexusConnect operations on vehicle gateways.
*   **Extensible Architecture**: Allows easy addition of new capabilities as MCP tools.

### 3. Retrieval-Augmented Generation (RAG)
Uses specialized metadata to ground the AI's understanding in real-world schema.
*   **Schema Context Retrieval**: Uses pgvector to find relevant table fragments based on user prompts.
*   **Redis Metadata Store**: High-speed caching of schema relationship and column definitions.
*   **Reduced Hallucination**: Ensures the AI only queries existing tables and columns.

### 4. Vehicle Gateway Data Model
Native support for complex vehicle schemas.
*   **Pre-defined Schemas**: Support for Vehicle, ECU, Device, and Operations tables.
*   **NexusConnect Integration**: Designed to bridge high-level intent with low-level device management protocols.

### 5. Advanced Web & Observability
*   **Real-time Streaming**: Utilizes Server-Sent Events (SSE) for a responsive, interactive chat experience.
*   **Full Observability**: Integration with Prometheus (metrics) and OpenTelemetry (tracing) for production monitoring.
*   **Spring Boot 3.4+**: Leveraging the latest Spring AI and reactor-based webflux capabilities.

---

## 🛠 Technical Stack
*   **Backend**: Java 17+, Spring Boot 3.4.4, Spring AI
*   **Database**: PostgreSQL 14+ with pgvector
*   **Caching**: Redis (Redisson Client)
*   **Interface**: Thymeleaf & Vanilla CSS (Chat UI)
*   **Deployment**: Docker & Docker Compose
