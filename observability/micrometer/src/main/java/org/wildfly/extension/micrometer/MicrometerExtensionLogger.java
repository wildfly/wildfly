/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.jboss.as.controller.PathAddress;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYMMTREXT", length = 4)
public interface MicrometerExtensionLogger extends BasicLogger {
    MicrometerExtensionLogger MICROMETER_LOGGER = Logger.getMessageLogger(
            MethodHandles.lookup(), MicrometerExtensionLogger.class,
            MicrometerExtensionLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating Micrometer Subsystem")
    void activatingSubsystem();

    @LogMessage(level = INFO) // DEBUG
    @Message(id = 2, value = "Micrometer Subsystem is processing deployment")
    void processingDeployment();

    @LogMessage(level = DEBUG)
    @Message(id = 3, value = "The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping Micrometer integration.")
    void noCdiDeployment();

    @LogMessage(level = DEBUG)
    @Message(id = 4, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    void deploymentRequiresCapability(String deploymentName, String capabilityName);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "Unable to read attribute %s on %s: %s.")
    void unableToReadAttribute(String attributeName, PathAddress address, String error);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "Unable to convert attribute %s on %s to Double value.")
    void unableToConvertAttribute(String attributeName, PathAddress address, @Cause Exception exception);

    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Malformed name.")
    void malformedName(@Cause Exception exception);

    @Message(id = 8, value = "Failed to initialize metrics from JMX MBeans")
    IllegalArgumentException failedInitializeJMXRegistrar(@Cause IOException e);

    @Message(id = 9, value = "An unsupported metric type was found: %s")
    IllegalArgumentException unsupportedMetricType(String type);

    @LogMessage(level = INFO)
    @Message(id = 10, value = "Not activating Micrometer Subsystem")
    void notActivatingSubsystem();

    @LogMessage(level = WARN)
    @Message(id = 11, value = "Micrometer has been enabled, but no endpoint has been configured. A No-op metrics registry has been configured.")
    void noOpRegistryChosen();

    @LogMessage(level = INFO)
    @Message(id = 12, value = "Additional metrics systems discovered while configuring Micrometer: %s. Please refer to the documentation for more information.")
    void multipleMetricsSystemsEnabled(String others);
}
