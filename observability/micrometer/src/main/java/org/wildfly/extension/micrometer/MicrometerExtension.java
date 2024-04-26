/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

public class MicrometerExtension extends SubsystemExtension<MicrometerSubsystemSchema> {
    public MicrometerExtension() {
        super(SubsystemConfiguration.of(MicrometerConfigurationConstants.NAME,
                        MicrometerSubsystemModel.CURRENT,
                        MicrometerSubsystemRegistrar::new),
                SubsystemPersistence.of(MicrometerSubsystemSchema.CURRENT));
    }
}
