# Subsystem development and review

Use this guide when a change affects a WildFly subsystem's management model, XML, or runtime configuration. Subsystems use different extension and persistence APIs; follow the affected subsystem's existing pattern. For code review, use the concise [subsystem review checklist](subsystem-review-checklist.md) alongside this guide. The [Extending WildFly documentation](../docs/src/main/asciidoc/Extending_WildFly.adoc) provides broader background.

## Choose the implementation route

Read the guide that matches the affected code, then use the shared checklist below. These are common routes, not exclusive generations: an extension may combine a newer registration API with an older XML API. Match the actual registered extension, parser, writer, and transformer rather than assuming one class determines all four.

| Route | Recognize it by | Guide |
| --- | --- | --- |
| `SubsystemExtension` and resource registrars | `SubsystemConfiguration`, `SubsystemPersistence`, `ManagementResourceRegistrar` | [Registrar-based subsystems](subsystem-pattern-new.md) |
| Direct `Extension` with a schema enum | `initialize`/`initializeParsers`, `PersistentSubsystemSchema` | [Schema-driven controller subsystems](subsystem-pattern-intermediate.md) |
| Direct `Extension` with versioned parser classes | `setSubsystemXmlMapping` per namespace, `PersistentResourceXMLParser` or manual StAX | [Versioned-parser subsystems](subsystem-pattern-legacy.md) |

## Find the contract

1. Locate the module from the subsystem name or configuration namespace. Search `rg -n -F -e 'registerSubsystem(' -e 'SubsystemRegistration.of(' -e 'setSubsystemXmlMapping' -e 'SubsystemPersistence.of' --glob '*.java'` and inspect its `pom.xml` and `src/main/` tree.
2. From its extension, trace the registered root and child resource definitions, attribute definitions, add/remove and write handlers, operations, capabilities, and runtime services. Look for `registerAttributes`, `registerChildren`, `registerOperations`, `ResourceDescriptor`, and `ManagementResourceRegistrar`. Confirm which values are stored in the model and which are runtime only.
3. Locate the registered XML reader and writer, the current namespace or schema enum, any `src/main/resources/schema/*.xsd`, and test XML. Some subsystems use `SubsystemResourceXMLSchema`, some use `PersistentSubsystemSchema` or `PersistentResourceXMLParser`, and some have hand-written versioned parsers. Inspect actual registration before editing; a class name alone does not prove which parser or writer is active.
4. Locate `ExtensionTransformerRegistration` or a subsystem transformer factory, including service registration, and search `src/test/` and `testsuite/` for the subsystem name, management addresses, and namespace. Follow the module's existing test harness and version targets.

## Management change checklist

For each new or changed attribute, child resource, operation, default, capability, description, or persistence rule, verify:

- The definition, validators, expression support, alternatives/requirements, access constraints, flags, defaults, add/write/remove behavior, runtime service updates, and capability references agree. For new cross-subsystem dependencies, check the declared capability reference or service descriptor rather than reaching into another subsystem's internal service name. Check the add operation and writes separately. For a child, check its registration and lifecycle; for an operation, check parameters, reply, handler, and whether it is runtime only.
- Management descriptions and localized property keys cover what clients can read or invoke. Tests assert the meaningful model and runtime behavior, including non-default values and failure cases.
- Persistence matches intent: persistent configuration is parsed and written back; runtime-only attributes or operations do not accidentally enter XML or persisted model. A management-only change need not change XML if no persisted XML representation changes.
- If the change touches a preview, community, or experimental feature, check the applicable stability on the extension, resources, attributes, operations, and XML schema. Make sure the intended server stability can register and parse the feature, and that a lower stability does not expose it accidentally.
- If default server configuration or module dependencies change, trace the affected feature-pack configuration, layer, and module metadata (including `ee-feature-pack/`, `galleon-pack/`, and `preview/` where applicable). Check for a module-local `subsystem-templates/` rather than assuming a root template directory exists. Run relevant provisioning or configuration tests.
- Model version and older-model compatibility have been assessed as below. XML namespace, parser/writer, XSD (if used), and old/current XML tests have been assessed independently. Document an inapplicable step with the code or history that makes it inapplicable.

