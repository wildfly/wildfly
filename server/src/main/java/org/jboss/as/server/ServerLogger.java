/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ServerLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    ServerLogger ROOT_LOGGER = Logger.getMessageLogger(ServerLogger.class, ServerLogger.class.getPackage().getName());

    /**
     * A logger with the category {@code org.jboss.as}.
     */
    ServerLogger AS_ROOT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as");

    /**
     * A logger with the category {@code org.jboss.as.config}.
     */
    ServerLogger CONFIG_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.config");

    /**
     * A logger with the category {@code org.jboss.as.controller}.
     */
    ServerLogger CONTROLLER_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.controller");

    /**
     * A logger with the category {@code org.jboss.as.deployment}.
     */
    ServerLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.deployment");

    /**
     * A logger with the category {@code org.jboss.as.deployment.module}.
     */
    ServerLogger DEPLOYMENT_MODULE_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.deployment.module");
    /**
     * Logs an informational message indicating the server is starting.
     *
     * @param version  the server version.
     * @param codeName the code name.
     */
    @LogMessage(level = INFO)
    @Message("JBoss AS %s \"%s\" starting")
    void serverStarting(String version, String codeName);

    /**
     * Logs an informational message indicating the server is stopped.
     *
     * @param version  the server version.
     * @param codeName the code name.
     * @param time     the time it took to stop.
     */
    @LogMessage(level = INFO)
    @Message("JBoss AS %s \"%s\" stopped in %dms")
    void serverStopped(String version, String codeName, int time);
}
