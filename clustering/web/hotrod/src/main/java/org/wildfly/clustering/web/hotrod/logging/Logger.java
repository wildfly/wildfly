/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.logging;

import static org.jboss.logging.Logger.Level.*;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLWEBHR", length = 4)
public interface Logger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.wildfly.clustering.web.hotrod";

    Logger ROOT_LOGGER = org.jboss.logging.Logger.getMessageLogger(MethodHandles.lookup(), Logger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = WARN)
    @Message(id = 1, value = "Failed to expire session %s")
    void failedToExpireSession(@Cause Throwable cause, String sessionId);

//    @Message(id = 3, value = "Session %s is not valid")
//    IllegalStateException invalidSession(String sessionId);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "Failed to activate attributes of session %s")
    void failedToActivateSession(@Cause Throwable cause, String sessionId);

    @LogMessage(level = WARN)
    @Message(id = 8, value = "Failed to activate attribute %2$s of session %1$s")
    void failedToActivateSessionAttribute(@Cause Throwable cause, String sessionId, String attribute);

    @Message(id = 9, value = "Failed to read attribute %2$s of session %1$s")
    IllegalStateException failedToReadSessionAttribute(@Cause Throwable cause, String sessionId, String attribute);

    @LogMessage(level = WARN)
    @Message(id = 10, value = "Failed to activate authentication for single sign on %s")
    void failedToActivateAuthentication(@Cause Throwable cause, String ssoId);

    @LogMessage(level = WARN)
    @Message(id = 11, value = "Session %s is missing cache entry for attribute %s")
    void missingSessionAttributeCacheEntry(String sessionId, String attribute);
}
