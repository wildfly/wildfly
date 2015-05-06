/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate4;

import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * JipiJapa message range is 20200-20299
 * note: keep duplicate messages in sync between different sub-projects that use the same messages
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Scott Marlow
 */
@MessageLogger(projectCode = "JIPI")
public interface JpaLogger extends BasicLogger {

    /**
     * A logger with the category {@code org.jboss.jpa}.
     */
    JpaLogger JPA_LOGGER = Logger.getMessageLogger(JpaLogger.class, "org.jipijapa");


    /**
     * warn that the entity class could not be loaded with the
     * {@link javax.persistence.spi.PersistenceUnitInfo#getClassLoader()}.
     *
     * @param cause     the cause of the error.
     * @param className the entity class name.
     */
    @LogMessage(level = WARN)
    @Message(id = 20200, value = "Could not load entity class '%s', ignoring this error and continuing with application deployment")
    void cannotLoadEntityClass(@Cause Throwable cause, String className);

    /**
     * Creates an exception indicating the input stream reference cannot be changed.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 20201, value = "Cannot change input stream reference.")
    IllegalArgumentException cannotChangeInputStream();

    /**
     * Creates an exception indicating the parameter, likely a collection, is empty.
     *
     * @param parameterName the parameter name.
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 20202, value = "Parameter %s is empty")
    IllegalArgumentException emptyParameter(String parameterName);

    /**
     * Creates an exception indicating the persistence unit metadata likely because thread local was not set.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 20203, value = "Missing PersistenceUnitMetadata (thread local wasn't set)")
    RuntimeException missingPersistenceUnitMetadata();

    /**
     * Creates an exception indicating the method is not yet implemented.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 20204, value = "Not yet implemented")
    RuntimeException notYetImplemented();

    /**
     * Creates an exception indicating the variable is {@code null}.
     *
     * @param varName the variable name.
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 20205, value = "Parameter %s is null")
    IllegalArgumentException nullVar(String varName);

}
