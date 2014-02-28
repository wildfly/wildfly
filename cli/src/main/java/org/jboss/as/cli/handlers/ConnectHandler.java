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


import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 * Connect handler.
 *
 * @author Alexey Loubyansky
 */
public class ConnectHandler extends CommandHandlerWithHelp {

    private final ArgumentWithValue arg = new ArgumentWithValue(this, 0, "--controller");

    public ConnectHandler() {
        this("connect");
    }

    public ConnectHandler(String command) {
        super(command);
    }

    @Override
    public boolean hasArgument(CommandContext ctx, int index) {
        return index <= 1;
    }

    @Override
    protected void recognizeArguments(CommandContext ctx) throws CommandFormatException {
        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        if(parsedCmd.getOtherProperties().size() > 1) {
            throw new CommandFormatException("The command accepts only one argument but received: " + parsedCmd.getOtherProperties());
        }
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        final String controller = arg.getValue(parsedCmd);
        final List<String> args = parsedCmd.getOtherProperties();

        if (!args.isEmpty()) {
            if (args.size() != 1) {
                throw new CommandFormatException("The command expects only one argument but got " + args);
            }
        }

        ctx.connectController(controller);
    }
}
