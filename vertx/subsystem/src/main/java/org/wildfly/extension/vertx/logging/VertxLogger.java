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

import static org.jboss.logging.Logger.Level.INFO;
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

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Vert.x %s is started")
    void vertxStarted(String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Vertx [%s] is closed")
    void vertxStopped(String jndiName);

    @Message(id = 3, value = "Could not read VertxOptions from file: %s")
    OperationFailedException cannotReadVertxOptionsFile(String vertxOptionsFile);

    @Message(id = 4, value = "Could not find VertxOptions from: %s")
    OperationFailedException failedToReadVertxOptions(String vertxOptionsFile, @Cause Exception e);

    @Message(id = 5, value = "Failed to start VertxProxyService")
    StartException failedToStartVertxService(@Cause Throwable e);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "Error when closing the Vert.x instance")
    void errorWhenClosingVertx(@Cause Exception e);

    @Message(id = 7, value = "Failed to read content from VirtualFile: %s .")
    DeploymentUnitProcessingException failedToReadConfig(String name, @Cause Throwable e);

    @Message(id = 8, value = "No verticle-class defined in deployment: %s")
    DeploymentUnitProcessingException noVerticleClassDefined(String deploymentName);

    @Message(id = 9, value = "Failed to lookup Vertx instance with JNDI name: %s")
    StartException failedToLookupVertx(String jndiName, @Cause Throwable e);

    @Message(id = 10, value = "Failed to deploy verticle: %s to Vert.x")
    StartException failedToDeployVerticle(String verticleClass, @Cause Throwable e);

    @LogMessage(level = WARN)
    @Message(id = 11, value = "Error to undeploy verticle: %s from Vert.x")
    void errorWhenUndeployVerticle(String verticleClass, @Cause Exception e);

    @Message(id = 12, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);

    @Message(id = 13, value = "Path of the vertx-options-file %s must be specified")
    OperationFailedException noOptionsFileSpecified(String optionName);

    @Message(id = 14, value = "Option Name: %s is reserved by system.")
    OperationFailedException optionNameIsReserved(String optionName);

    @Message(id = 15, value = "Either path or value should be specified for the option: %s")
    OperationFailedException atLeastPathOrValueDefined(String optionName);

    @Message(id = 16, value = "Cannot specify both jgroups-stack-file and jgroups-channel for clustered Vert.x")
    OperationFailedException onlyOneJgroupsConfigNeeded();

    @LogMessage(level = WARN)
    @Message(id = 17, value = "Failed to resolve file: %s")
    void failedToResolveVFSFile(String fileName, @Cause Exception e);

    @Message(id = 18, value = "Resolving file: %s is not allowed")
    IllegalArgumentException fileResolveNotAllowed(String fileName);

}
