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
        final CommandContextImpl cmdCtx = new CommandContextImpl();
        addShutdownHook(cmdCtx);
        return cmdCtx;
    }

    @Override
    public CommandContext newCommandContext(String username, char[] password)
            throws CliInitializationException {
        final CommandContextImpl cmdCtx = new CommandContextImpl(username, password);
        addShutdownHook(cmdCtx);
        return cmdCtx;
    }

    @Override
    public CommandContext newCommandContext(String controllerHost,
            int controllerPort, String username, char[] password)
            throws CliInitializationException {
        return newCommandContext(controllerHost, controllerPort, username, password, false);
    }

    @Override
    public CommandContext newCommandContext(String controllerHost,
            int controllerPort, String username, char[] password,
            boolean initConsole) throws CliInitializationException {
        final CommandContext ctx = new CommandContextImpl(controllerHost, controllerPort, username, password, initConsole);
        addShutdownHook(ctx);
        return ctx;
    }

    @Override
    public CommandContext newCommandContext(String controllerHost, int controllerPort,
            String username, char[] password,
            InputStream consoleInput, OutputStream consoleOutput) throws CliInitializationException {
        final CommandContext ctx = new CommandContextImpl(controllerHost, controllerPort, username, password, consoleInput, consoleOutput);
        addShutdownHook(ctx);
        return ctx;
    }

    protected void addShutdownHook(final CommandContext cmdCtx) {
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                cmdCtx.terminateSession();
            }
        }));
    }
}
