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
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationCallbackHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public class PrefixHandler extends CommandHandlerWithHelp {

    public PrefixHandler() {
        this("cn");
    }

    public PrefixHandler(String command) {
        super(command, true,
                new SimpleTabCompleterWithDelegate(new String[]{"--help"},
                        OperationRequestCompleter.INSTANCE));
    }

    @Override
    protected void doHandle(CommandContext ctx) {

        OperationRequestAddress prefix = ctx.getPrefix();

        if(!ctx.hasArguments()) {
            ctx.printLine(ctx.getPrefixFormatter().format(prefix));
            return;
        }

        OperationRequestParser.CallbackHandler handler = new DefaultOperationCallbackHandler(ctx.getPrefix());
        try {
            ctx.getOperationRequestParser().parse(ctx.getCommandArguments(), handler);
        } catch (CommandFormatException e) {
            ctx.printLine(e.getLocalizedMessage());
        }
    }
}
