/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.undertow.logging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYCLWEBUT", length = 4)
public interface UndertowClusteringLogger extends BasicLogger {

    UndertowClusteringLogger ROOT_LOGGER = Logger.getMessageLogger(UndertowClusteringLogger.class, "org.wildfly.clustering.web.undertow");

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
