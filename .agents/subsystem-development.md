# Subsystem development and review

Use this guide when a change affects a WildFly subsystem's management model, XML, or runtime configuration. Subsystems use different extension and persistence APIs; follow the affected subsystem's existing pattern. For code review, use the concise [subsystem review checklist](subsystem-review-checklist.md) alongside this guide. The [Extending WildFly documentation](../docs/src/main/asciidoc/Extending_WildFly.adoc) provides broader background.

## Choose the implementation route

Read the guide that matches the affected code, then use the shared checklist below. These are common routes, not exclusive generations: an extension may combine a newer registration API with an older XML API. Match the actual registered extension, parser, writer, and transformer rather than assuming one class determines all four.

| Route | Recognize it by | Guide |
| --- | --- | --- |
| `SubsystemExtension` and resource registrars | `SubsystemConfiguration`, `SubsystemPersistence`, `ManagementResourceRegistrar` | [Registrar-based subsystems](subsystem-registrar.md) |
| Direct `Extension` with a schema enum | `initialize`/`initializeParsers`, `PersistentSubsystemSchema` | [Persistent-schema subsystems](subsystem-persistent-schema.md) |
| Direct `Extension` with versioned parser classes | `setSubsystemXmlMapping` per namespace, `PersistentResourceXMLParser` or manual StAX | [Versioned-parser subsystems](subsystem-versioned-parsers.md) |

## Find the contract

1. Locate the module from the subsystem name or configuration namespace. Search `rg -n -F -e 'registerSubsystem(' -e 'SubsystemRegistration.of(' -e 'setSubsystemXmlMapping' -e 'SubsystemPersistence.of' --glob '*.java'` and inspect its `pom.xml` and `src/main/` tree.
2. From its extension, trace the registered root and child resource definitions, attribute definitions, add/remove and write handlers, operations, capabilities, and runtime services. Look for `registerAttributes`, `registerChildren`, `registerOperations`, `ResourceDescriptor`, and `ManagementResourceRegistrar`. Confirm which values are stored in the model and which are runtime only.
3. Locate the registered XML reader and writer, the current namespace or schema enum, any `src/main/resources/schema/*.xsd`, and test XML. Some subsystems use `SubsystemResourceXMLSchema`, some use `PersistentSubsystemSchema` or `PersistentResourceXMLParser`, and some have hand-written versioned parsers. Inspect actual registration before editing; a class name alone does not prove which parser or writer is active.
4. Locate `ExtensionTransformerRegistration` or a subsystem transformer factory, including service registration, and search `src/test/` and `testsuite/` for the subsystem name, management addresses, and namespace. Follow the module's existing test harness and version targets.

## Implement and inspect a management change

For each new or changed attribute, child resource, operation, default, capability, description, or persistence rule, verify:

- The definition has the intended type, validators, expressions, alternatives/requirements, access constraints, flags, and default. Check add and write behavior separately, including the runtime service update.
- Child resources have the intended registration and add/remove lifecycle. Operations have the intended parameters, reply, handler, and runtime-only behavior. For new cross-subsystem dependencies, check the declared capability reference or service descriptor rather than reaching into another subsystem's internal service name.
- Management descriptions and localized property keys cover what clients can read or invoke. Tests assert the meaningful model and runtime behavior, including non-default values and failure cases.
- Persistence matches intent: persistent configuration is parsed and written back; runtime-only attributes or operations do not accidentally enter XML or persisted model. A management-only change need not change XML if no persisted XML representation changes.
- If the change touches a preview, community, or experimental feature, check the applicable stability on the extension, resources, attributes, operations, and XML schema. Make sure the intended server stability can register and parse the feature, and that a lower stability does not expose it accidentally.
- If default server configuration or module dependencies change, trace the affected feature-pack configuration, layer, and module metadata (including `ee-feature-pack/`, `galleon-pack/`, and `preview/` where applicable). Check for a module-local `subsystem-templates/` rather than assuming a root template directory exists. Run relevant provisioning or configuration tests.
- Model version and older-model compatibility have been assessed as below. XML namespace, parser/writer, XSD (if used), and old/current XML tests have been assessed independently. Document an inapplicable step with the code or history that makes it inapplicable.

## Two version decisions

