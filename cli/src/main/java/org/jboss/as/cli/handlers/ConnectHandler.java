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


import org.jboss.as.cli.CommandContext;

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
    protected void doHandle(CommandContext ctx) {

        String args = ctx.getArgumentsString();
        int port = -1;
        String host = null;
        if(args != null) {
            String portStr = null;
            int colonIndex = args.indexOf(':');
            if(colonIndex < 0) {
                // default port
                host = args;
            } else if(colonIndex == 0) {
                // default host
                portStr = args.substring(1).trim();
            } else {
                host = args.substring(0, colonIndex).trim();
                portStr = args.substring(colonIndex + 1).trim();
            }

            if(portStr != null) {
                try {
                    port = Integer.parseInt(portStr);
                } catch(NumberFormatException e) {
                    ctx.printLine("The port must be a valid non-negative integer: '" + args + "'");
                    return;
                }

                if(port < 0) {
                    ctx.printLine("The port must be a valid non-negative integer: '" + args + "'");
                    return;
                }
            }
        }

        ctx.connectController(host, port);
    }
}
