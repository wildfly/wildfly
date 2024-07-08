/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee.infinispan.logging;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logger for the org.wildfly.clustering.ee.infinispan module.
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLEEINF", length = 4)
public interface Logger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.wildfly.clustering.ee.infinispan";

    Logger ROOT_LOGGER = org.jboss.logging.Logger.getMessageLogger(MethodHandles.lookup(), Logger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Failed to cancel %s on primary owner.")
    void failedToCancel(@Cause Throwable cause, Object id);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Failed to schedule %s on primary owner.")
    void failedToSchedule(@Cause Throwable cause, Object id);
}
