package org.wildfly.extension.micrometer.model;

import org.jboss.as.controller.ModelVersion;

public enum MicrometerModel {
    VERSION_1_0_0(1, 0, 0);

    public static final MicrometerModel CURRENT = VERSION_1_0_0;

    private final ModelVersion version;

    MicrometerModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    public ModelVersion getVersion() {
        return version;
    }
}
