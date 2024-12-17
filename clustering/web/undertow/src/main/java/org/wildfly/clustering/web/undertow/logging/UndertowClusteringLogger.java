/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.logging;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYCLWEBUT", length = 4)
public interface UndertowClusteringLogger extends BasicLogger {

    UndertowClusteringLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), UndertowClusteringLogger.class, "org.wildfly.clustering.web.undertow");

    @Message(id = 1, value = "Session %s is invalid")
    IllegalStateException sessionIsInvalid(String sessionId);

    @Message(id = 2, value = "Session %s already exists")
    IllegalStateException sessionAlreadyExists(String sessionId);

    @Message(id = 3, value = "Session manager was stopped")
    IllegalStateException sessionManagerStopped();

    @Message(id = 4, value = "Legacy <replication-config/> overriding attached distributable session management provider for %s")
    @LogMessage(level = Level.WARN)
    void legacySessionManagementProviderOverride(String deploymentName);

    @Message(id = 5, value = "No distributable session management provider found for %s; using legacy provider based on <replication-config/>")
    @LogMessage(level = Level.WARN)
    void legacySessionManagementProviderInUse(String name);

    @Message(id = 7, value = "No routing provider found for %s; using legacy provider based on static configuration")
    @LogMessage(level = Level.WARN)
    void legacyRoutingProviderInUse(String name);

    @Message(id = 8, value = "No distributable single sign-on management provider found for %s; using legacy provider based on static configuration")
    @LogMessage(level = Level.WARN)
    void legacySingleSignOnProviderInUse(String name);

    @Message(id = 9, value = "Invalidation attempted for session %s after the response was committed (e.g. after HttpServletResponse.sendRedirect or sendError)")
    IllegalStateException batchIsAlreadyClosed(String sessionId);
}
