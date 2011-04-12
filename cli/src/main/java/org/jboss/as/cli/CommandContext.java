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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.PrefixFormatter;
import org.jboss.as.controller.client.ModelControllerClient;



/**
 *
 * @author Alexey Loubyansky
 */
public interface CommandContext {

    /**
     * Returns the current command.
     * @return the current command.
     */
    String getCommand();

    /**
     * Returns the current command's arguments as a string.
     * @return current command's arguments as a string or null if the command was entered w/o arguments.
     */
    String getCommandArguments();

    /**
     * Checks whether there are arguments on the command line for the current command.
     * @return true if there are arguments, false if there aren't.
     */
    boolean hasArguments();

    /**
     * Checks whether the switch is present among the command arguments.
     * @return
     */
    boolean hasSwitch(String switchName);

    /**
     * Returns a value for the named argument on the command line or
     * null if the argument with the name isn't present.
     * @param argName  the name of the argument
     * @return  the value of the argument or null if the argument isn't present
     */
    String getNamedArgument(String argName);

    /**
     * Returns a set of argument names present on the command line
     * of an empty set if there no named arguments on the command line.
     *
     * @return  a set of argument names present on the command line
     * of an empty set if there no named arguments on the command line
     */
    Set<String> getArgumentNames();

    /**
     * Returns arguments that are not switches as a list of strings
     * in the order they appear on the command line. If there no such arguments
     * an empty list is returned.
     * @return a list of arguments that are not switches or an empty list
     * if there are no such arguments.
     */
    List<String> getArguments();

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
     * Terminates the command line session.
     */
    void terminateSession();

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
     */
    void connectController(String host, int port);

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
    OperationRequestParser getOperationRequestParser();

    /**
     * Returns the current prefix.
     * @return current prefix
     */
    OperationRequestAddress getPrefix();

    /**
     * Returns the prefix formatter.
     * @return the prefix formatter.
     */
    PrefixFormatter getPrefixFormatter();

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
     * Starts batch mode.
     * If name argument is null then if there was a previously held back unnamed batch, it is activated.
     * If the name argument is null and there is no previously held back unnamed batch, a new batch is started.
     * Otherwise, the name should be a name of an existing batch which had been held back previously,
     * this batch becomes active and is removed from the storage. It can still be held back under the same
     * or a different name, or w/o a name.
     * The method returns true if the batch was successfully started or previously held back batch activated,
     * otherwise (if already in the batch mode) - false;
     */
    boolean startBatch(String name);

    /**
     * Discards the current or a named batch w/o executing it.
     * If invoked in the batch mode, the current batch will be discarded.
     * If invoked not in the batch mode the held back (named or not, depending on the argument)
     * batch will be discarded.
     * Returns true if the batch was discarded, false is returned if the batch with the given
     * name didn't exist.
     * @param name  the name of the batch to discard or null for the current or a batch w/o a name.
     */
    boolean discardBatch(String name);

    /**
     * Hold back the current batch and quit the batch mode. If the argument is not null,
     * the batch will be saved under the specified name. If the argument is null,
     * the batch will be saved w/o a name.
     * The method returns true if the batch was successfully saved. False is returned if there
     * already is a held back batch with the given name (or unnamed if the name argument is null).
     * @param name  the name under which to save the batch or null if the batch should be saved w/o a name.
     */
    boolean holdbackBatch(String name);

    /**
     * If in the batch mode, runs the current batch. If not in the batch mode,
     * runs the batch with the given name if the argument is not null or the batch
     * w/o a name if the argument is null.
     * The method returns true if the batch with the given name (or no name if the argument is null)
     * existed, otherwise false is returned.
     * @param name  the name of the batch to run or null if the batch doesn't have a name.
     */
    boolean runBatch(String name);

    /**
     * Returns the current batch or null if not in the batch mode.
     * @return the current batch or null if not in the batch mode.
     */
    List<String> getCurrentBatch();
}
