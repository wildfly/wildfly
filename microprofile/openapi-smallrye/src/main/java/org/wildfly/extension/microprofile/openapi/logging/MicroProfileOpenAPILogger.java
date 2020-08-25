/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.openapi.logging;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.util.Set;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly microprofile-openapi-smallrye subsystem.
 *
 * @author Michael Edgar
 */
@MessageLogger(projectCode = "WFLYMPOAI", length = 4)
public interface MicroProfileOpenAPILogger extends BasicLogger {
    MicroProfileOpenAPILogger LOGGER = Logger.getMessageLogger(MicroProfileOpenAPILogger.class, "org.wildfly.extension.microprofile.openapi.smallrye");

    @LogMessage(level = DEBUG)
    @Message(id = 1, value = "Activating WildFly MicroProfile OpenAPI Subsystem")
    void activatingSubsystem();

    @Message(id = 2, value = "Failed to load OpenAPI '%s' from deployment '%s'")
    IllegalArgumentException failedToLoadStaticFile(@Cause IOException e, String fileName, String deploymentName);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "WildFly MicroProfile OpenAPI endpoint already registered for host '%s'.  Skipping OpenAPI documentation of '%s'.")
    void endpointAlreadyRegistered(String hostName, String deployment);

    @LogMessage(level = INFO)
    @Message(id = 4, value = "Registered WildFly MicroProfile OpenAPI endpoint '%s' for host '%s'")
    void endpointRegistered(String path, String hostName);

    @LogMessage(level = INFO)
    @Message(id = 5, value = "Unregistered WildFly MicroProfile OpenAPI endpoint '%s' for host '%s'")
    void endpointUnregistered(String path, String hostName);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "\u00A75.1 of WildFly MicroProfile OpenAPI specification requires that the endpoint be accessible via %2$s, but no such listeners exists for server '%1$s'.")
    void requiredListenersNotFound(String serverName, Set<String> requisiteSchemes);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "\u00A75.1 of WildFly MicroProfile OpenAPI specification requires documentation to be available at '%3$s', but '%1$s' is configured to use '%2$s'")
    void nonStandardEndpoint(String deploymentName, String deploymentEndpoint, String standardEndpoint);

    @LogMessage(level = INFO)
    @Message(id = 8, value = "WildFly MicroProfile OpenAPI documentation disabled for '%s'")
    void disabled(String deploymentName);
}
