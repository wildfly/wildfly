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
package org.jboss.as.cli;

import java.io.File;
import java.util.Collection;

import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.NodePathFormatter;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;


/**
 *
 * @author Alexey Loubyansky
 */
public interface CommandContext {

    /**
     * Returns the JBoss CLI configuration.
     * @return  CLI configuration
     */
    CliConfig getConfig();

    /**
     * Returns the current command's arguments as a string.
     * @return current command's arguments as a string or null if the command was entered w/o arguments.
     */
    String getArgumentsString();

    /**
     * Parsed command line arguments.
     * @return  parsed command line arguments.
     */
    ParsedCommandLine getParsedCommandLine();

    /**
     * Prints a string to the CLI's output.
     * @param message the message to print
     */
    void printLine(String message);

    /**
     * Prints a collection of strings as columns to the CLI's output.
     * @param col  the collection of strings to print as columns.
     */
    void printColumns(Collection<String> col);

    /**
     * Clears the screen.
     */
    void clearScreen();

    /**
     * Terminates the command line session.
     * Also closes the connection to the controller if it's still open.
     */
    void terminateSession();

    /**
     * Checks whether the session has been terminated.
     * @return
     */
    boolean isTerminated();

    /**
     * Associates an object with key. The mapping is valid until this method is called with the same key value
     * and null as the new value for this key.
     * @param key the key
     * @param value the value to be associated with the key
     */
    void set(String key, Object value);

    /**
     * Returns the value the key was associated with using the set(key, value) method above.
     * @param key the key to fetch the value for
     * @return the value associated with the key or null, if the key wasn't associated with any non-null value.
     */
    Object get(String key);

    /**
     * Removes the value the key was associated with using the set(key, value) method above.
     * If the key isn't associated with any value, the method will return null.
     * @param key the key to be removed
     * @return the value associated with the key or null, if the key wasn't associated with any non-null value.
     */
    Object remove(String key);

    /**
     * Returns the model controller client or null if it hasn't been initialized.
     * @return the model controller client or null if it hasn't been initialized.
     */
    ModelControllerClient getModelControllerClient();

    /**
     * Connects the controller client using the host and the port.
     * If the host is null, the default controller host will be used,
     * which is localhost.
     * If the port is less than zero, the default controller port will be used,
     * which is 9999.
     *
     * @param host the host to connect with
     * @param port the port to connect on
     * @throws CommandLineException  in case the attempt to connect failed
     */
    void connectController(String host, int port) throws CommandLineException;

    /**
     * Bind the controller to an existing, connected client.
     */
    void bindClient(ModelControllerClient newClient);

    /**
     * Connects the controller client using the default host and the port.
     * It simply calls connectController(null, -1).
     *
     * @throws CommandLineException  in case the attempt to connect failed
     */
    void connectController() throws CommandLineException;

    /**
     * Closes the previously established connection with the controller client.
     * If the connection hasn't been established, the method silently returns.
     */
    void disconnectController();

    /**
     * Returns the default host the controller client will be connected to
     * in case the host argument isn't specified.
     *
     * @return  the default host the controller client will be connected to
     * in case the host argument isn't specified.
     */
    String getDefaultControllerHost();

    /**
     * Returns the default port the controller client will be connected to
     * in case the port argument isn't specified.
     *
     * @return  the default port the controller client will be connected to
     * in case the port argument isn't specified.
     */
    int getDefaultControllerPort();

    /**
     * Returns the host the controller client is connected to or
     * null if the connection hasn't been established yet.
     *
     * @return  the host the controller client is connected to or
     * null if the connection hasn't been established yet.
     */
    String getControllerHost();

    /**
     * Returns the port the controller client is connected to.
     *
     * @return  the port the controller client is connected to.
     */
    int getControllerPort();

    /**
     * Returns the current operation request parser.
     * @return  current operation request parser.
     */
    CommandLineParser getCommandLineParser();

    /**
     * Returns the current prefix.
     * @return current prefix
     */
    OperationRequestAddress getCurrentNodePath();

    /**
     * Returns the prefix formatter.
     * @return the prefix formatter.
     */
    NodePathFormatter getNodePathFormatter();

    /**
     * Returns the provider of operation request candidates for tab-completion.
     * @return provider of operation request candidates for tab-completion.
     */
    OperationCandidatesProvider getOperationCandidatesProvider();

    /**
     * Returns the history of all the commands and operations.
     * @return  the history of all the commands and operations.
     */
    CommandHistory getHistory();

    /**
     * Checks whether the CLI is in the batch mode.
     * @return true if the CLI is in the batch mode, false - otherwise.
     */
    boolean isBatchMode();

    /**
     * Returns batch manager.
     * @return batch manager
     */
    BatchManager getBatchManager();

