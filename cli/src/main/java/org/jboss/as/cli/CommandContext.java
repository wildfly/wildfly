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
     * Returns the current command's arguments as a string.
     * @return current command's arguments as a string or null if the command was entered w/o arguments.
     */
    String getCommandArguments();

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
     * Connects with the controller client using the host and the port.
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
}
