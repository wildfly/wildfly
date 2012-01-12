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
package org.jboss.as.jdr;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;
import static org.jboss.logging.Logger.Level.INFO;

/**
 * JBoss Diagnostic Reporter (JDR) logger.
 * Uses id's 14500 to 14599.
 *
 * @author Mike M. Clark
 */
@MessageLogger(projectCode = "JBAS")
interface JdrLogger extends BasicLogger {
    /**
     * A logger with the category of the default jdr package.
     */
    JdrLogger ROOT_LOGGER = Logger.getMessageLogger(JdrLogger.class, JdrLogger.class.getPackage().getName());

    /**
     * Indicates that a JDR report has been initiated.
     */
    @LogMessage(level = INFO)
    @Message(id=14500, value="Starting creation of a JBoss Diagnostic Report (JDR).")
    void startingCollection();

    /**
     * Indicates that a JDR report has completed
     */
    @LogMessage(level = INFO)
    @Message(id=14501, value="Completed creation of a JBoss Diagnostic Report (JDR).")
    void endingCollection();

    /**
     * Indicates that the JBoss home directory was not set.
     */
    @LogMessage(level = ERROR)
    @Message(id=14502, value="Unable to create JDR report, JBoss Home directory cannot be determined.")
    void jbossHomeNotSet();

    /**
     * The sosreport python library threw an exception
     */
    @LogMessage(level = WARN)
    @Message(id=14503, value="JDR python interpreter encountered an exception.")
    void pythonExceptionEncountered(@Cause Throwable cause);

    /**
     * JDR was unable to decode a path URL for standarization across platforms.
     */
    @LogMessage(level = WARN)
    @Message(id=14504, value="Unable to decode a url while creating JDR report.")
    void urlDecodeExceptionEncountered(@Cause Throwable cause);

    /**
     * JDR was unable to decode a path URL for standarization across platforms.
     */
    @LogMessage(level = WARN)
    @Message(id=14505, value="Plugin contrib location is not a directory.  Ignoring.")
    void contribNotADirectory();
}
