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

package org.jboss.as.appclient.logging;

import static org.jboss.logging.Logger.Level.ERROR;

import java.io.File;
import java.net.URL;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYAC", length = 4)
public interface AppClientLogger extends BasicLogger {

    /**
     * The root logger.
     */
    AppClientLogger ROOT_LOGGER = Logger.getMessageLogger(AppClientLogger.class, "org.jboss.as.appclient");

//    /**
//     * Logs a generic error message using the {@link Throwable#toString() t.toString()} for the error message.
//     *
//     * @param cause the cause of the error.
//     * @param t     the cause to use for the log message.
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 1, value = "%s")
//    void caughtException(@Cause Throwable cause, Throwable t);

    /**
     * Logs an error message indicating there was an error running the app client.
     *
     * @param cause         the cause of the error.
     * @param exceptionName the exception name thrown.
     */
    @LogMessage(level = ERROR)
    @Message(id = 2, value = "%s running app client main")
    void exceptionRunningAppClient(@Cause Throwable cause, String exceptionName);


//    /**
//     *
//     * @param cause         the cause of the error.
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 3, value = "Error closing connection")
//    void exceptionClosingConnection(@Cause Throwable cause);

    @Message(id = Message.NONE, value = "Name of the app client configuration file to use (default is \"appclient.xml\")")
    String argAppClientConfig();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#HELP} command line arguments.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Display this message and exit")
    String argHelp();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#HOST} command line arguments.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Set the url of the application server instance to connect to")
    String argHost();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#CONNECTION_PROPERTIES} command line
     * arguments.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Load ejb-client.properties file from the given url")
    String argConnectionProperties();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#PROPERTIES} command line
     * arguments.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Load system properties from the given url")
    String argProperties();

    /**
     * Instructions for {@link org.jboss.as.process.CommandLineConstants#SYS_PROP} command line argument.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Set a system property")
    String argSystemProperty();

//    /**
//     * Instructions for the usage of the command line arguments instructions.
//     *
//     * @return the instructions.
//     */
//    @Message(id = Message.NONE, value = "Usage: ./appclient.sh [args...] myear.ear#appClient.jar [client args...]%n%nwhere args include:%n")
//    String argUsage();

    /**
     * Instructions for {@link org.jboss.as.process.CommandLineConstants#VERSION} command line argument.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Print version and exit")
    String argVersion();

    /**
     * Instructions for {@link org.jboss.as.process.CommandLineConstants#VERSION} command line argument.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Runs the container with the security manager enabled.")
    String argSecMgr();

    /**
     * A general description of the appclient usage.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "The appclient script starts an application client which can be used to test and access the deployed EJBs.")
    String usageDescription();

    /**
     * A message indicating that you must specify an application client to execute.
     *
     * @return the message.
     */
    @Message(id = 4, value = "You must specify the application client to execute")
    String appClientNotSpecified();

    /**
     * A message indicating the argument, represented by the {@code arg} parameter, expected an additional argument.
     *
     * @param arg the argument that expects an additional argument.
     *
     * @return the message.
     */
    @Message(id = 5, value = "Argument expected for option %s")
    String argumentExpected(String arg);

    /**
     * Creates an exception indicating the application client could not be found to start.
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 6, value = "Could not find application client jar in deployment")
    RuntimeException cannotFindAppClient();

    /**
     * Creates an exception indicating that the application client, represented by the {@code deploymentName}, could
     * not be found.
     *
     * @param deploymentName the name of the deployment.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 7, value = "Could not find application client %s")
    DeploymentUnitProcessingException cannotFindAppClient(String deploymentName);

    /**
     * Creates an exception indicating that the application client could not load the main class.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 8, value = "Could not load application client main class")
    RuntimeException cannotLoadAppClientMainClass(@Cause Throwable cause);

//    /**
//     * Creates an exception indicating the component class could not be loaded.
//     *
//     * @param cause the cause of the error.
//     *
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 9, value = "Could not load component class")
//    DeploymentUnitProcessingException cannotLoadComponentClass(@Cause Throwable cause);

    /**
     * A message indicating the properties could not be loaded from the URL.
     *
     * @param url the url to the properties.
     *
     * @return the message.
     */
    @Message(id = 10, value = "Unable to load properties from URL %s")
    String cannotLoadProperties(URL url);

    /**
     * Creates an exception indicating the app client could not start due to no main class being found.
     *
     * @param deploymentName the deployment name.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11, value = "Could not start app client %s as no main class was found")
    RuntimeException cannotStartAppClient(String deploymentName);

    /**
     * Creates an exception indicating the app client could not start due to the main method missing on the main class.
     *
     * @param deploymentName the deployment name.
     * @param mainClass      the main class defined.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 12, value = "Could not start app client %s as no main method was found on main class %s")
    RuntimeException cannotStartAppClient(String deploymentName, Class<?> mainClass);

    /**
     * Creates an exception indicating the subsystem declaration has been duplicated.
     *
     * @param location the location of the error for the constructor of th exception.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 13, value = "Duplicate subsystem declaration")
    XMLStreamException duplicateSubsystemDeclaration(@Param Location location);

//    /**
//     * Creates an exception indicating the element, represented by the {@code elementName} parameter, has already been
//     * declared.
//     *
//     * @param elementName the element name.
//     * @param value       the value.
//     * @param location    the location used in the constructor of the exception.
//     *
//     * @return a {@link XMLStreamException} for the error.
//     */
//    @Message(id = 14, value = "%s %s already declared")
//    XMLStreamException elementAlreadyDeclared(String elementName, Object value, @Param Location location);

    /**
     * Creates an exception indicating a failure to parse the xml file represented by the {@code appXml} parameter.
     *
     * @param cause  the cause of the error.
     * @param appXml the file that failed to be parsed.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 15, value = "Failed to parse %s")
    DeploymentUnitProcessingException failedToParseXml(@Cause Throwable cause, VirtualFile appXml);

    /**
     * Creates an exception indicating a failure to parse the xml file represented by the {@code appXml} parameter.
     *
     * @param appXml       the file that failed to be parsed.
     * @param lineNumber   the line the failure occurred on.
     * @param columnNumber the column the failure occurred on.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 16, value = "Failed to parse %s at [%d,%d]")
    DeploymentUnitProcessingException failedToParseXml(@Cause Throwable cause, VirtualFile appXml, int lineNumber, int columnNumber);

    /**
     * A message indicating the URL in the argument was malformed.
     *
     * @param arg the invalid argument.
     *
     * @return the message.
     */
    @Message(id = 17, value = "Malformed URL provided for option %s")
    String malformedUrl(String arg);

    /**
     * Creates an exception indicating that more than one application client was found and not app client name was
     * specified.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 18, value = "More than one application client found and no app client name specified")
    RuntimeException multipleAppClientsFound();

//    /**
//     * Creates an exception indicating the model contains multiple nodes represented by the {@code nodeName} parameter.
//     *
//     * @param nodeName the name of the node.
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 19, value = "Model contains multiple %s nodes")
//    IllegalStateException multipleNodesFound(String nodeName);

    /**
     * A message indicating a known option.
     *
     * @param option the unknown option.
     *
     * @return the message.
     */
    @Message(id = 20, value = "Unknown option %s")
    String unknownOption(String option);

    /**
     * A message indicating the callback handler could not be loaded
     *
     */
    @Message(id = 21, value = "Could not load callback-handler class %s")
    DeploymentUnitProcessingException couldNotLoadCallbackClass(String clazz);

    /**
     * A message indicating the callback handler could not be instantiated
     *
     */
    @Message(id = 22, value = "Could not create instance of callback-handler class %s")
    DeploymentUnitProcessingException couldNotCreateCallbackHandler(String clazz);

    /**
     * Creates an exception indicating that the application client, represented by the {@code deploymentName}, could
     * not be found.
     *
     * @param deploymentName the name of the deployment.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 23, value = "Could not find application client %s")
    RuntimeException cannotFindAppClientFile(File deploymentName);

    @Message(id = 24, value = "Cannot specify both a host to connect to and an ejb-client.properties file. ")
    RuntimeException cannotSpecifyBothHostAndPropertiesFile();

//    /**
//     * The ejb-client.properties could not be loaded
//     */
//    @Message(id = 25, value = "Unable to load ejb-client.properties URL: %s ")
//    DeploymentUnitProcessingException exceptionLoadingEjbClientPropertiesURL(final String file, @Cause Throwable cause);

}
