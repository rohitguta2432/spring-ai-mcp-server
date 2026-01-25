# PR: Project Rebranding and Test Suite Addition

## Summary
This PR implements the project-wide rebranding from **LwM2M** to **NexusConnect** and adds a comprehensive Postman/Newman test suite for the core APIs. It also includes general stabilization fixes for Redis and service startup.

## Key Changes
- **NexusConnect Rebranding**:
  - Replaced all user-facing mentions of "LwM2M" with "**NexusConnect**" in documentation (`README.md`, `FEATURES.md`), configuration (`pom.xml`, `application.yml`), and source code logs/comments.
  - *Note*: Package names were preserved for stability.
- **Documentation**:
  - Created `FEATURES.md` to document the core capabilities of StellarMIND.
- **Testing Suite**:
  - Created a dedicated `postman/` directory with a collection testing multiple scenarios for `/api/v3/cot-chat/stream`.
  - Added `run_tests.sh` to facilitate automated Newman testing.
- **Fixes**:
  - Resolved `ECONNREFUSED` issues for Redis by starting a local daemon.

## Verification
- **Build**: `mvn clean compile` successful.
- **Service Connectivity**: Verified Redis connection via `lsof` and `redis-cli client list`.
- **API Visibility**: Verified standard endpoints and MCP tool availability.
