/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the supported versions of the Undertow subsystem model.
 * @author Paul Ferraro
 */
public enum UndertowSubsystemModel implements SubsystemModel {

    VERSION_11_0_0(11), // WildFly 23-26.x, EAP 7.4.x
    VERSION_12_0_0(12), // WildFly 27
    VERSION_13_0_0(13), // WildFly 28
    VERSION_14_0_0(14), // WildFly 32-present
    ;
    static final UndertowSubsystemModel CURRENT = VERSION_14_0_0;

    private final ModelVersion version;

    UndertowSubsystemModel(int major) {
        this(major, 0, 0);
    }

    UndertowSubsystemModel(int major, int minor) {
        this(major, minor, 0);
    }

    UndertowSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
