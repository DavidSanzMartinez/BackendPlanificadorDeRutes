# Backend Planificador de Rutes Ferroviàries

Backend del sistema de planificació de rutes per a la xarxa ferroviària espanyola, desenvolupat com a Treball de Fi de Grau al grau d'Enginyeria Informàtica de la Facultat d'Informàtica de Barcelona (FIB).

Aquest backend és l'API REST que serveix les consultes del frontend (vegeu el [repositori del frontend](https://github.com/DavidSanzMartinez/[NOM-DEL-REPO-FRONTEND])).

## Característiques

- Tres algoritmes de planificació de rutes: Dijkstra, CSA-MEAT i McRAPTOR
- Model probabilístic de fiabilitat calibrat amb dades reals
- Integració amb dades GTFS i GTFS-Realtime de Renfe
- Arquitectura hexagonal + Domain-Driven Design

## Tecnologies

- Java 21
- Spring Boot 4.0.5
- PostgreSQL 16
- Redis 7
- Maven

## Requisits previs

- JDK 21 o superior
- Maven 3.6+
- PostgreSQL 16 i Redis 7 (o Docker Compose per executar-los en local)

## Configuració

1. Clona el repositori:

```bash
    git clone https://github.com/DavidSanzMartinez/BackendPlanificadorDeRutes.git
    cd BackendPlanificadorDeRutes
```

2. Copia el fitxer de configuració d'exemple:

```bash
    cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

3. Edita `src/main/resources/application-local.yml` amb les teves credencials de PostgreSQL, Redis i la clau d'API d'administració.

4. Assegura't que PostgreSQL i Redis estan en execució.

5. Executa el backend:

```bash
    mvn spring-boot:run
```

El backend estarà disponible a `http://localhost:8080`.

## Estructura del projecte

src/main/java/.../planificadorderutes/

├── api/              # Controllers i DTOs

├── application/      # Casos d'ús

├── domain/           # Model i ports

├── infrastructure/   # Adaptadors (persistència, routing, GTFS, etc.)

└── config/           # Configuracions Spring

## Tests

```bash
mvn test
```

## Llicència

Aquest projecte està sota llicència MIT — vegeu [LICENSE](LICENSE) per a més informació.

## Autor

David Sanz Martínez — Treball de Fi de Grau, Facultat d'Informàtica de Barcelona, UPC (2025–2026).

Director: Alex Barceló Cuerda, departament d'Enginyeria de Serveis i Sistemes d'Informació (ESSI).


