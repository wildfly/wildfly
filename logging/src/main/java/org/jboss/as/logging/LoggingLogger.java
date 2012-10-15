/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.Closeable;

import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * This module is using message IDs in the range 11500-11599.
 * <p/>
 * This file is using the subset 11500-11529 for logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 * Date: 09.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface LoggingLogger extends BasicLogger {

    /**
     * A root logger with the category of the package name.
     */
    LoggingLogger ROOT_LOGGER = Logger.getMessageLogger(LoggingLogger.class, LoggingLogger.class.getPackage().getName());

    // id = 11500, value = "%s caught exception attempting to revert operation %s at address %s" -- now unused

    /**
     * Logs a warning message indicating an error occurred trying to set the property, represented by the
     * {@code propertyName} parameter, on the handler class, represented by the {@code className} parameter.
     *
     * @param cause        the cause of the error.
     * @param propertyName the name of the property.
     * @param className    the name of the class.
     */
    @LogMessage(level = WARN)
    @Message(id = 11501, value = "An error occurred trying to set the property '%s' on handler '%s'.")
    void errorSettingProperty(@Cause Throwable cause, String propertyName, String className);

    // id = 11502, value = "Removing bootstrap log handlers" -- now unused
    // id = 11503, value = "Restored bootstrap log handlers" -- now unused

    /**
     * Logs a warning message indicating an unknown property, represented by the {@code propertyName} parameter, for
     * the class represented by the {@code className} parameter.
     *
     * @param propertyName the name of the property.
     * @param className    the name of the class.
     */
    @LogMessage(level = WARN)
    @Message(id = 11504, value = "Unknown property '%s' for '%s'.")
    void unknownProperty(String propertyName, String className);

    /**
     * Logs an error message indicating to failure to close the resource represented by the {@code closeable}
     * parameter.
     *
     * @param cause     the cause of the error
     * @param closeable the resource
     */
    @LogMessage(level = ERROR)
    @Message(id = 11505, value = "Failed to close resource %s")
    void failedToCloseResource(@Cause Throwable cause, Closeable closeable);

    /**
     * Logs a warning message indicating the attribute, represented by the {@code name} parameter, is not a
     * configurable  property value.
     *
     * @param name the name of the attribute
     */
    @LogMessage(level = WARN)
    @Message(id = 11506, value = "The attribute %s could not be set as it is not a configurable property value.")
    void invalidPropertyAttribute(String name);

    /**
     * Logs a warning message indicating the path manager service has not been started and any changes may be lost as a
     * result of this.
     * <p/>
     * Essentially this means the {@code logging.properties} could not be written out and until the changes may have
     * been lost unless they were written to the logging subsystem in the XML configuration.
     */
    @LogMessage(level = WARN)
    @Message(id = 11507, value = "The path manager service does not appear to be started. Any changes may be lost as a result of this.")
    void pathManagerServiceNotStarted();

    /**
     * Logs a warning message indicating filters are not currently supported for log4j appenders.
     */
    @LogMessage(level = WARN)
    @Message(id = 11508, value = "Filters are not currently supported for log4j appenders.")
    void filterNotSupported();

    /**
     * Logs a warning message indicating the deployment specified a logging profile, but the logging profile was not
     * found.
     *
     * @param loggingProfile the logging profile that was not found
     * @param deployment     the deployment that specified the logging profile
     */
    @LogMessage(level = WARN)
    @Message(id = 11509, value = "Logging profile '%s' was specified for deployment '%s' but was not found. Using system logging configuration.")
    void loggingProfileNotFound(String loggingProfile, ResourceRoot deployment);

    /**
     * Logs a warning message indicating the configuration file found appears to be a {@link
     * java.util.logging.LogManager J.U.L.} configuration file and the log manager does not allow this configuration.
     *
     * @param fileName the configuration file name
     */
    @LogMessage(level = WARN)
    @Message(id = 11510, value = "The configuration file in '%s' appears to be a J.U.L. configuration file. The log manager does not allow this type of configuration file.")
    void julConfigurationFileFound(String fileName);
}
