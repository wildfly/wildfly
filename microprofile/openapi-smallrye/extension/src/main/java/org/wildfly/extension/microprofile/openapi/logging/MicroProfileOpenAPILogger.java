/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi.logging;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
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
    MicroProfileOpenAPILogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileOpenAPILogger.class, "org.wildfly.extension.microprofile.openapi.smallrye");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile OpenAPI Subsystem")
    void activatingSubsystem();

    @Message(id = 2, value = "Failed to load OpenAPI '%s' from deployment '%s'")
    IllegalArgumentException failedToLoadStaticFile(@Cause IOException e, String fileName, String deploymentName);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "MicroProfile OpenAPI endpoint already registered for host '%s'.  Skipping OpenAPI documentation of '%s'.")
    void endpointAlreadyRegistered(String hostName, String deployment);

    @LogMessage(level = INFO)
    @Message(id = 4, value = "Registered MicroProfile OpenAPI endpoint '%s' for host '%s'")
    void endpointRegistered(String path, String hostName);

    @LogMessage(level = INFO)
    @Message(id = 5, value = "Unregistered MicroProfile OpenAPI endpoint '%s' for host '%s'")
    void endpointUnregistered(String path, String hostName);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "\u00A75.1 of MicroProfile OpenAPI specification requires that the endpoint be accessible via %2$s, but no such listeners exists for server '%1$s'.")
    void requiredListenersNotFound(String serverName, Set<String> requisiteSchemes);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "\u00A75.1 of MicroProfile OpenAPI specification requires documentation to be available at '%3$s', but '%1$s' is configured to use '%2$s'")
    void nonStandardEndpoint(String deploymentName, String deploymentEndpoint, String standardEndpoint);

    @LogMessage(level = INFO)
    @Message(id = 8, value = "MicroProfile OpenAPI documentation disabled for '%s'")
    void disabled(String deploymentName);

    @LogMessage(level = WARN)
    @Message(id = 9, value = "Ignoring deployment-specific property value for %s due to conflicts: %s")
    void propertyValueConflicts(String propertyName, Map<String, String> conflicts);

    @LogMessage(level = INFO)
    @Message(id = 10, value = "Host configuration overrides deployment-specific property value for %s: %s")
    void propertyValueOverride(String propertyName, String propertyValue);
}
