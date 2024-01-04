/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumeration of MicroProfile OpenAPI subsystem model versions.
 * @author Paul Ferraro
 */
public enum MicroProfileOpenAPISubsystemModel implements SubsystemModel {

    VERSION_1_0_0(1, 0, 0), // WildFly 19
    ;
    static final MicroProfileOpenAPISubsystemModel CURRENT = VERSION_1_0_0;

    private final ModelVersion version;

    MicroProfileOpenAPISubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
