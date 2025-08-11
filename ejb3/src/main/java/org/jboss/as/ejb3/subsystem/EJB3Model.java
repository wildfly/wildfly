/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the supported model versions.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum EJB3Model implements SubsystemModel {

    VERSION_1_2_0(1, 2, 0),
    VERSION_1_2_1(1, 2, 1), // EAP 6.4.0
    VERSION_1_3_0(1, 3, 0), // EAP 6.4.7
    VERSION_3_0_0(3, 0, 0),
    VERSION_4_0_0(4, 0, 0), // EAP 7.0.0
    VERSION_5_0_0(5, 0, 0), // EAP 7.1 - 7.2
    VERSION_6_0_0(6, 0, 0),
    VERSION_7_0_0(7, 0, 0),
    VERSION_8_0_0(8, 0, 0),
    VERSION_9_0_0(9, 0, 0), // EAP 7.3 - 7.4
    VERSION_10_0_0(10, 0, 0), // EAP 8.0 - 8.1
    ;

    static final EJB3Model CURRENT = VERSION_10_0_0;

    private final ModelVersion version;

    EJB3Model(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
