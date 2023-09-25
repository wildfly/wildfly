/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumeration of supported versions of management model.
 *
 * @author Radoslav Husar
 */
public enum MicroProfileFaultToleranceModel implements SubsystemModel {

    VERSION_1_0_0(1, 0, 0), // WildFly 19-present
    ;
    static final MicroProfileFaultToleranceModel CURRENT = VERSION_1_0_0;

    private final ModelVersion version;

    MicroProfileFaultToleranceModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}