DOCKER_COMPOSE=docker-compose
PROJECT_NAME=payment-system

SERVICES = individuals-api person-service-api keycloak keycloak-postgres person-service-postgres
INFRA_SERVICES ?= keycloak keycloak-postgres alloy prometheus loki tempo grafana docker-socket-proxy
OBSERVABILITY_SERVICES ?= alloy prometheus loki tempo grafana docker-socket-proxy

.PHONY: all up start stop restart rebuild logs clean ps infra

all: up

up:
	$(DOCKER_COMPOSE) -p $(PROJECT_NAME) up --build -d

start:
	$(DOCKER_COMPOSE) -p $(PROJECT_NAME) up -d

stop:
	$(DOCKER_COMPOSE) -p $(PROJECT_NAME) down

restart: stop up

rebuild:
	$(DOCKER_COMPOSE) -p $(PROJECT_NAME) build --no-cache

logs:
	$(DOCKER_COMPOSE) -p $(PROJECT_NAME) logs -f --tail=200

logs-%:
	$(DOCKER_COMPOSE) -p $(PROJECT_NAME) logs -f --tail=200 $*

ps:
	$(DOCKER_COMPOSE) -p $(PROJECT_NAME) ps

clean:
	$(DOCKER_COMPOSE) -p $(PROJECT_NAME) down -v --remove-orphans
	docker system prune -f

service:
	$(DOCKER_COMPOSE) up -d $(SERVICES)

infra:
	$(DOCKER_COMPOSE) up -d $(INFRA_SERVICES)

observability:
	$(DOCKER_COMPOSE) up -d $(OBSERVABILITY_SERVICES)