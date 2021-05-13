package org.wildfly.extension.opentelemetry;

import org.jboss.as.controller.ModelVersion;

public enum OpenTelemetryModel {
    VERSION_1_0_0(1, 0, 0);

    public static final OpenTelemetryModel CURRENT = VERSION_1_0_0;

    private final ModelVersion version;

    OpenTelemetryModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    public ModelVersion getVersion() {
        return version;
    }
}
