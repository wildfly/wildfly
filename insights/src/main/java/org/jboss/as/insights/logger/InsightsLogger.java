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
package org.jboss.as.insights.logger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.*;

/**
 * Insights logger
 *
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
@MessageLogger(projectCode = "WFLYTELEMTRY", length = 4)
public interface InsightsLogger extends BasicLogger {
    /**
     * A logger with the category of the default insights package.
     */
    InsightsLogger ROOT_LOGGER = Logger.getMessageLogger(
            InsightsLogger.class, "org.jboss.as.insights.extension");

    /**
     * Insights could not create JDR.
     */
    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Could not generate JDR.")
    void couldNotGenerateJdr(@Cause Throwable cause);

    /**
     * Insights thread incurred an InterruptedException.
     */
    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Insights thread has been interrupted.")
    void threadInterrupted(@Cause Throwable cause);

    /**
     * Frequency was updated to given value
     *
     * @param var
     *            new frequency value
     */
    @LogMessage(level = INFO)
    @Message(id = 3, value = "Frequency of Insights Subsystem updated to %s")
    void frequencyUpdated(String var);

    /**
     * System was enabled
     */
    @LogMessage(level = INFO)
    @Message(id = 4, value = "Insights Subsystem Enabled")
    void insightsEnabled();

    /**
     * System was enabled
     */
    @LogMessage(level = INFO)
    @Message(id = 5, value = "Insights Subsystem Disabled")
    void insightsDisabled();

    /**
     * Could not update/create insights.properties
     */
    @LogMessage(level = ERROR)
    @Message(id = 6, value = "Could not create/update insights.properties file with given username and password.")
    void couldNotCreateEditPropertiesFile(@Cause Throwable cause);

    /**
     * Could not close insights.properties
     */
    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Could not close insights.properties file.")
    void couldNotClosePropertiesFile(@Cause Throwable cause);

    /**
     * Could not load insights.properties
     */
    @LogMessage(level = ERROR)
    @Message(id = 8, value = "Could not load insights.properties file.")
    void couldNotLoadPropertiesFile(@Cause Throwable cause);

    /**
     * Could not load insights.properties
     */
    @LogMessage(level = ERROR)
    @Message(id = 9, value = "Could not register system with Insights")
    void couldNotRegisterSystem(@Cause Throwable cause);

    /**
     * Could not upload JDR
     */
    @LogMessage(level = ERROR)
    @Message(id = 10, value = "Could not upload JDR")
    void couldNotUploadJdr(@Cause Throwable cause);

    /**
     * Could not find JDR file
     */
    @LogMessage(level = ERROR)
    @Message(id = 11, value = "Could not find JDR file")
    void couldNotFindJdr(@Cause Throwable cause);

    /**
     * Could not start thread
     */
    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Could not start Insights thread")
    void couldNotStartThread(@Cause Throwable cause);

    /**
     * Could not find system
     */
    @LogMessage(level = ERROR)
    @Message(id = 13, value = "Could not find system in Insights")
    void couldNotFindSystem(@Cause Throwable cause);

    /**
     * Jdr was sent successfully
     */
    @LogMessage(level = INFO)
    @Message(id = 14, value = "Jdr sent successfully")
    void jdrSent();

    /**
     * Could not write frequency to properties file
     */
    @LogMessage(level = ERROR)
    @Message(id = 15, value = "Could not write frequency to properties file")
    void couldNotWriteFrequency(@Cause Throwable cause);

}