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
    @Message(id = 1, value = "Vertx %s [%s] is started")
    void vertxStarted(String name, String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Vertx %s [%s] is closed")
    void vertxStopped(String name, String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Vertx %s [%s] is removed")
    void vertxRemoved(String name, String jndiName);

    @Message(id = 4, value = "Could not read VertxOptions from file: %s")
    OperationFailedException cannotReadVertxOptionsFile(String vertxOptionsFile);

    @Message(id = 5, value = "Could not find VertxOptions from: %s")
    OperationFailedException failedToReadVertxOptions(String vertxOptionsFile, @Cause Exception e);

    @Message(id = 6, value = "Failed to start VertxProxyService for %s")
    StartException failedToStartVertxService(String name, @Cause Throwable e);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "Error when closing the Vertx: %s")
    void errorWhenClosingVertx(String name, @Cause Exception e);

    @Message(id = 8, value = "Vertx: %s was not found.")
    OperationFailedException vertxNotFound(String name);

    @Message(id = 9, value = "Failed to read content from VirtualFile: %s .")
    DeploymentUnitProcessingException failedToReadConfig(String name, @Cause Throwable e);

    @Message(id = 10, value = "No verticle-class defined in deployment: %s")
    DeploymentUnitProcessingException noVerticleClassDefined(String deploymentName);

    @Message(id = 11, value = "Failed to lookup Vertx instance with JNDI name: %s")
    StartException failedToLookupVertx(String jndiName, @Cause Throwable e);

    @Message(id = 12, value = "Failed to deploy verticle: %s to Vertx: %s")
    StartException failedToDeployVerticle(String verticleClass, String jndiName, @Cause Throwable e);

    @LogMessage(level = WARN)
    @Message(id = 13, value = "Error to undeploy verticle: %s from Vertx: %s")
    void errorWhenUndeployVerticle(String verticleClass, String jndiName, @Cause Exception e);

    @Message(id = 14, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);

    @Message(id = 15, value = "Alias %s has been used already in Vertx: %s")
    OperationFailedException aliasUsedAlready(String alias, String vertxName);

    @Message(id = 16, value = "path of the vertx-options-file %s must be specified")
    OperationFailedException noOptionsFileSpecified(String optionName);

    @Message(id = 17, value = "Option Name: %s is reserved by system.")
    OperationFailedException optionNameIsReserved(String optionName);

    @Message(id = 18, value = "Either path or value should be specified for the option: %s")
    OperationFailedException atLeastPathOrValueDefined(String optionName);

    @Message(id = 19, value = "Cannot specify both jgroups-stack-file and jgroups-channel for clustered Vert.x: %s")
    OperationFailedException onlyOneJgroupsConfigNeeded(String name);

}
