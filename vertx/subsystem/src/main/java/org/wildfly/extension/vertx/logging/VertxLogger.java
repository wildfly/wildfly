/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx.logging;

import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;

@MessageLogger(projectCode = "WFLYVTX", length = 4)
public interface VertxLogger extends BasicLogger {

    VertxLogger VERTX_LOGGER = Logger.getMessageLogger(VertxLogger.class, "org.wildfly.extension.vertx");

    @Message(id = 1, value = "Could not read VertxOptions from file: %s")
    OperationFailedException cannotReadVertxOptionsFile(String vertxOptionsFile);

    @Message(id = 2, value = "Could not find VertxOptions from: %s")
    OperationFailedException failedToReadVertxOptions(String vertxOptionsFile, @Cause Exception e);

    @Message(id = 3, value = "Failed to start VertxProxyService")
    StartException failedToStartVertxService(@Cause Throwable e);

    @LogMessage(level = WARN)
    @Message(id = 4, value = "Error when closing the Vert.x instance")
    void errorWhenClosingVertx(@Cause Exception e);

    @Message(id = 5, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);

    @Message(id = 6, value = "Path of the vertx-options-file %s must be specified")
    OperationFailedException noOptionsFileSpecified(String optionName);

    @Message(id = 7, value = "Absolute directory %s is not allowed for the VertxOptions file")
    OperationFailedException absoluteDirectoryNotAllowed(String dir);
}
