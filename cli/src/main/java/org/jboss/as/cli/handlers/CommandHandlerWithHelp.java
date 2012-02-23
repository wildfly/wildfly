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
package org.jboss.as.cli.handlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.protocol.StreamUtils;

/**
 * Abstract handler that checks whether the argument is '--help', in which case it
 * tries to locate file [cmd].txt and print its content. If the argument
 * is absent or isn't '--help', it'll call doHandle(ctx) method.
 *
 * @author Alexey Loubyansky
 */
public abstract class CommandHandlerWithHelp extends CommandHandlerWithArguments {

    private final String filename;
    private final boolean connectionRequired;
    protected ArgumentWithoutValue helpArg = new ArgumentWithoutValue(this, "--help", "-h");

    public CommandHandlerWithHelp(String command) {
        this(command, false);
    }

    public CommandHandlerWithHelp(String command, boolean connectionRequired) {
        if(command == null) {
            throw new IllegalArgumentException("command can't be null");
        }
        this.filename = "help/" + command + ".txt";
        this.connectionRequired = connectionRequired;
        this.helpArg.setExclusive(true);
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        if(connectionRequired && ctx.getModelControllerClient() == null) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandHandler#handle(org.jboss.as.cli.CommandContext)
     */
    @Override
    public void handle(CommandContext ctx) throws CommandLineException {

        if(helpArg.isPresent(ctx.getParsedCommandLine())) {
            printHelp(ctx);
            return;
        }

        if(!isAvailable(ctx)) {
            throw new CommandFormatException("The command is not available in the current context (e.g. required subsystems or connection to the controller might be unavailable).");
        }

        doHandle(ctx);
    }

    protected void printHelp(CommandContext ctx) throws CommandFormatException {
        InputStream helpInput = SecurityActions.getClassLoader(CommandHandlerWithHelp.class).getResourceAsStream(filename);
        if(helpInput != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(helpInput));
            try {
                String helpLine = reader.readLine();
                while(helpLine != null) {
                    ctx.printLine(helpLine);
                    helpLine = reader.readLine();
                }
            } catch(java.io.IOException e) {
                throw new CommandFormatException ("Failed to read help/help.txt: " + e.getLocalizedMessage());
            } finally {
                StreamUtils.safeClose(reader);
            }
        } else {
            throw new CommandFormatException("Failed to locate command description " + filename);
        }
    }

    protected abstract void doHandle(CommandContext ctx) throws CommandLineException;

    @Override
    public boolean isBatchMode(CommandContext ctx) {
        return false;
    }

    /**
     * Prints a list of strings. If -l switch is present then the list is printed one item per line,
     * otherwise the list is printed in columns.
     * @param ctx  the context
     * @param list  the list to print
     */
    protected void printList(CommandContext ctx, List<String> list, boolean l) {
        if(l) {
            for(String item : list) {
                ctx.printLine(item);
            }
        } else {
            ctx.printColumns(list);
        }
    }
}
