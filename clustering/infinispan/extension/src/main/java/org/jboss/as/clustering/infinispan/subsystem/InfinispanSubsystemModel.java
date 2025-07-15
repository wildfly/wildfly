/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the supported model versions.
 * @author Paul Ferraro
 */
public enum InfinispanSubsystemModel implements SubsystemModel {
/*  Unsupported model versions - for reference only
    VERSION_1_6_0(1, 6, 0), // EAP 6.4
    VERSION_2_0_0(2, 0, 0), // WildFly 8
    VERSION_3_0_0(3, 0, 0), // WildFly 9
    VERSION_4_0_0(4, 0, 0), // WildFly 10, EAP 7.0
    VERSION_4_1_0(4, 1, 0), // WildFly 10.1
    VERSION_5_0_0(5, 0, 0), // WildFly 11, EAP 7.1
    VERSION_6_0_0(6, 0, 0), // WildFly 12
*/
    VERSION_7_0_0(7, 0, 0), // WildFly 13
/*
    VERSION_8_0_0(8, 0, 0), // WildFly 14-15, EAP 7.2
    VERSION_9_0_0(9, 0, 0), // WildFly 16
    VERSION_10_0_0(10, 0, 0), // WildFly 17
    VERSION_11_0_0(11, 0, 0), // WildFly 18-19, EAP 7.3
    VERSION_11_1_0(11, 1, 0), // EAP 7.3.4
    VERSION_12_0_0(12, 0, 0), // WildFly 20
    VERSION_13_0_0(13, 0, 0), // WildFly 21-22
*/
    VERSION_14_0_0(14, 0, 0), // WildFly 23, EAP 7.4
    VERSION_15_0_0(15, 0, 0), // WildFly 24-26
    VERSION_16_0_0(16, 0, 0), // WildFly 27
    VERSION_17_0_0(17, 0, 0), // WildFly 28-29
    VERSION_17_1_0(17, 1, 0), // EAP 8.0
    VERSION_18_0_0(18, 0, 0), // WildFly 30-34
    VERSION_19_0_0(19, 0, 0), // WildFly 35-36, EAP 8.1
    VERSION_20_0_0(20, 0, 0), // WildFly 37-present
    ;
    static final InfinispanSubsystemModel CURRENT = VERSION_20_0_0;

    private final ModelVersion version;

    InfinispanSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