**Management model version** identifies the structure and semantics exposed to other controllers in a mixed domain. Find the version passed to `registerSubsystem` or `SubsystemConfiguration`, then compare the changed model with versions supported by the subsystem's transformers. New attributes/resources/operations, changed defaults, and changed semantics can require a new version and a transformation boundary. A runtime-only operation still needs this assessment if it changes the exposed management API, though it may need no XML work.

**XML namespace/schema version** identifies the syntax accepted and emitted for configuration. Find the current namespace and writer separately. A new XML element/attribute or changed XML meaning may require a new namespace, parser gating, and a corresponding XSD when the subsystem maintains XSDs. A management-only change does not itself require a namespace bump. Published old namespaces should retain their old grammar; never let a shared attribute list silently add a new field to old parsers. Conversely, changing only XML mapping need not imply a different management model version.

Make the two decisions independently against the preceding Final release for the target release stream. A prerelease of the target Final does not reset the baseline: for changes targeting 43.0.0.Final, compare with 42.0.0.Final even after 43.0.0.Beta1 is published.

1. Identify the preceding Final release on the target stream's development line, not a Beta/CR tag for the target Final or a maintenance release on another stream. Compare the subsystem's model and schema version constants at that tag with current source, for example using `git log <baseline-tag>..HEAD -- <subsystem-path>` and `git show <baseline-tag>:<path>`.
2. If the management model version already changed since the baseline, do not bump it again for another model change on this stream; use its compatibility boundary. Apply the same rule independently to the XML namespace/schema version. Otherwise, decide whether the changed contract requires a version change and its compatibility work.
3. If a shallow checkout lacks tags or history, obtain the relevant upstream tag and history before making a definitive version claim. The `-SNAPSHOT` project version alone does not establish the subsystem's release baseline. If the applicable baseline still cannot be established, note that uncertainty in the PR description or review rather than asserting that a bump is or is not required.

In every case, gate new XML syntax away from published old namespaces and check transformers for supported older model targets.

## Older model compatibility

Review every supported older target model version, including mixed domain use. Locate the transformer path from the current model to each target. Decide how the new model state and management operations behave on an older controller:

- **Discard** only an undefined value or a value whose effective behavior is demonstrably identical in the older model. A default is not automatically safe: compare old defaults and runtime semantics, including expression values.
- **Convert** when an older representation preserves the intended meaning, and test the converted value.
- **Reject** a defined value, child, or operation that cannot be represented safely. Prefer a clear rejection over silently losing user intent. Check add, write, undefine, and resource transformation as applicable.

Add or update the transformer and tests where needed. A success test should show current configuration or operations transform to the target and yield the intended legacy model/behavior. A rejection test should show an incompatible non-default configuration or operation fails transformation for the intended reason. Existing transformer tests may cover only a narrow path; inspect their assertions. If no transformer change is needed, record the target versions, old defaults/semantics, and code or test evidence supporting that conclusion in the review.

Verify that any transformer registration is discoverable: check the `ExtensionTransformerRegistration` implementation and either its `@MetaInfServices` annotation or the generated/declared `META-INF/services/org.jboss.as.controller.transform.ExtensionTransformerRegistration` entry. The implementation pattern varies; do not require a handwritten service file when annotation processing supplies it.

## XML compatibility

For persistent XML changes:

1. Update the active reader and writer. Check that current XML parses to the expected model and the model marshals to current XML that parses again.
2. Gate new syntax to the intended namespace. Test an old namespace with its old configuration and, where relevant, that it rejects new syntax. If versioned parsers share `ALL_ATTRIBUTES` or equivalent, check old versions explicitly.
3. Update the matching XSD when the subsystem has schema files and validate current XML against it; retain old schema behavior. Decide the XML and management model versions separately even when one feature affects both.

If the change affects a packaged default configuration, also check the [distribution XML validation tests](../dist/src/test/java/org/wildfly/dist/subsystem/xml/StandardConfigsXMLValidationUnitTestCase.java) and [XSD validation test](../dist/src/test/java/org/wildfly/dist/subsystem/xml/XSDValidationUnitTestCase.java), or their preview counterparts when applicable. These complement the module's subsystem tests; they do not replace old-namespace checks.
