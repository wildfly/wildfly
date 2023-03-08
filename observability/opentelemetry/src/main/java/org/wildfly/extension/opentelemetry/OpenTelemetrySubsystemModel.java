package org.wildfly.extension.opentelemetry;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

public enum OpenTelemetrySubsystemModel implements SubsystemModel {
    VERSION_1_0_0(1, 0, 0);

    public static final OpenTelemetrySubsystemModel CURRENT = VERSION_1_0_0;

    private final ModelVersion version;

    OpenTelemetrySubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return version;
    }
}
