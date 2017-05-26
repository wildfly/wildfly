/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.logging;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * InfinispanWebLogger
 *
 * logging id range: 10320 - 10329
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYCLWEBINF", length = 4)
public interface InfinispanWebLogger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.wildfly.clustering.web.infinispan";

    InfinispanWebLogger ROOT_LOGGER = Logger.getMessageLogger(InfinispanWebLogger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = WARN)
    @Message(id = 1, value = "Failed to passivate attributes of session %s")
    void failedToPassivateSession(@Cause Throwable cause, String sessionId);

    @LogMessage(level = WARN)
    @Message(id = 2, value = "Failed to passivate attribute %2$s of session %1$s")
    void failedToPassivateSessionAttribute(@Cause Throwable cause, String sessionId, String attribute);

    @Message(id = 3, value = "Session %s is not valid")
    IllegalStateException invalidSession(String sessionId);

    @LogMessage(level = WARN)
    @Message(id = 4, value = "Failed to expire session %s")
    void failedToExpireSession(@Cause Throwable cause, String sessionId);

    @LogMessage(level = DEBUG)
    @Message(id = 5, value = "Failed to cancel expiration/passivation of session %s on primary owner.")
    void failedToCancelSession(@Cause Throwable cause, String sessionId);

    @LogMessage(level = DEBUG)
    @Message(id = 6, value = "Failed to schedule expiration/passivation of session %s on primary owner.")
    void failedToScheduleSession(@Cause Throwable cause, String sessionId);

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

    @LogMessage(level = WARN)
    @Message(id = 12, value = "Disabling eviction for cache '%s'. Web session passivation should be configured via <max-active-sessions/> in jboss-web.xml.")
    void evictionDisabled(String cacheName);

    @LogMessage(level = WARN)
    @Message(id = 13, value = "Disabling expiration for cache '%s'. Web session expiration should be configured per \u00A77.5 of the servlet specification.")
    void expirationDisabled(String cacheName);
}