## Two version decisions

**Management model version** identifies the structure and semantics exposed to other controllers in a mixed domain. Find the version passed to `registerSubsystem` or `SubsystemConfiguration`, then compare the changed model with versions supported by the subsystem's transformers. New attributes/resources/operations, changed defaults, and changed semantics can require a new version and a transformation boundary. A runtime-only operation still needs this assessment if it changes the exposed management API, though it may need no XML work.

**XML namespace/schema version** identifies the syntax accepted and emitted for configuration. Find the current namespace and writer separately. A new XML element/attribute or changed XML meaning may require a new namespace, parser gating, and a corresponding XSD when the subsystem maintains XSDs. A management-only change does not itself require a namespace bump. Published old namespaces should retain their old grammar; never let a shared attribute list silently add a new field to old parsers. Conversely, changing only XML mapping need not imply a different management model version.

Make the two decisions independently against the last release on the current stream. Inspect release tags, the subsystem's version constants, and parser/schema history with `git log <release-tag>..HEAD -- <subsystem-path>` and `git show <release-tag>:<path>`. If the management model version has already changed since that release, do not bump it again for another model change on the same stream; use the new version's compatibility boundary. Apply the same rule separately to the XML namespace/schema version: if it has already changed since that release, do not bump it again for another XML change. If a version has not changed since the release and the new contract needs a version change, bump it and add the required compatibility work. Where the current stream has no release tag yet, use the latest applicable preceding release as the baseline. In every case, check that new XML syntax is gated away from published old namespaces and that transformations cover the supported older model targets. Record the release tag and version comparison in the review.

## Older model compatibility

Review every supported older target model version, including mixed domain use. Locate the transformer path from the current model to each target. Decide how the new model state and management operations behave on an older controller:

- **Discard** only an undefined value or a value whose effective behavior is demonstrably identical in the older model. A default is not automatically safe: compare old defaults and runtime semantics, including expression values.
- **Convert** when an older representation preserves the intended meaning, and test the converted value.
- **Reject** a defined value, child, or operation that cannot be represented safely. Prefer a clear rejection over silently losing user intent. Check add, write, undefine, and resource transformation as applicable.

Add or update the transformer and tests where needed. A success test should show current configuration or operations transform to the target and yield the intended legacy model/behavior. A rejection test should show an incompatible non-default configuration or operation fails transformation for the intended reason. Existing transformer tests may cover only a narrow path; inspect their assertions. If no transformer change is needed, record the target versions, old defaults/semantics, and code or test evidence supporting that conclusion in the review.

Verify that any transformer registration is discoverable: check the `ExtensionTransformerRegistration` implementation and either its `@MetaInfServices` annotation or the generated/declared `META-INF/services/org.jboss.as.controller.transform.ExtensionTransformerRegistration` entry. The implementation pattern varies; do not require a handwritten service file when annotation processing supplies it.

## XML compatibility

For persistent XML changes, update the active reader and writer and gate new syntax to the intended namespace. Check both directions: current XML parses to the expected model, and the model marshals to current XML that parses again. Test an old namespace with its old configuration and, where relevant, that it rejects new syntax. If versioned parsers share `ALL_ATTRIBUTES` or equivalent, check old versions explicitly. Update the matching XSD when the subsystem has schema files, and validate current XML against it; retain old schema behavior. XML changes and management model changes have separate version decisions even when both are required by one feature.

If the change affects a packaged default configuration, also check the [distribution XML validation tests](../dist/src/test/java/org/wildfly/dist/subsystem/xml/StandardConfigsXMLValidationUnitTestCase.java) and [XSD validation test](../dist/src/test/java/org/wildfly/dist/subsystem/xml/XSDValidationUnitTestCase.java), or their preview counterparts when applicable. These complement the module's subsystem tests; they do not replace old-namespace checks.

## Review finding format

Point to the changed definition or parser/registration line, name the missing compatibility step and the affected older model or namespace, and explain the observable failure or silent behavior change. Ask the author to add the version/transformer/parser/XSD/test change, or provide evidence that the existing contract already handles it. Do not treat a green module or integration run as a substitute for this assessment.
