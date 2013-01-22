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


/**
 * This interface represents the JBoss CLI configuration.
 *
 * @author Alexey Loubyansky
 */
public interface CliConfig {

    /**
     * The default server controller host to connect to.
     *
     * @return default server controller host to connect to
     */
    String getDefaultControllerHost();

    /**
     * The default server controller port to connect to.
     *
     * @return  default server controller port to connect to
     */
    int getDefaultControllerPort();

    /**
     * Whether the record the history of executed commands and operations.
     *
     * @return  true if the history is enabled, false - otherwise.
     */
    boolean isHistoryEnabled();

    /**
     * The name of the command and operation history file.
     *
     * @return  name of the command and operation history file
     */
    String getHistoryFileName();

    /**
     * The directory which contains the command and operation history file.
     *
     * @return  directory which contains the command and operation history file.
     */
    String getHistoryFileDir();

    /**
     * Maximum size of the history log.
     *
     * @return maximum size of the history log
     */
    int getHistoryMaxSize();

    /**
     * Connection timeout period in milliseconds.
     *
     * @return connection timeout in milliseconds
     */
    int getConnectionTimeout();

    /**
     * The global SSL configuration if it has been defined.
     *
     * @return The SSLConfig
     */
    SSLConfig getSslConfig();

    /**
     * Whether the operation requests should be validated in terms of
     * addresses, operation names and parameters before they are
     * sent to the controller for execution.
     *
     * @return  true is the operation requests should be validated, false - otherwise.
     */
    boolean isValidateOperationRequests();

    /**
     * Whether to resolve system properties specified as command argument
     * (or operation parameter) values before sending the operation request
     * to the controller or let the resolution happen on the server side.
     * If the method returns true, the resolution should be performed by the CLI,
     * otherwise - on the server side.
     *
     * @return  true if the system properties specified as operation parameter
     * values should be resolved by the CLI, false if the resolution
     * of the parameter values should happen on the server side.
     */
    boolean isResolveParameterValues();

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
     * @return
     */
    boolean isSilent();
}
