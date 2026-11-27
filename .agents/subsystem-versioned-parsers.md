# Versioned-parser subsystems

Use this route when a direct `org.jboss.as.controller.Extension` maps XML namespaces to parser classes one by one. Apply the shared [subsystem checklist](subsystem-development.md). The [messaging extension](../messaging-activemq/subsystem/src/main/java/org/wildfly/extension/messaging/activemq/MessagingExtension.java) provides a current example of this wiring.

## Trace model and parser registration

In `initialize(...)`, find `registerSubsystem(...)` and the current management `ModelVersion`, root and child resource registrations, and `registerXMLElementWriter(...)`. In `initializeParsers(...)`, trace each `setSubsystemXmlMapping(...)` from namespace to reader. The current writer may be an instance of the latest parser; old readers may be constructed only when their namespace is encountered. Check the model version and XML namespace separately against the preceding Final baseline in the shared guide.

Inspect the resource definitions and their `registerAttributes`, `registerOperations`, `registerChildren`, add/write/remove handlers, capabilities, and runtime-only flags. A parser can accept an attribute without giving it correct management or runtime behavior, and a management definition can exist without any XML representation.

## Update the correct parser and writer

Versioned parser classes are not all hand-written StAX. Some extend `PersistentResourceXMLParser` and build `PersistentResourceXMLDescription`, as in the [messaging parser](../messaging-activemq/subsystem/src/main/java/org/wildfly/extension/messaging/activemq/MessagingSubsystemParser_18_0.java); others implement the XML reader/writer interfaces directly. Follow the affected parser's existing style. Check how its `getParserDescription()`, read methods, and write methods handle the new field, and which namespace the writer emits.

When adding a new namespace, wire its reader and current writer and preserve the old mappings. If parsers share a current attribute collection or builder, explicitly verify that old namespaces do not start accepting the new syntax. Where XSDs exist, keep released schemas compatible with their published grammar and validate new XML against the intended schema. Add old and current XML fixtures and a round-trip/boot test using the module's existing harness.

## Trace transformers and tests

The [messaging transformer registration](../messaging-activemq/subsystem/src/main/java/org/wildfly/extension/messaging/activemq/MessagingTransformerRegistration.java) uses a chained transformation description for older models. Other versioned-parser subsystems may use a flat description or another factory. Inspect target versions, root and child builders, operation transformation, and service discovery; choose discard, convert, or reject according to the shared compatibility rules.

Find tests under the module's `src/test/` by parser version and model target. Many versioned-parser subsystems keep distinct XML fixtures and transformation fixtures for successful and rejected configurations; follow their actual naming and test setup rather than assuming a universal test class or fixture name. Verify both the parsed model and the behavior of transformed operations where the change affects them.
