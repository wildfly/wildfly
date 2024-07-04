/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.metrics._private;

import java.lang.invoke.MethodHandles;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly microprofile-metrics-smallrye Extension.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@MessageLogger(projectCode = "WFLYMPMETRICS", length = 4)
public interface MicroProfileMetricsLogger extends BasicLogger {
    /**
     * A logger with the category {@code org.wildfly.extension.batch}.
     */
    MicroProfileMetricsLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileMetricsLogger.class,
            "org.wildfly.extension.microprofile.metrics.smallrye");

    // no longer used
    // @LogMessage(level = INFO)
    // @Message(id = 1, value = "Activating MicroProfile Metrics Subsystem")
    // void activatingSubsystem();


    // no longer used
    // @Message(id = 2, value = "Failed to initialize metrics from JMX MBeans")
    // IllegalArgumentException failedInitializeJMXRegistrar(@Cause IOException e);

    // no longer used
    // @Message(id = 3, value = "Unable to read attribute %s on %s: %s.")
    // IllegalStateException unableToReadAttribute(String attributeName, PathAddress address, String error);

    // no longer used
    // @Message(id = 4, value = "Unable to convert attribute %s on %s to Double value.")
    // IllegalStateException unableToConvertAttribute(String attributeName, PathAddress address, @Cause Exception exception);

    // no longer used
    // @Message(id = 5, value = "Metric attribute %s on %s is undefined and will not be exposed.")
    // IllegalStateException undefinedMetric(String attributeName, PathAddress address);

    // no longer used
    // @Message(id = 6, value = "The metric was unavailable")
    // IllegalStateException metricUnavailable();

    //7, 8 and 9 are taken downstream
    /*
    @Message(id = 7, value = "")
    OperationFailedException seeDownstream();

    @Message(id = 8, value = "")
    String seeDownstream();

    @Message(id = 9, value = "")
    OperationFailedException seeDownstream();
    */

    @Message(id = 10, value = "The migrate operation cannot be performed. The server must be in admin-only mode.")
    OperationFailedException migrateOperationAllowedOnlyInAdminOnly();

    @Message(id = 11, value = "Migration failed. See results for more details.")
    String migrationFailed();
}
