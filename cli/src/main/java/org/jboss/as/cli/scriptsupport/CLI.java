/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.cli.scriptsupport;

import java.io.IOException;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.dmr.ModelNode;

/**
 * This class is intended to be used with JVM-based scripting languages.  It acts as a facade to the CLI public API,
 * providing a single class that can be used to connect, run CLI commands, and disconnect.  It also removes the need
 * to catch checked exceptions.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CLI {

    private CommandContext ctx;

    private CLI() {} // only allow new instances from newInstance() method.

    /**
     * Create a new CLI instance.
     *
     * @return The CLI instance.
     */
    public static CLI newInstance() {
        return new CLI();
    }

    private void checkAlreadyConnected() {
        if (ctx != null) throw new IllegalStateException("Already connected to server.");
    }

    private void checkNotConnected() {
        if (ctx == null) throw new IllegalStateException("Not connected to server.");
        if (ctx.isTerminated()) throw new IllegalStateException("Session is terminated.");
    }

    /**
     * Return the CLI CommandContext that was created when connected to the server.  This allows a script developer full
     * access to CLI facilities if needed.
     *
     * @return The CommandContext, or <code>null</code> if not connected.
     */
    public CommandContext getCommandContext() {
        return this.ctx;
    }

    /**
     * Connect to the server using the default host and port.
     */
    public void connect() {
        checkAlreadyConnected();
        try {
            ctx = CommandContextFactory.getInstance().newCommandContext();
            ctx.connectController();
        } catch (CliInitializationException e) {
            throw new IllegalStateException("Unable to initialize command context.", e);
        } catch (CommandLineException e) {
            throw new IllegalStateException("Unable to connect to controller.", e);
        }
    }

    /**
     * Connect to the server using the default host and port.
     *
     * @param username The user name for logging in.
     * @param password The password for logging in.
     */
    public void connect(String username, char[] password) {
        checkAlreadyConnected();
        try {
            ctx = CommandContextFactory.getInstance().newCommandContext(username, password);
            ctx.connectController();
        } catch (CliInitializationException e) {
            throw new IllegalStateException("Unable to initialize command context.", e);
        } catch (CommandLineException e) {
            throw new IllegalStateException("Unable to connect to controller.", e);
        }
    }

    /**
     * Connect to the server using a specified host and port.
     *
     * @param controllerHost The host name.
     * @param controllerPort The port.
     * @param username The user name for logging in.
     * @param password The password for logging in.
     */
    public void connect(String controllerHost, int controllerPort, String username, char[] password) {
        checkAlreadyConnected();
        try {
            ctx = CommandContextFactory.getInstance().newCommandContext(controllerHost, controllerPort, username, password);
            ctx.connectController();
        } catch (CliInitializationException e) {
            throw new IllegalStateException("Unable to initialize command context.", e);
        } catch (CommandLineException e) {
            throw new IllegalStateException("Unable to connect to controller.", e);
        }
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        try {
            checkNotConnected();
            ctx.disconnectController();
        } finally {
            ctx = null;
        }
    }

    /**
     * Execute a CLI command.  This can be any command that you might execute on the CLI command line, including both
     * server-side operations and local commands such as 'cd' or 'cn'.
     *
     * @param cliCommand A CLI command.
     * @return A result object that provides all information about the execution of the command.
     */
    public Result cmd(String cliCommand) {
        checkNotConnected();
        try {
            ModelNode request = ctx.buildRequest(cliCommand);
            ModelNode response = ctx.getModelControllerClient().execute(request);
            return new Result(cliCommand, request, response);
        } catch (CommandFormatException cfe) {
            // if the command can not be converted to a ModelNode, it might be a local command
            try {
                ctx.handle(cliCommand);
                return new Result(cliCommand, ctx.getExitCode());
            } catch (CommandLineException cle) {
                throw new IllegalArgumentException("Error handling command: " + cliCommand, cle);
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("Unable to send command " + cliCommand + " to server.", ioe);
        }
    }

    /**
     * The Result class provides all information about an executed CLI command.
     */
    public class Result {
        private String cliCommand;
        private ModelNode request;
        private ModelNode response;

        private boolean isSuccess = false;
        private boolean isLocalCommand = false;

        Result(String cliCommand, ModelNode request, ModelNode response) {
            this.cliCommand = cliCommand;
            this.request = request;
            this.response = response;
            this.isSuccess = response.get("outcome").asString().equals("success");
        }

        Result(String cliCommand, int exitCode) {
            this.cliCommand = cliCommand;
            this.isSuccess = exitCode == 0;
            this.isLocalCommand = true;
        }

        /**
         * Return the original command as a String.
         * @return The original CLI command.
         */
        public String getCliCommand() {
            return this.cliCommand;
        }

        /**
         * If the command resulted in a server-side operation, return the ModelNode representation of the operation.
         *
         * @return The request as a ModelNode, or <code>null</code> if this was a local command.
         */
        public ModelNode getRequest() {
            return this.request;
        }

        /**
         * If the command resulted in a server-side operation, return the ModelNode representation of the response.
         *
         * @return The server response as a ModelNode, or <code>null</code> if this was a local command.
         */
        public ModelNode getResponse() {
            return this.response;
        }

        /**
         * Return true if the command was successful.  For a server-side operation, this is determined by the outcome of the
         * operation on the server side.
         *
         * @return <code>true</code> if the command was successful, <code>false</code> otherwise.
         */
        public boolean isSuccess() {
            return this.isSuccess;
        }

        /**
         * Return true if the command was only executed locally and did not result in a server-side operation.
         *
         * @return <code>true</code> if the command was only executed locally, <code>false</code> otherwise.
         */
        public boolean isLocalCommand() {
            return this.isLocalCommand;
        }
    }
}
