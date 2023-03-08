package org.wildfly.extension.opentelemetry;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.SubsystemURN;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

public enum OpenTelemetrySubsystemSchema implements SubsystemSchema<OpenTelemetrySubsystemSchema> {
    VERSION_1_0(1, 0), // WildFly 25
    ;
    public static final OpenTelemetrySubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> namespace;

    OpenTelemetrySubsystemSchema(int major, int minor) {
        this.namespace = new SubsystemURN<>(OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        new OpenTelemetryParser(this).readElement(reader, operations);
    }
}
