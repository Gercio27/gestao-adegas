# Gestão de Adegas — ACV Vinhos de Talha

Aplicação web (Java / Spring Boot + Thymeleaf + MySQL) para gestão de pequenas adegas,
cobrindo o ciclo "da vinha até à rotulagem / produto acabado".

Princípio: **registar logo quando acontece, uma só vez, toda a informação.**

## Estado atual (MVP — Fase 1: Fichas)

Já implementado:

- Autenticação e perfis (**Administrador** / **Operador**).
- Código automático do sistema em todas as fichas (ex.: `CAS-000001`).
- Fichas: **Castas** (com lista nacional inicial), **Vinhas** (com parcelas, casta e área +
  resumo por casta e área total), **Trabalhadores**, **Adegas**, **Talhas**,
  **Depósitos/Cubas** (com capacidade, volume e próprio/terceiros).
- **Listagens por datas** das fichas.

Próximas fases: restantes fichas (equipamentos, consumíveis, produtos) e os **Processos por fases**
(vindima, moagem, fermentação, engarrafamento, rotulagem, comercial) com controlo de capacidade,
rastreabilidade, notas de entrada/saída, DA, etiquetas e certificação.

## Requisitos

- **Java 17+ (JDK)**
- **Maven 3.9+**
- (Opcional para produção) **MySQL/MariaDB**

## Como arrancar

Por omissão a aplicação usa **MySQL** (dados persistentes). Login inicial:
**utilizador `admin`, palavra-passe `admin123`** (alterável em `application.yml`).

### 1. Instalar e preparar o MySQL (uma vez)

Instalar o servidor MySQL (ex.: no Windows, com winget):
```
winget install Oracle.MySQL
```
Depois criar a base de dados e o utilizador que a aplicação usa:
```sql
CREATE DATABASE IF NOT EXISTS gestao_adegas CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'adega'@'localhost' IDENTIFIED BY 'adega';
GRANT ALL PRIVILEGES ON gestao_adegas.* TO 'adega'@'localhost';
FLUSH PRIVILEGES;
```
> O esquema (tabelas) é criado automaticamente no primeiro arranque
> (`ddl-auto=update`). Utilizador/senha da BD configuram-se em
> `src/main/resources/application.yml` (perfil `mysql`).

### 2. Arrancar
```
mvn spring-boot:run
```
Abrir <http://localhost:8080>.

### Modo demonstração (sem instalar nada) — base H2 em memória
Útil para experimentar rapidamente; os dados são apagados a cada reinício:
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Consola H2: <http://localhost:8080/h2-console>

## Estrutura

```
src/main/java/pt/acv/adega
├── AdegaApplication.java        # arranque
├── common/                      # BaseEntity + geração de código automático
├── security/                    # utilizadores, perfis, login
├── fichas/                      # entidades/controladores das fichas (variáveis)
├── web/                         # dashboard e listagens
└── config/                      # dados iniciais (admin + castas)
src/main/resources/templates/    # páginas Thymeleaf
```
