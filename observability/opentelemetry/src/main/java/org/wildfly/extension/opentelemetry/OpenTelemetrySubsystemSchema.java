package org.wildfly.extension.opentelemetry;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

public enum OpenTelemetrySubsystemSchema implements PersistentSubsystemSchema<OpenTelemetrySubsystemSchema> {
    VERSION_1_0(1, 0), // WildFly 25
    ;
    public static final OpenTelemetrySubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> namespace;

    OpenTelemetrySubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(OpenTelemetrySubsystemExtension.SUBSYSTEM_PATH, this.namespace)
                .addAttributes(OpenTelemetrySubsystemDefinition.ATTRIBUTES)
                .build();
    }
}
