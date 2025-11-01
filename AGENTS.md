# Repository Guidelines

## Project Structure & Module Organization
- `backend/` contains the Spring Boot app in `src/main/java/com/example/ainovel` with controller/service/repository/model layers; keep material- and world-specific logic in their subpackages.
- Configuration and prompt assets sit in `backend/src/main/resources` (`application.yml`, `application-dev.yml`, `prompts/`, `worldbuilding/`). Add new profiles or templates alongside the existing structure.
- `frontend/` hosts the Vite + React client: shared UI in `src/components`, routed screens in `src/pages`, hooks in `src/hooks`, and HTTP helpers in `src/services/api.ts`; keep `doc/` updated when structures change.

## Build, Test, and Development Commands
- `cd backend && mvn spring-boot:run` starts the API on `http://localhost:8080`; export `OPENAI_API_KEY`, `QDRANT_HOST`, `QDRANT_PORT`, and database credentials beforehand.
- `mvn test` runs the JUnit 5 suite; run it before every push to guard retry, ACL, and repository logic.
- `cd frontend && npm install` (first run), `npm run dev` for the dev server, `npm run build` for type-check + production bundles, `npm run lint` for ESLint, and `npm run preview` to inspect built assets.

## Coding Style & Naming Conventions
- Java uses 4-space indentation, PascalCase types, camelCase members, and the `com.example.ainovel` package prefix; keep controllers thin, move orchestration into services, and expose DTOs rather than entities.
- TypeScript uses 2-space indentation; components/pages are PascalCase, hooks start with `use`, shared types live in `src/types.ts`, and Tailwind utility classes handle styling.
- Run `npm run lint` before committing and rely on IDE auto-formatting for Java imports; avoid adding generated artifacts or temporary `tmp_*.tsx` files to git.

## Testing Guidelines
- Place backend tests in `backend/src/test/java/com/example/ainovel/...`, mirroring production packages. Prefer slice tests (`@DataJpaTest`, `@WebMvcTest`) and reserve `@SpringBootTest` for end-to-end flows.
- Target >70% line coverage on new backend work, covering happy paths, permission denials, and retry fallbacks.
- Frontend automation is pending; adopt Vitest + React Testing Library (`*.test.tsx` beside the component), document manual checks in PRs, and keep `npm run lint` clean until tests land.

## Commit & Pull Request Guidelines
- Keep commit subjects concise (<60 chars), present tense, and optionally prefixed `feat|fix|docs`; history already mixes Chinese summaries (`素材库问题修复`) with conventional commits.
- Group related changes per commit, squash noisy WIP, and reference tickets or issue IDs when possible.
- PRs need a short summary, UI/API evidence, configuration notes, and the commands you ran (`mvn test`, `npm run lint`). Flag cross-stack work so backend and frontend reviewers can engage early.
