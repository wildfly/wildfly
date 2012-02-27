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
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 * Connect handler.
 *
 * @author Alexey Loubyansky
 */
public class ConnectHandler extends CommandHandlerWithHelp {

    public ConnectHandler() {
        this("connect");
    }

    public ConnectHandler(String command) {
        super(command);
    }

    @Override
    public boolean hasArgument(int index) {
        return index <= 1;
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        int port = -1;
        String host = null;
        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        final List<String> args = parsedCmd.getOtherProperties();

        if(!args.isEmpty()) {
            if(args.size() != 1) {
                throw new CommandFormatException("The command expects only one argument but got " + args);
            }
            final String arg = args.get(0);
            String portStr = null;
            int colonIndex = arg.lastIndexOf(':');
            if(colonIndex < 0) {
                // default port
                host = arg;
            } else if(colonIndex == 0) {
                // default host
                portStr = arg.substring(1).trim();
            } else {
                final boolean hasPort;
                int closeBracket = arg.lastIndexOf(']');
                if (closeBracket != -1) {
                    //possible ip v6
                    if (closeBracket > colonIndex) {
                        hasPort = false;
                    } else {
                        hasPort = true;
                    }
                } else {
                    //probably ip v4
                    hasPort = true;
                }
                if (hasPort) {
                    host = arg.substring(0, colonIndex).trim();
                    portStr = arg.substring(colonIndex + 1).trim();
                } else {
                    host = arg;
                }
            }

            if(portStr != null) {
                try {
                    port = Integer.parseInt(portStr);
                } catch(NumberFormatException e) {
                    throw new CommandFormatException("The port must be a valid non-negative integer: '" + args + "'");
                }
                if(port < 0) {
                    throw new CommandFormatException("The port must be a valid non-negative integer: '" + args + "'");
                }
            }
        }

        ctx.connectController(host, port);
    }
}
