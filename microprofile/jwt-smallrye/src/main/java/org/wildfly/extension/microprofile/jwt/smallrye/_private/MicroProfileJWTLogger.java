/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.microprofile.jwt.smallrye._private;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

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
    MicroProfileJWTLogger ROOT_LOGGER = Logger.getMessageLogger(MicroProfileJWTLogger.class, "org.wildfly.extension.microprofile.jwt.smallrye");

    /**
     * Logs an informational message indicating the naming subsystem is being activated.
     */
    @LogMessage(level = DEBUG)
    @Message(id = 1, value = "Activating WildFly MicroProfile JWT Subsystem")
    void activatingSubsystem();

    @LogMessage(level = WARN)
    @Message(id = 2, value = "@LoginConfig annotation detected on invalid target \"%s\".")
    void loginConfigInvalidTarget(String target);

    @Message(id = 3, value = "No `authMethod` specified on the @LoginConfig annotation.")
    DeploymentUnitProcessingException noAuthMethodSpecified();

}
