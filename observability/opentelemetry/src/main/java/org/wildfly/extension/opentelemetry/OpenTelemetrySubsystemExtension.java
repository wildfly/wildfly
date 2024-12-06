/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import io.vertx.core.logging.LoggerFactory;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
public class OpenTelemetrySubsystemExtension extends SubsystemExtension<OpenTelemetrySubsystemSchema> {
    public OpenTelemetrySubsystemExtension() {
        super(SubsystemConfiguration.of(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME,
                OpenTelemetrySubsystemModel.CURRENT,
                OpenTelemetrySubsystemRegistrar::new),
                SubsystemPersistence.of(OpenTelemetrySubsystemSchema.CURRENT));
        // Initialize the Vert.x logger factory
        //noinspection deprecation
        LoggerFactory.initialise();
    }
}
