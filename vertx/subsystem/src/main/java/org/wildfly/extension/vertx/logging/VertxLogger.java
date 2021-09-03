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

    @Message(id = 4, value = "Could not find VertxOptions from: %s")
    IllegalStateException cannotFindVertxOptionsURL(String vertxOptionsFile);

    @Message(id = 5, value = "No Jgroups channel is specified for clustered Vertx: %s")
    OperationFailedException noJgroupsChannelConfigured(String name);

    @Message(id = 6, value = "Could not find VertxOptions from: %s")
    IllegalStateException failedToStartVertx(String vertxOptionsFile, @Cause Exception e);

    @Message(id = 7, value = "Failed to start VertxProxyService for %s")
    StartException failedToStartVertxService(String name, @Cause Throwable e);

    @LogMessage(level = WARN)
    @Message(id = 8, value = "Error when closing the Vertx: %s")
    void errorWhenClosingVertx(String name, @Cause Exception e);

}
