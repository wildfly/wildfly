/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.metrics._private;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import static org.jboss.logging.Logger.Level.ERROR;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.jboss.as.controller.PathAddress;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly metrics Extension.
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@MessageLogger(projectCode = "WFLYMETRICS", length = 4)
public interface MetricsLogger extends BasicLogger {
    /**
     * A logger with the category {@code org.wildfly.extension.batch}.
     */
    MetricsLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MetricsLogger.class, "org.wildfly.extension.metrics");

    /**
     * Logs an informational message indicating the subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating Base Metrics Subsystem")
    void activatingSubsystem();

    @Message(id = 2, value = "Failed to initialize metrics from JMX MBeans")
    IllegalArgumentException failedInitializeJMXRegistrar(@Cause IOException e);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Unable to read attribute %s on %s: %s.")
    void unableToReadAttribute(String attributeName, PathAddress address, String error);

    @LogMessage(level = WARN)
    @Message(id = 4, value = "Unable to convert attribute %s on %s to Double value.")
    void unableToConvertAttribute(String attributeName, PathAddress address, @Cause Exception exception);

    @LogMessage(level = ERROR)
    @Message(id = 5, value = "Malformed name.")
    void malformedName(@Cause Exception exception);

    @LogMessage(level = INFO)
    @Message(id = 6, value = "Additional metrics systems discovered while configuring WildFly Metrics: %s. Please refer to the documentation for more information.")
    void multipleMetricsSystemsEnabled(String others);
}