    /**
     * Builds an operation request from the passed in command line.
     * If the line contains a command, the command must supported the batch mode,
     * otherwise an exception will thrown.
     *
     * @param line the command line which can be an operation request or a command that can be translated into an operation request.
     * @return  the operation request
     * @throws CommandFormatException  if the operation request couldn't be built.
     */
    BatchedCommand toBatchedCommand(String line) throws CommandFormatException;

    /**
     * Builds a DMR request corresponding to the command or the operation.
     * If the line contains a command, the corresponding command handler
     * must implement org.jboss.cli.OperationCommand interface,
     * in other words the command must translate into an operation request,
     * otherwise an exception will be thrown.
     *
     * @param line  command or an operation to build a DMR request for
     * @return  DMR request corresponding to the line
     * @throws CommandFormatException  thrown in case the line couldn't be
     * translated into a DMR request
     */
    ModelNode buildRequest(String line) throws CommandFormatException;

    /**
     * Returns the default command line completer.
     * @return  the default command line completer.
     */
    CommandLineCompleter getDefaultCommandCompleter();

    /**
     * Indicates whether the CLI is in the domain mode or standalone one (assuming established
     * connection to the controller).
     * @return  true if the CLI is connected to the domain controller, otherwise false.
     */
    boolean isDomainMode();

    /**
     * Adds a listener for CLI events.
     * @param listener  the listener
     */
    void addEventListener(CliEventListener listener);

    /**
     * Returns value that should be used as the exit code of the JVM process.
     * @return  JVM exit code
     */
    int getExitCode();

    /**
     * Executes a command or an operation. Or, if the context is in the batch mode
     * and the command is allowed in the batch, adds the command (or the operation)
     * to the currently active batch.
     * NOTE: errors are not handled by this method, they won't affect the exit code or
     * even be logged. Error handling is the responsibility of the caller.
     *
     * @param line  command or operation to handle
     * @throws CommandFormatException  in case there was an error handling the command or operation
     */
    void handle(String line) throws CommandLineException;

    /**
     * Executes a command or an operation. Or, if the context is in the batch mode
     * and the command is allowed in the batch, adds the command (or the operation)
     * to the currently active batch.
     * NOTE: unlike handle(String line), this method catches CommandLineException
     * exceptions thrown by command handlers, logs them and sets the exit code
     * status to indicate that the command or the operation has failed.
     * It's up to the caller to check the exit code with getExitCode()
     * to find out whether the command or the operation succeeded or failed.
     *
     * @param line  command or operation to handle
     * @throws CommandFormatException  in case there was an error handling the command or operation
     */
    void handleSafe(String line);

    /**
     * This method will start an interactive session.
     * It requires an initialized at the construction time console.
     */
    void interact();

    /**
     * Returns current default filesystem directory.
     * @return  current default filesystem directory.
     */
    File getCurrentDir();

    /**
     * Changes the current default filesystem directory to the argument.
     * @param dir  the new default directory
     */
    void setCurrentDir(File dir);

    /**
     * Command argument or operation parameter values may contain system properties.
     * If this method returns true then the CLI will try to resolve
     * the system properties before sending the operation request to the controller.
     * Otherwise, the resolution will happen on the server side.
     *
     * @return true if system properties in the operation parameter values
     * should be resolved by the CLI before the request is sent to the controller,
     * false if system properties should be resolved on the server side.
     */
    boolean isResolveParameterValues();

    /**
     * Command argument or operation parameter values may contain system properties.
     * If this property is set to true then the CLI will try to resolve
     * the system properties before sending the operation request to the controller.
     * Otherwise, the resolution will happen on the server side.
     *
     * @param resolve  true if system properties in the operation parameter values
     * should be resolved by the CLI before the request is sent to the controller,
     * false if system properties should be resolved on the server side.
     */
    void setResolveParameterValues(boolean resolve);

    /**
     * Whether the info or error messages should be written to the terminal output.
     *
     * The output of the info and error messages is done in the following way:
     * 1) the message is always logged using a logger
     *    (which is disabled in the config by default);
     * 2) if the output target was specified on the command line using '>'
     *    it would be used;
     * 3) if the output target was not specified, whether the message is
     *    written or not to the terminal output will depend on
     *    whether it's a silent mode or not.
     *
     * @return  true if the CLI is in the silent mode, i.e. not writing info
     *          and error messages to the terminal output, otherwise - false.
     */
    boolean isSilent();

    /**
     * Enables of disables the silent mode.
     *
     * @param silent  true if the CLI should go into the silent mode,
     *                false if the CLI should resume writing info
     *                and error messages to the terminal output.
     */
    void setSilent(boolean silent);

    /**
     * Returns the current terminal window width in case the console
     * has been initialized. Otherwise -1.
     *
     * @return  current terminal with if the console has been initialized,
     *          -1 otherwise
     */
    int getTerminalWidth();

    /**
     * Returns the current terminal window height in case the console
     * has been initialized. Otherwise -1.
     *
     * @return  current terminal height if the console has been initialized,
     *          -1 otherwise
     */
    int getTerminalHeight();
}
