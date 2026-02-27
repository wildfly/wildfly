/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the model versions for the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 * @author Radoslav Husar
 */
public enum DistributableEjbSubsystemModel implements SubsystemModel {

    VERSION_1_0_0(1, 0, 0), // WildFly 27-38, EAP 8.0-present
    VERSION_2_0_0(2, 0, 0), // WildFly 39
    VERSION_3_0_0(3, 0, 0), // WildFly 40-present
    ;
    public static final DistributableEjbSubsystemModel CURRENT = VERSION_3_0_0;

    private final ModelVersion version;

    DistributableEjbSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
