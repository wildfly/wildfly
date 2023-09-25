/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the supported model versions of the MicroProfile LRA participant subsystem.
 * @author Paul Ferraro
 */
enum MicroProfileLRAParticipantSubsystemModel implements SubsystemModel {
    VERSION_1_0_0(1),
    ;

    private final ModelVersion version;

    MicroProfileLRAParticipantSubsystemModel(int major) {
        this.version = ModelVersion.create(major);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}