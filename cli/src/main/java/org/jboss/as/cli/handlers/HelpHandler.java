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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.impl.ArgumentWithoutValue;

/**
 * Help command handler. Reads 'help/help.txt' and prints its content to the output stream.
 *
 * @author Alexey Loubyansky
 */
public class HelpHandler extends CommandHandlerWithHelp {

    private final CommandRegistry cmdRegistry;
    private final ArgumentWithoutValue commands = new ArgumentWithoutValue(this, "--commands");

    public HelpHandler(CommandRegistry cmdRegistry) {
        this("help", cmdRegistry);
    }

    public HelpHandler(String command, CommandRegistry cmdRegistry) {
        super(command);
        if(cmdRegistry == null) {
            throw new IllegalArgumentException("CommandRegistry is null");
        }
        this.cmdRegistry = cmdRegistry;
        // trick to disable the help arg
        helpArg.setExclusive(false);
        helpArg.addCantAppearAfter(commands);
        helpArg.addRequiredPreceding(commands);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandHandler#handle(org.jboss.as.cli.Context)
     */
    @Override
    public void handle(CommandContext ctx) throws CommandLineException {
        boolean printCommands;
        try {
            printCommands = commands.isPresent(ctx.getParsedCommandLine());
        } catch (CommandFormatException e) {
            throw new CommandFormatException(e.getLocalizedMessage());
        }

        if(printCommands) {
            final List<String> commands = new ArrayList<String>();
            for(String cmd : cmdRegistry.getTabCompletionCommands()) {
                CommandHandler handler = cmdRegistry.getCommandHandler(cmd);
                if(handler.isAvailable(ctx)) {
                    commands.add(cmd);
                }
            }
            Collections.sort(commands);

            ctx.printLine("Commands available in the current context:");
            ctx.printColumns(commands);
            ctx.printLine("To read a description of a specific command execute 'command_name --help'.");
        } else {
            printHelp(ctx);
        }
    }

    @Override
    protected void doHandle(CommandContext ctx) {
    }
}