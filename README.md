# Distributed AI RAG Engine

Aplicacao distribuida para treino de RAG, agentes, tools, mensageria e observabilidade com Java 21+, Spring Boot, Node.js, TypeScript, PostgreSQL/pgvector, Redis e RabbitMQ.

## Estrutura atual

```txt
ai/            # ai-gateway-node em NestJS, modulo adiado para commits futuros
rag/           # rag-engine-java em Spring Boot
rag-training/  # documentacao tecnica e backlog, modulo adiado para commits futuros
```

## Ambiente local

Copie `.env.example` para `.env` apenas no ambiente local e ajuste valores conforme necessario. Nunca versionar `.env`.

Suba as dependencias principais:

```bash
docker compose up -d postgres redis rabbitmq otel-lgtm
```

Para incluir Ollama localmente:

```bash
docker compose --profile local-llm up -d
```

Endpoints uteis:

```txt
Java health:   GET http://localhost:8080/health
RabbitMQ UI:   http://localhost:15672
Grafana LGTM:  http://localhost:3001
PostgreSQL:    localhost:5432
Redis:         localhost:6379
```

## Testes

Backend Java:

```bash
cd rag
mvn clean verify
```

O gate de cobertura minimo do backend Java e 95% via JaCoCo.

## Principios de desenvolvimento

- TDD: teste falhando antes de codigo de producao.
- Piramide de testes: unitarios primeiro, integracao para adapters, e2e para fluxos criticos.
- Clean Architecture + Hexagonal: dominio e casos de uso independentes de frameworks.
- SOLID, DRY e KISS: pequenas entregas, baixo acoplamento e abstracoes apenas quando pagam o custo.
- Security by design: contexto recuperado e sempre tratado como dado nao confiavel.
- Observabilidade sem vazamento: traces e logs devem evitar prompts completos, segredos e dados sensiveis.
