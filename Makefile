.PHONY: dev test coverage build lint clean infra-up infra-down infra-clean infra-logs check-infra sonar

# Standardized commands
dev:
	docker compose -f docker-compose.dev.yml up -d
	./mvnw quarkus:dev -Dquarkus.http.port=8001 -Dquarkus.http.test-port=8002 -Dquarkus.http.host=0.0.0.0; \
	docker compose -f docker-compose.dev.yml down

check-infra:
	@python3 scripts/check-infra.py

test: check-infra
	./mvnw clean verify -DforkCount=1 -DreuseForks=true -Dnet.bytebuddy.experimental=true

coverage: check-infra
	./mvnw clean verify -DforkCount=1 -DreuseForks=true -Dnet.bytebuddy.experimental=true
	@python3 scripts/check-coverage.py

# Helper / Infrastructure commands
build:
	./mvnw package -DskipTests -Dquarkus.package.jar.type=uber-jar

lint:
	./mvnw compile -Dmaven.compiler.failOnWarning=true

clean:
	./mvnw clean

infra-up:
	docker compose -f docker-compose.dev.yml up -d

infra-down:
	docker compose -f docker-compose.dev.yml down

infra-clean:
	docker compose -f docker-compose.dev.yml down -v

infra-logs:
	docker compose -f docker-compose.dev.yml logs -f

sonar:
	@echo "📡 Subindo Redis (necessário para testes)..."
	@docker ps --format '{{.Names}}' 2>/dev/null | grep -q 'auth_service_java_dev_redis' || \
		docker compose -f docker-compose.dev.yml up -d redis
	@echo "🔍 Rodando scan do SonarQube..."
	./scripts/sonar-scan.sh "auth-service-java" "Auth Service Java"
