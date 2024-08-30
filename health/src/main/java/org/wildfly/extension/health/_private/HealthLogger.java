/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.health._private;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly health Extension.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2020 Red Hat inc.
 */
@MessageLogger(projectCode = "WFLYHEALTH", length = 4)
public interface HealthLogger extends BasicLogger {

    /**
     * A logger with the category {@code org.wildfly.extension.health}.
     */
    HealthLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), HealthLogger.class, "org.wildfly.extension.health");

    /**
     * Logs an informational message indicating the naming subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating Base Health Subsystem")
    void activatingSubsystem();
}
