# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AINovel is an AI-powered Chinese novel writing platform built with a Spring Boot backend and React/TypeScript frontend. It assists authors through the complete writing process: story conception, outline design, character management, and manuscript writing with AI-generated content.

## Development Setup

### Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- Runs on `http://localhost:8080`
- Requires MySQL 8.x with database named `ainovel`
- Initialize database: `mysql -u root -p ainovel < backend/src/database.sql`
- Configure in `backend/src/main/resources/application-dev.properties`:
  - Database credentials
  - JWT secret
  - OpenAI API key
  - Qdrant vector store settings (localhost:6333 by default)

### Frontend (Vite + React)
```bash
cd frontend
npm install
npm run dev
```
- Runs on `http://localhost:5173`
- Vite proxy forwards `/api` requests to backend on port 8080
- Build: `npm run build` (outputs to `frontend/dist`)
- Lint: `npm run lint`

### Database
- MySQL 8.x required
- Schema in `backend/src/database.sql`
- Dev mode uses `spring.jpa.hibernate.ddl-auto=update` for auto-migration
- For production, manually manage schema changes

## Architecture

### Backend Structure (`backend/src/main/java/com/example/ainovel/`)

**Controller Layer** - REST API endpoints
- `AuthController` - User login/registration with JWT
- `StoryController` - Story card CRUD
- `ConceptionController` - AI story generation
- `OutlineController` - Outline/chapter/scene management
- `ManuscriptController` - Manuscript writing and character change analysis
- `MaterialController` - Material library (upload, search, review)
- `WorldBuildingDefinitionController` - Worldbuilding module definitions
- `WorldController` - World settings management
- `AiController` - AI text refinement and character dialogue generation

**Service Layer** - Business logic
- `OpenAiService` - Direct implementation of AI service (no multi-provider abstraction)
- `ConceptionService` - Story conception generation
- `OutlineService` - Outline and chapter management
- `ManuscriptService` - Manuscript generation and character evolution tracking
- `CharacterDialogueService` - Memory-driven dialogue generation
- `material/MaterialService` - Material CRUD and text chunking
- `material/FileImportService` - File upload parsing (TXT via Apache Tika)
- `material/HybridSearchService` - Multi-route retrieval (Qdrant vectors + MySQL fulltext)
- `material/MaterialVectorService` - Named vector management (semantics/title/keywords)
- `material/DeduplicationService` - Material duplicate detection and merging
- `material/MaterialAuditService` - Audit logging for searches and citations
- `service/security/PermissionService` - ACL permissions for workspace/material access
- `world/` - Worldbuilding module services

**Repository Layer** - JPA data access
- Spring Data JPA repositories for all entities
- Custom queries in repositories like `MaterialRepository` (fulltext search)

**Model Layer** - JPA entities
- `User`, `UserSetting` - User accounts and AI configuration
- `StoryCard`, `CharacterCard` - Story and character definitions
- `OutlineCard`, `OutlineChapter`, `OutlineScene` - Hierarchical outline structure
- `Manuscript`, `ManuscriptSection` - Final manuscript content
- `TemporaryCharacter` - Scene-specific minor characters
- `CharacterChangeLog` - Character evolution tracking across sections
- `material/Material`, `MaterialChunk`, `FileImportJob` - Material library entities
- `audit/SearchLog`, `Citation` - Audit logs for material usage
- `security/Permission` - ACL permission records

**Config Layer**
- `SecurityConfig` - Spring Security + JWT filter configuration
- `WebConfig` - CORS settings (allows `http://localhost:5173` in dev)
- `ChatClientConfig` - Spring AI ChatClient base configuration
- `RestTemplateConfig` - HTTP client setup

**Security**
- `PermissionAspect` - AOP aspect for `@CheckPermission` annotations
- `annotation/` - Custom annotations for permission checks

**DTO Layer** (`dto/`)
- Request/response objects to avoid exposing entities directly
- Material DTOs: `MaterialCreateRequest`, `StructuredMaterial`, `EditorContextDto`, `MaterialReviewItem`, etc.

**Prompt Engineering** (`prompt/`)
- `TemplateEngine` - Manages AI prompt templates
- `context/` - Context builders for different scenarios (story, outline, manuscript)

