# Registrar-based subsystems

Use this route when the extension extends `org.wildfly.subsystem.SubsystemExtension`. Apply the shared [subsystem checklist](subsystem-development.md) for versioning, compatibility, XML, and review decisions. The [JGroups extension](../clustering/jgroups/extension/src/main/java/org/jboss/as/clustering/jgroups/subsystem/JGroupsExtension.java) and [resource registrar](../clustering/jgroups/extension/src/main/java/org/jboss/as/clustering/jgroups/subsystem/JGroupsSubsystemResourceDefinitionRegistrar.java) show this route in the current tree.

## Trace registration and runtime behavior

1. Read the extension constructor. `SubsystemConfiguration.of(...)` identifies the subsystem registration, current `SubsystemModel`, and registrar supplier; `SubsystemPersistence.of(...)` identifies the schema(s) used for XML persistence. Check extension/service discovery (`@MetaInfServices(Extension.class)` or a declared service entry).
2. Follow the `SubsystemResourceDefinitionRegistrar.register(...)` implementation. It registers the root `ResourceDefinition`, builds a `ResourceDescriptor`, calls `ManagementResourceRegistrar.of(descriptor).register(...)`, and may register child resource registrars. Trace each new attribute or operation through its descriptor/definition, model population, write handling, runtime handler, and capability dependency. Do not assume adding it to a constant list registers it everywhere.
3. Find the `SubsystemModel` enum or equivalent version supplied to `SubsystemConfiguration`; compare it with the last release on the current stream as described in the shared guide. Versioning is separate from XML persistence.

## Follow the XML API actually used

`SubsystemExtension` does not imply one XML API. One route is `SubsystemResourceXMLSchema` with `ResourceXMLParticleFactory` and `getSubsystemXMLElement()`; see the [JGroups schema](../clustering/jgroups/extension/src/main/java/org/jboss/as/clustering/jgroups/subsystem/JGroupsSubsystemSchema.java). Another is `PersistentSubsystemSchema` with `PersistentResourceXMLDescription` and `getXMLDescription()`; see the [Vert.x schema](../vertx/subsystem/src/main/java/org/wildfly/extension/vertx/VertxSubsystemSchema.java). Identify the current schema and the actual element builder, then add or filter XML syntax for the intended namespace. Check its XSD if the module has one. Older schemas must not inherit new syntax through shared builders.

## Trace transformers and tests

Find the subsystem's `ExtensionTransformerRegistration` implementation or transformation factory. The [JGroups registration](../clustering/jgroups/extension/src/main/java/org/jboss/as/clustering/jgroups/subsystem/JGroupsExtensionTransformerRegistration.java) registers descriptions for older model targets and uses `@MetaInfServices`; the transformer logic is in a separate factory. Other registrar-based subsystems may use a different factory or direct builders. Confirm registration discovery, target versions, and the behavior of new attributes, children, and operations rather than copying one registration shape.

Look under the module's `src/test/java` and `src/test/resources` for schema, model, and transformer tests. The [JGroups schema tests](../clustering/jgroups/extension/src/test/java/org/jboss/as/clustering/jgroups/subsystem/JGroupsSubsystemTestCase.java) exercise supported XML versions; its [transformer tests](../clustering/jgroups/extension/src/test/java/org/jboss/as/clustering/jgroups/subsystem/JGroupsTransformersTestCase.java) show success and rejection paths. Adapt the subsystem's harness and test supported targets, including old XML and an incompatible new value where relevant.
