# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Spring Boot)
```bash
# Clean and package
mvn clean package

# Run tests
mvn test

# Skip tests and build
mvn clean package -DskipTests

# Run application (ensure correct Java version)
mvn spring-boot:run
```

### Frontend (Vue 3)
```bash
# Navigate to frontend directory
cd yu-ai-code-mother-frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Type checking
npm run type-check

# Linting and formatting
npm run lint
npm run format

# Generate TypeScript types from OpenAPI
npm run openapi2ts
```

### Docker
```bash
# Build Docker image
docker build -t yu-ai-code-mother .

# Run with Docker Compose (when available)
docker-compose up -d
```

## Architecture Overview

This is an enterprise-grade AI code generation platform built with Spring Boot 3 + LangChain4j + Vue 3. The system consists of:

### Backend Architecture
- **AI Core Layer**: `src/main/java/com/yupi/yuaicodemother/ai/`
  - AI models and message handling
  - Guardrails for AI safety
  - Tool calling mechanisms for code generation
- **Core Business Logic**: `src/main/java/com/yupi/yuaicodemother/core/`
  - Code generation engines
  - Workflow management
  - Builder patterns for complex objects
- **Web Layer**: `src/main/java/com/yupi/yuaicodemother/controller/`
  - REST API endpoints
  - Request/response handling
- **Data Layer**: `src/main/java/com/yupi/yuaicodemother/mapper/`
  - MyBatis mappers for database operations
- **Cross-cutting Concerns**: `src/main/java/com/yupi/yuaicodemother/`
  - AOP aspects
  - Configuration classes
  - Common utilities and constants

### Frontend Architecture
- **Vue 3 + TypeScript**: Modern reactive frontend
- **Ant Design Vue**: UI component library
- **Pinia**: State management
- **Vue Router**: Client-side routing
- **Vite**: Build tool and dev server

### Key Technologies
- **AI Framework**: LangChain4j for AI integration
- **Workflow**: LangGraph4j for AI agent workflows
- **Caching**: Redis + Caffeine multi-level caching
- **Database**: MySQL with MyBatis
- **Monitoring**: Prometheus + Grafana configuration included
- **Deployment**: Docker containerization

### Core Business Capabilities
1. **Smart Code Generation**: AI analyzes requirements and generates complete applications
2. **Visual Editing**: Real-time.preview with AI-assisted modifications
3. **One-click Deployment**: Automatic deployment to cloud with screenshot generation
4. **Enterprise Management**: Admin dashboard for user/app management and monitoring

### Development Notes
- The project uses Maven wrapper (`mvnw`) for consistent builds
- Frontend is in separate `yu-ai-code-mother-frontend` directory
- Database scripts are in `sql/` directory
- Monitoring configs: `prometheus.yml` and `grafana/` directory
- The system excludes Redis embedding store auto-configuration in main application class

### Important File Locations
- Main Application: `src/main/java/com/yupi/yuaicodemother/YuaiCodeMotherApplication.java`
- Frontend Entry: `yu-ai-code-mother-frontend/src/main.ts`
- Database Config: Check `src/main/resources/application.yml` for datasource settings
- AI Model Configurations: Look in `src/main/java/com/yupi/yuaicodemother/ai/model/`
- Tool Definitions: `src/main/java/com/yupi/yuaicodemother/ai/tools/`