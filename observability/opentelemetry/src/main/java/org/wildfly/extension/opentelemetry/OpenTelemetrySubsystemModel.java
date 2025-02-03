/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

public enum OpenTelemetrySubsystemModel implements SubsystemModel {
    VERSION_1_0_0(1, 0, 0),
    VERSION_1_1_0(1, 1, 0),
    VERSION_2_0_0(2, 0, 0);

    public static final OpenTelemetrySubsystemModel CURRENT = VERSION_2_0_0;

    private final ModelVersion version;

    OpenTelemetrySubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return version;
    }
}
