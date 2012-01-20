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
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public class PrefixHandler extends CommandHandlerWithHelp {

    private final ArgumentWithValue nodePath;

    public PrefixHandler() {
        this("cn");
    }

    public PrefixHandler(String command) {
        super(command, true);
        nodePath = new ArgumentWithValue(this, OperationRequestCompleter.ARG_VALUE_COMPLETER, 0, "--node-path");
    }

    @Override
    protected void doHandle(CommandContext ctx) {

        final String nodePath = this.nodePath.getValue(ctx.getParsedCommandLine());

        OperationRequestAddress prefix = ctx.getPrefix();

        if(nodePath == null) {
            ctx.printLine(ctx.getPrefixFormatter().format(prefix));
            return;
        }

        CommandLineParser.CallbackHandler handler = new DefaultCallbackHandler(prefix);
        try {
            ctx.getCommandLineParser().parse(ctx.getArgumentsString(), handler);
        } catch (CommandFormatException e) {
            ctx.error(e.getLocalizedMessage());
        }
    }
}
