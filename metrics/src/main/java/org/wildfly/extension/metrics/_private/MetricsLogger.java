/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.metrics._private;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;

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
    MetricsLogger LOGGER = Logger.getMessageLogger(MetricsLogger.class, "org.wildfly.extension.metrics");

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
}