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
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultOperationCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;

/**
 *
 * @author Alexey Loubyansky
 */
public class LsHandler implements CommandHandler {

    @Override
    public void handle(CommandContext ctx) {

        final OperationRequestAddress address;

        String args = ctx.getCommandArguments();
        if(args != null) {
            args = args.trim();
            if (!args.isEmpty()) {
                address = new DefaultOperationRequestAddress(ctx.getPrefix());
                OperationRequestParser.CallbackHandler handler = new DefaultOperationCallbackHandler(address);
                try {
                    ctx.getOperationRequestParser().parse(args, handler);
                } catch (CommandFormatException e) {
                    ctx.printLine(e.getLocalizedMessage());
                }
            } else {
                address = ctx.getPrefix();
            }
        } else {
            address = ctx.getPrefix();
        }

        final List<String> names;
        if(address.endsOnType()) {
            names = ctx.getOperationCandidatesProvider().getNodeNames(address);
        } else {
            names = ctx.getOperationCandidatesProvider().getNodeTypes(address);
        }

        ctx.printColumns(names);
    }
}
