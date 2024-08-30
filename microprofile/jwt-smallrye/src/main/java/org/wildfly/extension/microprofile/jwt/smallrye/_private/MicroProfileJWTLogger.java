/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.jwt.smallrye._private;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 *
 * <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@MessageLogger(projectCode = "WFLYJWT", length = 4)
public interface MicroProfileJWTLogger extends BasicLogger {
    /**
     * The root logger with a category of the package name.
     */
    MicroProfileJWTLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileJWTLogger.class, "org.wildfly.extension.microprofile.jwt.smallrye");

    /**
     * Logs an informational message indicating the naming subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile JWT Subsystem")
    void activatingSubsystem();

    @LogMessage(level = WARN)
    @Message(id = 2, value = "@LoginConfig annotation detected on invalid target \"%s\".")
    void loginConfigInvalidTarget(String target);

    @Message(id = 3, value = "No `authMethod` specified on the @LoginConfig annotation.")
    DeploymentUnitProcessingException noAuthMethodSpecified();

}
