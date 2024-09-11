/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYMPTEL", length = 4)
interface MicroProfileTelemetryExtensionLogger extends BasicLogger {
    MicroProfileTelemetryExtensionLogger MPTEL_LOGGER = Logger.getMessageLogger(
            MethodHandles.lookup(), MicroProfileTelemetryExtensionLogger.class,
            MicroProfileTelemetryExtensionLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile Telemetry Subsystem")
    void activatingSubsystem();

    @Message(id = 2, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);

}
