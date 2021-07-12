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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

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

    @Message(id = 4, value = "Illegal State exception occurred")
    IllegalStateException illegalException(@Cause Exception e);

    @Message(id = 5, value = "Failed to start Vertx %s")
    IllegalStateException failedToStartVertx(String name, @Cause Exception e);



}
