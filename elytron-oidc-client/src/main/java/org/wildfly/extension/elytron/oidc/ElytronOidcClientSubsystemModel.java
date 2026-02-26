/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumeration of elytron-oidc-client subsystem model versions.
 * @author <a href="mailto:prpaul@redhat.com">Prarthona Paul</a>
 */
enum ElytronOidcClientSubsystemModel implements SubsystemModel {
    VERSION_1_0_0(1, 0, 0),
    VERSION_2_0_0(2, 0, 0),
    VERSION_3_0_0(3, 0, 0), // WildFly 32.0-present
    VERSION_4_0_0(4, 0, 0), // WildFly 33.0-present
    VERSION_5_0_0(5, 0, 0), // WildFly 34.0-present
    ;
    static final ElytronOidcClientSubsystemModel CURRENT = VERSION_5_0_0;

    private final ModelVersion version;

    ElytronOidcClientSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
