/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.logger;


import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.*;

/**
 * JBoss Diagnostic Reporter (JDR) logger.
 *
 * @author Mike M. Clark
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYJDR", length = 4)
public interface JdrLogger extends BasicLogger {
    /**
     * A logger with the category of the default jdr package.
     */
    JdrLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), JdrLogger.class, "org.jboss.as.jdr");

//    /**
//     * Indicates that a JDR report has been initiated.
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 1, value = "Starting creation of a JBoss Diagnostic Report (JDR).")
//    void startingCollection();
//
//    /**
//     * Indicates that a JDR report has completed
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 2, value = "Completed creation of a JBoss Diagnostic Report (JDR).")
//    void endingCollection();
//
//    /**
//     * Indicates that the JBoss home directory was not set.
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 3, value = "Unable to create JDR report, JBoss Home directory cannot be determined.")
//    void jbossHomeNotSet();
//
//    /**
//     * The sosreport python library threw an exception
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 4, value = "JDR python interpreter encountered an exception.")
//    void pythonExceptionEncountered(@Cause Throwable cause);
//
//    /**
//     * JDR was unable to decode a path URL for standardization across platforms.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 5, value = "Unable to decode a url while creating JDR report.")
//    void urlDecodeExceptionEncountered(@Cause Throwable cause);
//
//    /**
//     * JDR plugin location is not a directory as expected.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 6, value = "Plugin contrib location is not a directory.  Ignoring.")
//    void contribNotADirectory();

    /**
     * JDR could not create a zipfile to store the report.
     */
    @Message(id = 7, value="Could not create zipfile.")
    String couldNotCreateZipfile();

    /**
     * One of the configuration steps in JDR threw an exception.
     */
    @Message(id = 8, value="Could not configure JDR. At least one configuration step failed.")
    String couldNotConfigureJDR();

    /**
     * No Commands to run, probably no valid plugin loaded
     */
    @Message(id = 9, value = "No JDR commands were loaded. Be sure that a valid Plugin class is specified in plugins.properties.")
    String noCommandsToRun();

//    /**
//     * Indicates an invalid, <code>null</code> argument was
//     * passed into a method.
//     *
//     * @param var method variable that was <code>null</code>
//     * @return Exception describing the invalid parameter.
//     */
//    @Message(id = 10, value = "Parameter %s may not be null.")
//    IllegalArgumentException varNull(String var);

    /**
     * Standalone property directory could not be located which is needed to find/create the JDR properties file.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11, value = "Could not find JDR properties file.")
    void couldNotFindJDRPropertiesFile();

    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Could not create JDR properties file at %s")
    void couldNotCreateJDRPropertiesFile(@Cause IOException ioex, Path path);

    @Message(id = Message.NONE, value = "Display this message and exit")
    String jdrHelpMessage();

    @Message(id = Message.NONE, value = "hostname that the management api is bound to. (default: localhost)")
    String jdrHostnameMessage();

    @Message(id = Message.NONE, value = "port that the management api is bound to. (default: 9990)")
    String jdrPortMessage();

    @Message(id = Message.NONE, value = "Protocol that is used to connect. Can be remote, http or https (default: http)")
    String jdrProtocolMessage();

    @Message(id = Message.NONE, value = "Configuration file of the server if it is not running.")
    String jdrConfigMessage();

    @Message(id = Message.NONE, value = "JBoss Diagnostic Reporter (JDR) is a subsystem built to collect information to aid in troubleshooting. The jdr script is a utility for generating JDR reports.")
    String jdrDescriptionMessage();
}
