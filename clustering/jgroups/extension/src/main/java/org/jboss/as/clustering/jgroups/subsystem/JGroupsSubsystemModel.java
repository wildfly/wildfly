/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the supported model versions.
 * @author Paul Ferraro
 */
public enum JGroupsSubsystemModel implements SubsystemModel {
/*  Unsupported model versions - for reference only
    VERSION_1_3_0(1, 3, 0), // EAP 6.4
    VERSION_2_0_0(2, 0, 0), // WildFly 8
    VERSION_3_0_0(3, 0, 0), // WildFly 9
    VERSION_4_0_0(4, 0, 0), // WildFly 10, EAP 7.0
    VERSION_4_1_0(4, 1, 0), // WildFly 10.1
*/  // We will continue to support generic protocol resource definition overrides
    VERSION_5_0_0(5, 0, 0), // WildFly 11, EAP 7.1
/*
    VERSION_6_0_0(6, 0, 0), // WildFly 12-16, EAP 7.2
    VERSION_7_0_0(7, 0, 0), // WildFly 17-19, EAP 7.3
*/
    VERSION_8_0_0(8, 0, 0), // WildFly 20-26, EAP 7.4
    VERSION_9_0_0(9, 0, 0), // WildFly 27-29
    VERSION_10_0_0(10, 0, 0), // WildFly 30-38, EAP 8.0-8.1
    VERSION_11_0_0(11, 0, 0), // WildFly 39-present
    ;
    static final JGroupsSubsystemModel CURRENT = VERSION_11_0_0;

    private final ModelVersion version;

    JGroupsSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
