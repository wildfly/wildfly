/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.elytron.oidc._private;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 *
 * <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@MessageLogger(projectCode = "WFLYOIDC", length = 4)
public interface ElytronOidcLogger extends BasicLogger {
    /**
     * The root logger with a category of the package name.
     */
    ElytronOidcLogger ROOT_LOGGER = Logger.getMessageLogger(ElytronOidcLogger.class, ElytronOidcLogger.class.getPackage().getName());

    /**
     * Logs an informational message indicating the naming subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating WildFly Elytron OIDC Subsystem")
    void activatingSubsystem();

}