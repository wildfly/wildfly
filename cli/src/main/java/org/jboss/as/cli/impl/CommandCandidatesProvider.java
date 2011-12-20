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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestHeader;


/**
 *
 * @author Alexey Loubyansky
 */
public class CommandCandidatesProvider implements OperationCandidatesProvider {

    private final CommandRegistry registry;

    public CommandCandidatesProvider(CommandRegistry registry) {
        if(registry == null) {
            throw new IllegalArgumentException("Command registry can't be null.");
        }
        this.registry = registry;
    }

    @Override
    public List<String> getNodeNames(CommandContext ctx, OperationRequestAddress prefix) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getNodeTypes(CommandContext ctx, OperationRequestAddress prefix) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getOperationNames(CommandContext ctx, OperationRequestAddress prefix) {
        final List<String> commands = new ArrayList<String>();
        for(String command : registry.getTabCompletionCommands()) {
            CommandHandler handler = registry.getCommandHandler(command);
            if(handler.isAvailable(ctx)) {
                commands.add(command);
            }
        }
        return commands;
    }

    @Override
    public Collection<CommandArgument> getProperties(CommandContext ctx, String operationName, OperationRequestAddress address) {
        CommandHandler handler = registry.getCommandHandler(operationName);
        if(handler == null) {
            return Collections.emptyList();
        }
        return handler.getArguments(ctx);
    }

    @Override
    public Map<String, OperationRequestHeader> getHeaders(CommandContext ctx) {
        return Collections.emptyMap(); // TODO need to implement this for commands
    }
}
