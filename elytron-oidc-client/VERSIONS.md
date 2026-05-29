# Elytron OIDC Client Subsystem Version History

This document tracks the relationship between WildFly releases, management model versions, and schema versions for the elytron-oidc-client subsystem.

## Version History

| WildFly Version | Model Version | Schema Version(s) Introduced | Used in EAP? |
|-----------------|---------------|------------------------------|--------------|
| 28.0.0.Beta1    | 1.0.0         | 1.0 (DEFAULT)                |              |
| 29.0.0.Alpha1   | 2.0.0         | 2.0 (DEFAULT)                | EAP 8.0      |
| 32.0.0.Beta1    | 3.0.0         | 2.0 (PREVIEW)                |              |
| 33.0.0.Beta1    | 4.0.0         | 3.0 (PREVIEW)                | EAP 8.1      |
| 40.0.0.Beta1    | 5.0.0         | 4.0 (PREVIEW)                |              |

## Notes

- **Model Version**: Refers to the management model version defined in `ElytronOidcClientSubsystemModel`
- **Schema Version**: Refers to the XML schema version defined in `ElytronOidcSubsystemSchema`
- **Stability Levels**:
  - `DEFAULT`: Standard stability level for production use
  - `PREVIEW`: Preview stability level for features under development
- Prior to WildFly 32, schema versions were implemented as separate parser classes (`ElytronOidcSubsystemParser_1_0`, `ElytronOidcSubsystemParser_2_0`)
- Starting with WildFly 32, schema versions are defined as an enum in `ElytronOidcSubsystemSchema`
- Each new schema version (including PREVIEW variants) typically corresponds to a model version bump