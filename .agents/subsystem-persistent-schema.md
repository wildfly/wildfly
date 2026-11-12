# Persistent-schema subsystems

Use this route when a direct `org.jboss.as.controller.Extension` implementation registers the model itself and uses a `PersistentSubsystemSchema` enum for XML. Apply the shared [subsystem checklist](subsystem-development.md). The [Undertow extension](../undertow/src/main/java/org/wildfly/extension/undertow/UndertowExtension.java) and [schema](../undertow/src/main/java/org/wildfly/extension/undertow/UndertowSubsystemSchema.java) show the wiring in this tree.

## Trace model registration

Read `initialize(...)`: it passes a model version to `context.registerSubsystem(...)`, registers the root resource with `registerSubsystemModel(...)`, may register child or deployment resources, and selects the XML writer. Read `initializeParsers(...)` to see which schema values are accepted. Some extensions select the current writer by server `Stability`; do not assume a single `CURRENT` enum constant is always active.

The root and child resource definitions may extend `SimpleResourceDefinition`, `PersistentResourceDefinition`, or another base. Follow their `registerAttributes`, `registerOperations`, and `registerChildren` methods (or their descriptors) to find read/write handlers, add/remove handlers, capabilities, runtime-only registrations, and descriptions. The XML schema is not the management resource definition: check both when the model changes.

## Update versioned XML

`PersistentSubsystemSchema.getXMLDescription()` builds a `PersistentResourceXMLDescription` from `PathElement`s. Find its `PersistentResourceXMLDescription.Factory` and current writer (`PersistentResourceXMLDescriptionWriter` in the [Undertow extension](../undertow/src/main/java/org/wildfly/extension/undertow/UndertowExtension.java)). Attribute streams and child builders can be filtered with `this.since(...)` or an equivalent version check; the [Undertow schema](../undertow/src/main/java/org/wildfly/extension/undertow/UndertowSubsystemSchema.java) does both. Gate new syntax to the intended namespace, verify marshalled XML, and update a matching XSD if this subsystem maintains XSDs. Compare model and XML versions independently with the last release on the current stream.

## Trace transformers and tests

The [Undertow transformer registration](../undertow/src/main/java/org/wildfly/extension/undertow/UndertowExtensionTransformerRegistration.java) iterates older model targets, builds a transformation description per target, and registers it. Other subsystems may use chained transformations. Find `@MetaInfServices` or a declared service entry, then inspect the actual builders for root and child changes. Test equivalent old behavior, conversion, and rejection as applicable; do not infer compatibility from the presence of a registration class.

Look for parameterized schema tests and transformer tests in the module. The [Undertow subsystem tests](../undertow/src/test/java/org/wildfly/extension/undertow/UndertowSubsystemTestCase.java) cover schema variants, and its [transformer tests](../undertow/src/test/java/org/wildfly/extension/undertow/UndertowSubsystemTransformerTestCase.java) check transformed models and failed operations. Follow the same harness and assertions for the affected resource.
