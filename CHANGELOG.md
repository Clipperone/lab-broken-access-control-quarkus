# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Guida formativa `SECURITY-TESTING-GUIDE.md`: anatomia di un security unit test di autorizzazione, ponte SAST/DAST → unit test, catalogo pattern/anti-pattern, learning path junior/senior, matrice di copertura.
- Scenario **Function Level Access Control & verb tampering**: nuova classe `DocResourceFunctionLevelTest` (escalation verticale sulla creazione, verb tampering → 405).
- Scenario **Mass assignment & Field-Level Authorization**: nuovo endpoint `PUT /doc/person/edit/{uuid}` con `EditPersonRequestDTO`, nuova classe `DocResourceFieldLevelTest` (campi server-controlled non sovrascrivibili, `minRole` modificabile solo da 'admin').
- Tassonomia tag estesa con `object-level`, `function-level`, `field-level`, verificati dal gate `junit5-tag-check`.
- Deny-by-default: `quarkus.security.jaxrs.deny-unannotated-endpoints=true` (mitigazione strutturale della classe "Missing Function Level Access Control").

### Changed

- Endpoint demo JWT (`/demo/{roles}.txt`): `@PermitAll` esplicito e disattivazione automatica nel profilo `prod` (`@UnlessBuildProfile("prod")`).
- Rinominato `CONTRIBUITING.md` → `CONTRIBUTING.md`; rimosso il file `dependabot.yml` duplicato (mantenuto `.github/dependabot.yml`).
- `JUNIT-TAG.md` e `JUNIT-TEST.md` aggiornati e corretti.

## [2.0.0] - 2026-02-26

### Added

- Aggiunto campo UUID all'entità persona da usare al posto dell' ID numerico sequenziale (Breaking change)

### Fixed 

- IDOR, Insecure Direct Object Reference sull'ID delle persone

## [1.0.1] - 2026-02-17

### Fixed

- VULNERABILITY (x)
- VULNERABILITY (5)
- VULNERABILITY (4)
- VULNERABILITY (3)
- VULNERABILITY (2)
- VULNERABILITY (1)

## [1.0.0] - 2026-02-16

### Added

- versione vulnerabile del progetto, basato su Quarkus 3.31.3, con vulnerabilità broken access control presenti.
