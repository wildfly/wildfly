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
package org.jboss.as.cli.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandContextFactoryImpl extends CommandContextFactory {

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContextFactory#newCommandContext()
     */
    @Override
    public CommandContext newCommandContext() throws CliInitializationException {
        return new CommandContextImpl();
    }

    @Override
    public CommandContext newCommandContext(String username, char[] password)
            throws CliInitializationException {
        return new CommandContextImpl(username, password, username != null);
    }

    @Override
    public CommandContext newCommandContext(String controller, String username, char[] password)
            throws CliInitializationException {
        return newCommandContext(controller, username, password, false, -1);
    }

    @Override
    public CommandContext newCommandContext(String controller, String username, char[] password,
            boolean initConsole, final int connectionTimeout) throws CliInitializationException {
        return new CommandContextImpl(controller, username, password, username != null, initConsole, connectionTimeout);
    }

    @Override
    public CommandContext newCommandContext(String controller,
            String username, char[] password,
            InputStream consoleInput, OutputStream consoleOutput) throws CliInitializationException {
        return new CommandContextImpl(controller, username, password, username != null, consoleInput, consoleOutput);
    }

    @Override
    public CommandContext newCommandContext(String controller,
            String username, char[] password, boolean disableLocalAuth, boolean initConsole, int connectionTimeout)
            throws CliInitializationException {
        return new CommandContextImpl(controller, username, password, disableLocalAuth || username != null, initConsole, connectionTimeout);
    }

    @Override
    @Deprecated
    public CommandContext newCommandContext(String controllerHost, int controllerPort, String username, char[] password)
            throws CliInitializationException {
        try {
            return newCommandContext(new URI(null, null, controllerHost, controllerPort, null, null, null).toString().substring(2),
                    username, password);
        } catch (URISyntaxException e) {
            throw new CliInitializationException("Unable to construct URI for connection.", e);
        }
    }

    @Override
    @Deprecated
    public CommandContext newCommandContext(String controllerHost, int controllerPort, String username, char[] password,
            boolean initConsole, int connectionTimeout) throws CliInitializationException {
        try {
            return newCommandContext(new URI(null, null, controllerHost, controllerPort, null, null, null).toString().substring(2),
                    username, password, initConsole, connectionTimeout);
        } catch (URISyntaxException e) {
            throw new CliInitializationException("Unable to construct URI for connection.", e);
        }
    }

    @Override
    @Deprecated
    public CommandContext newCommandContext(String controllerHost, int controllerPort, String username, char[] password,
            InputStream consoleInput, OutputStream consoleOutput) throws CliInitializationException {
        try {
            return newCommandContext(new URI(null, null, controllerHost, controllerPort, null, null, null).toString().substring(2),
                    username, password, consoleInput, consoleOutput);
        } catch (URISyntaxException e) {
            throw new CliInitializationException("Unable to construct URI for connection.", e);
        }
    }
}
