/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the supported mod_cluster model versions.
 *
 * @author Radoslav Husar
 */
public enum ModClusterSubsystemModel implements SubsystemModel {
/*  Unsupported model versions - for reference only:

    VERSION_1_5_0(1, 5, 0), // EAP 6.3-6.4
    VERSION_2_0_0(2, 0, 0), // WildFly 8
    VERSION_3_0_0(3, 0, 0), // WildFly 9
    VERSION_4_0_0(4, 0, 0), // WildFly 10, EAP 7.0
    VERSION_5_0_0(5, 0, 0), // WildFly 11-13, EAP 7.1
    VERSION_6_0_0(6, 0, 0), // WildFly 14-15, EAP 7.2
*/
    VERSION_7_0_0(7, 0, 0), // WildFly 16-26, EAP 7.3-7.4
    VERSION_8_0_0(8, 0, 0), // WildFly 27-38
    VERSION_9_0_0(9, 0, 0), // WildFly 39-present
    ;

    public static final ModClusterSubsystemModel CURRENT = VERSION_8_0_0;

    private final ModelVersion version;

    ModClusterSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}