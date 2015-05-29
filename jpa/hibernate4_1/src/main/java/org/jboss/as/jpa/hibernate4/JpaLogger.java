/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
