/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the model versions for the distributable-web subsystem.
 * @author Paul Ferraro
 */
public enum DistributableWebSubsystemModel implements SubsystemModel {

    /*
    List of unsupported versions commented out for reference purposes:

    VERSION_1_0_0(1, 0, 0), // WildFly 17
     */
    VERSION_2_0_0(2, 0, 0), // WildFly 18-26, EAP 7.4
    VERSION_3_0_0(3, 0, 0), // WildFly 27-29
    VERSION_4_0_0(4, 0, 0), // WildFly 30-38, EAP 8.0-present
    VERSION_5_0_0(5, 0, 0), // WildFly 39-present
    ;
    public static final DistributableWebSubsystemModel CURRENT = VERSION_5_0_0;

    private final ModelVersion version;

    DistributableWebSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
