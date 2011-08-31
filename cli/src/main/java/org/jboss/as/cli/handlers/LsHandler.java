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
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;

/**
 *
 * @author Alexey Loubyansky
 */
public class LsHandler extends CommandHandlerWithHelp {

    private final ArgumentWithValue nodePath;
    private final ArgumentWithoutValue l;

    public LsHandler() {
        this("ls");
    }

    public LsHandler(String command) {
        super(command, true);
        l = new ArgumentWithoutValue(this, "-l");
        nodePath = new ArgumentWithValue(this, OperationRequestCompleter.ARG_VALUE_COMPLETER, 0, "--node-path");
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        String nodePath = this.nodePath.getValue(parsedCmd);

        final OperationRequestAddress address;
        if (nodePath != null) {
            address = new DefaultOperationRequestAddress(ctx.getPrefix());
            CommandLineParser.CallbackHandler handler = new DefaultCallbackHandler(address);
            nodePath = ctx.getArgumentsString();
            if(l.isPresent(parsedCmd)) {
                nodePath = nodePath.trim();
                if(nodePath.startsWith("-l ")) {
                    nodePath = nodePath.substring(3);
                } else {
                    nodePath = nodePath.substring(0, nodePath.length() - 3);
                }
            }

            try {
                ctx.getCommandLineParser().parse(nodePath, handler);
            } catch (CommandFormatException e) {
                ctx.printLine(e.getLocalizedMessage());
            }
        } else {
            address = new DefaultOperationRequestAddress(ctx.getPrefix());
        }

        final List<String> names;
        if(address.endsOnType()) {
            final String type = address.getNodeType();
            address.toParentNode();
            names = Util.getNodeNames(ctx.getModelControllerClient(), address, type);
        } else {
            names = Util.getNodeTypes(ctx.getModelControllerClient(), address);
        }

        printList(ctx, names, l.isPresent(parsedCmd));
    }
}