### Frontend Structure (`frontend/src/`)

**Components** (`components/`)
- `Workbench.tsx` - Main workspace integrating writing, outline, and materials
- `StoryList.tsx`, `StoryConception.tsx` - Story management
- `OutlinePage.tsx`, `OutlineTreeView.tsx` - Outline editing with tree view
- `ManuscriptWriter.tsx` - Main writing interface
- `CharacterStatusSidebar.tsx` - Character evolution tracking sidebar
- `MaterialCreateForm.tsx`, `MaterialUpload.tsx`, `MaterialSearchPanel.tsx` - Material library UI
- `MaterialList.tsx` - Material management with deduplication
- `Can.tsx` - Permission-based component rendering
- `modals/` - Modal components (EditStoryCardModal, EditCharacterCardModal, RelationshipGraphModal, CharacterGrowthPath, GenerateDialogueModal, etc.)

**Pages** (`pages/`)
- `Material/MaterialPage.tsx` - Material library management
- `Material/ReviewDashboard.tsx` - Material review workbench
- `WorldBuilder/WorldBuilderPage.tsx` - Worldbuilding interface

**Services** (`services/`)
- `api.ts` - All backend API calls (axios/fetch wrapped functions)

**Contexts** (`contexts/`)
- `AuthContext.tsx` - Global authentication state (user, token, permissions, workspaceId)

**Hooks** (`hooks/`)
- `useOutlineData.ts` - Outline data fetching and management
- `useStoryData.ts` - Story data fetching and management
- `useAutoSuggestions.ts` - Auto-suggestions for material during writing

**Types** (`types.ts`)
- TypeScript type definitions for all entities and DTOs

## Key Technical Patterns

### AI Service Integration
- Backend uses Spring AI with `ChatClient` for structured output
- Single provider: `OpenAiService` (no multi-provider abstraction)
- Services call `OpenAiService.generateJson()` for structured responses
- User can customize `baseUrl`, `modelName`, and `apiKey` in settings

### Material Library (素材库)
- **Vector Store**: Qdrant integration via Spring AI
  - Named vectors: `semantics`, `title`, `keywords` (3-route vectors)
  - Reciprocal Rank Fusion for multi-vector retrieval
- **Hybrid Search**: `HybridSearchService` combines:
  - Qdrant vector similarity
  - MySQL FULLTEXT search (ngram tokenization)
- **File Import**: Upload `.txt` → Apache Tika extraction → text chunking → vectorization → Qdrant
- **Structured Parsing**: ChatClient outputs `StructuredMaterial` JSON on import
- **Human Review**: Imported materials default to `PENDING_REVIEW` status
- **Auto-suggestions**: Editor monitors content and auto-retrieves relevant materials after 1.5s
- **ACL Permissions**: AOP-based permission checks on Workspace/Material operations
- **Audit Logs**: Async logging of searches (`SearchLog`) and citations (`Citation`)
- **Deduplication**: Backend API for duplicate detection and merging (archives source materials)

### Character Evolution Tracking
- After generating manuscript sections, call `POST /api/v1/manuscripts/{manuscriptId}/sections/analyze-character-changes`
- Backend assembles context (previous character details, scene content, character list) and calls AI for structured `CharacterChangeLog`
- Frontend displays in `CharacterStatusSidebar`: new info, status changes, relationship changes, turning points
- `CharacterGrowthPath` and `RelationshipGraphModal` visualize character development
- `GenerateDialogueModal` uses character memories to generate authentic dialogue via `POST /api/v1/ai/generate-dialogue`

### Authentication Flow
- JWT-based authentication with Spring Security
- Token stored in `localStorage`
- `AuthContext` manages auth state globally
- Auto-login: On app load, validate token via `GET /api/auth/validate`
- `ProtectedRoute` HOC guards authenticated routes

### Frontend-Backend Integration
- Dev: Vite proxy at `http://localhost:5173` forwards `/api` to `http://localhost:8080`
- Prod: Frontend builds to `frontend/dist`, can be served separately or copied to `backend/src/main/resources/static/`
- CORS configured in `WebConfig` to allow `http://localhost:5173` origin in dev

## Common Development Tasks

