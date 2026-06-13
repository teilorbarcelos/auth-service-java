.PHONY: dev test coverage generate storage-driver build lint clean infra-up infra-down infra-logs check-infra metrics-up metrics-stop metrics-down

# Standardized commands
dev:
	docker compose -f docker-compose.dev.yml up -d
	./mvnw quarkus:dev -Dquarkus.http.port=8888 -Dquarkus.http.test-port=8891 -Dquarkus.http.host=0.0.0.0; \
	docker compose -f docker-compose.dev.yml down

check-infra:
	@python3 scripts/check-infra.py

test: check-infra
	./mvnw clean verify -DforkCount=1 -DreuseForks=true -Dnet.bytebuddy.experimental=true

coverage: check-infra
	./mvnw clean verify -DforkCount=1 -DreuseForks=true -Dnet.bytebuddy.experimental=true
	@python3 scripts/check-coverage.py

# Example: make generate name=Product
generate:
	python3 scripts/generate_module.py $(name)

# Example: make storage-driver name=s3
storage-driver:
	@python3 scripts/generate_storage.py $(name)

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

# Métricas (Prometheus & Grafana)
metrics-up:
	@echo "📈 Subindo stack de métricas (Prometheus & Grafana)..."
	docker compose -f docker-compose.metrics.yml up -d

metrics-stop:
	@echo "🛑 Parando stack de métricas..."
	docker compose -f docker-compose.metrics.yml stop

metrics-down:
	@echo "🗑️ Removendo stack de métricas..."
	docker compose -f docker-compose.metrics.yml down
