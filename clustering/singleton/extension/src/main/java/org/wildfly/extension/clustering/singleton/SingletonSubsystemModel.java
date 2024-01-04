/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumeration of supported versions of management model.
 * @author Paul Ferraro
 */
public enum SingletonSubsystemModel implements SubsystemModel {
/*  Unsupported model versions - for reference purposes only
    VERSION_1_0_0(1, 0, 0), // WildFly 10, EAP 7.0
    VERSION_2_0_0(2, 0, 0), // WildFly 11-14, EAP 7.1-7.2
*/
    VERSION_3_0_0(3, 0, 0), // WildFly 15-present, EAP 7.3-present
    ;
    static final SingletonSubsystemModel CURRENT = VERSION_3_0_0;

    private final ModelVersion version;

    SingletonSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