### Running Tests
```bash
# Backend
cd backend
mvn test

# Frontend
cd frontend
npm run lint
```

### Building for Production
```bash
# Frontend
cd frontend
npm run build
# Output: frontend/dist/

# Backend (creates JAR)
cd backend
mvn package
# Output: backend/target/*.jar
```

### Adding a New Feature (End-to-End)

Example: Adding a "tags" field to StoryCard

**Backend:**
1. Add `tags` field to `model/StoryCard.java` (String type, comma-separated)
2. Update schema: `ALTER TABLE story_cards ADD COLUMN tags VARCHAR(255);`
3. Add `tags` field to relevant DTO in `dto/`
4. Update `StoryController` to handle tags in create/update endpoints
5. Modify `StoryService` to process tags (validation, sanitization)
6. Add tests in `backend/src/test/java/`

**Frontend:**
1. Add `tags?: string` to StoryCard type in `types.ts`
2. Update API functions in `services/api.ts` (e.g., `updateStoryCard`)
3. Update `EditStoryCardModal.tsx` to include tags input field
4. Display tags in `StoryList.tsx` or `OutlineTreeView.tsx`

### Working with Permissions
- Use `@CheckPermission` annotation on controller methods
- Specify resource type (e.g., `MATERIAL`, `WORKSPACE`) and permission level (e.g., `READ`, `WRITE`)
- Frontend uses `Can` component to conditionally render based on user permissions in `AuthContext`

### Material Search Implementation
- **Manual Search**: User triggers search in `MaterialSearchPanel` → calls `HybridSearchService`
- **Auto-suggestions**: `useAutoSuggestions` hook monitors editor content → debounced search after 1.5s
- Both use same backend endpoint with different UI patterns

## Important Notes

- **No Multi-Provider AI**: Despite historical references, current code uses ONLY `OpenAiService`. Any references to multiple providers (Claude, Gemini) are legacy.
- **Material Library**: Latest feature (V2) includes ACL, deduplication, audit logs, and multi-route vector retrieval.
- **Worldbuilding**: Separate module with its own controllers (`WorldController`, `WorldBuildingDefinitionController`) and services.
- **Retry Logic**: Backend uses Spring Retry (`@Retryable`) for LLM calls and database operations to handle transient failures.
- **Async Operations**: Material import jobs run asynchronously; frontend polls status via `FileImportJob` entity.
- **Character Memory**: `CharacterChangeLog` records act as character "memory" for dialogue generation and consistency tracking.
- **Database Schema**: Auto-update in dev (`ddl-auto=update`), manual migrations recommended for production.

## Code Style

### Backend
- **Layered Architecture**: Controller → Service → Repository → Model (strict separation)
- **DTOs**: Always use DTOs in controllers, never expose entities directly
- **Exception Handling**: Use custom exceptions (e.g., `ResourceNotFoundException`) and global exception handlers
- **Security**: JWT validation in `JwtRequestFilter`, Spring Security config in `SecurityConfig`
- **Configuration**: Sensitive config in environment variables, not committed to repo

### Frontend
- **TypeScript**: Explicit types, avoid `any`
- **Component Naming**: PascalCase for components, camelCase for functions/variables
- **API Calls**: Centralized in `services/api.ts`, never inline in components
- **Styling**: Tailwind CSS utility classes + Ant Design components
- **State Management**: React Context for global state (`AuthContext`), hooks for local state

## Troubleshooting

### CORS Issues
Check `backend/src/main/java/com/example/ainovel/config/WebConfig.java` and `SecurityConfig.java` to ensure `http://localhost:5173` is allowed.

### JWT Token Errors
- Token stored in `localStorage` under key typically `token` or `auth_token`
- Validate token endpoint: `GET /api/auth/validate`
- Check `backend/src/main/java/com/example/ainovel/utils/JwtUtil.java` for expiration settings

### Material Search Not Working
- Verify Qdrant is running on `localhost:6333` (or configured host/port)
- Check `OPENAI_API_KEY` environment variable for embedding model
- Ensure MySQL FULLTEXT index exists on material content fields

### Database Migration Issues
- Dev mode auto-updates with `ddl-auto=update`, but may fail on complex changes
- For production, manually write SQL migration scripts
- Reference schema: `backend/src/database.sql`
