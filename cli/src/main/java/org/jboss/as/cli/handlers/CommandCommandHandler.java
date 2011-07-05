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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.impl.DefaultOperationCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandCommandHandler extends CommandHandlerWithHelp {

    private final ArgumentWithValue profile;
    private final ArgumentWithValue action = new ArgumentWithValue(this, new SimpleTabCompleter(new String[]{"add", "list", "remove"}), 0, "--action");
    private final ArgumentWithValue nodePath;
    private final ArgumentWithValue idProperty;
    private final ArgumentWithValue commandName;

    private final CommandRegistry cmdRegistry;

    public CommandCommandHandler(CommandRegistry cmdRegistry) {
        super("command", true);
        this.cmdRegistry = cmdRegistry;

        action.addCantAppearAfter(helpArg);

        profile = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                return Util.getNodeNames(ctx.getModelControllerClient(), null, "profile");
            }}), "--profile") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                final String actionName = action.getValue(ctx.getParsedArguments());
                if(actionName == null || !"add".equals(actionName)) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
        profile.addCantAppearAfter(helpArg);

        nodePath = new ArgumentWithValue(this, new CommandLineCompleter(){
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                int offset = 0;
                if(ctx.isDomainMode()) {
                    final String profileName = profile.getValue(ctx.getParsedArguments());
                    if(profileName == null) {
                        return -1;
                    }
                    StringBuilder buf = new StringBuilder();
                    buf.append("profile=").append(profileName).append('/');
                    offset = buf.length();
                    buf.append(buffer);
                    buffer = buf.toString();
                }
                return OperationRequestCompleter.INSTANCE.complete(ctx, buffer, cursor + offset, candidates) - offset;
            }}, "--node-type") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return "add".equals(action.getValue(ctx.getParsedArguments())) && super.canAppearNext(ctx);
            }
        };

        idProperty = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                List<Property> props = getNodeProperties(ctx);
                if(props.isEmpty()) {
                    return Collections.emptyList();
                }

                final List<String> candidates = new ArrayList<String>();
                for(Property prop : props) {
                    final ModelNode value = prop.getValue();
                    if(value.has("access-type") && "read-only".equals(value.get("access-type").asString())) {
                        candidates.add(prop.getName());
                    }
                }
                return candidates;
            }}), "--property-id");
        idProperty.addRequiredPreceding(nodePath);

        commandName = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {

                final String actionName = action.getValue(ctx.getParsedArguments());
                if(actionName == null) {
                    return Collections.emptyList();
                }

                if (actionName.equals("add")) {
                   final String thePath = nodePath.getValue(ctx.getParsedArguments());
                   if (thePath == null) {
                      return Collections.emptyList();
                   }

                   DefaultOperationCallbackHandler handler = new DefaultOperationCallbackHandler();
                   try {
                       DefaultOperationRequestParser.INSTANCE.parse(thePath, handler);
                   } catch (CommandFormatException e) {
                       return Collections.emptyList();
                   }

                   OperationRequestAddress typeAddress = handler.getAddress();
                   if (!typeAddress.endsOnType()) {
                       return Collections.emptyList();
                   }
                   return Collections.singletonList(typeAddress.getNodeType());
               }

                if (actionName.equals("remove")) {
                    return getExistingCommands();
                }
                return Collections.emptyList();
            }}), "--command-name") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                ParsedArguments args = ctx.getParsedArguments();
                if(isPresent(args)) {
                    return false;
                }
                final String actionStr = action.getValue(args);
                if(actionStr == null) {
                    return false;
                }
                if("add".equals(actionStr)) {
                    return idProperty.isPresent(args);
                }
                if("remove".equals(actionStr)) {
                    return true;
                }
                return false;
            }
        };
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        final ParsedArguments args = ctx.getParsedArguments();
        final String action = this.action.getValue(args);
        if(action == null) {
            ctx.printLine("Command is missing.");
            return;
        }

        if(action.equals("list")) {
            ctx.printColumns(getExistingCommands());
            return;
        }

        if(action.equals("add")) {
            final String nodePath = this.nodePath.getValue(args, true);
            final String propName = this.idProperty.getValue(args, true);
            final String cmdName = this.commandName.getValue(args, true);

            if(!validateInput(ctx, profile.getValue(args), nodePath, propName)) {
                return;
            }

            if(cmdRegistry.getCommandHandler(cmdName) != null) {
                ctx.printLine("Command '" + cmdName + "' already registered.");
                return;
            }
            cmdRegistry.registerHandler(new GenericTypeOperationHandler(nodePath, idProperty.getValue(args)), cmdName);
            return;
        }

        if(action.equals("remove")) {
            final String cmdName = this.commandName.getValue(args, true);
            CommandHandler handler = cmdRegistry.getCommandHandler(cmdName);
            if(!(handler instanceof GenericTypeOperationHandler)) {
                ctx.printLine("Command '" + cmdName + "' is not a generic type command.");
                return;
            }
            cmdRegistry.remove(cmdName);
            return;
        }

        ctx.printLine("Unexpected action: " + action);
    }

    protected List<String> getExistingCommands() {
        final List<String> commands = new ArrayList<String>();
        for(String cmd : cmdRegistry.getTabCompletionCommands()) {
            if(cmdRegistry.getCommandHandler(cmd) instanceof GenericTypeOperationHandler) {
                commands.add(cmd);
            }
        }
        return commands;
    }

    protected List<Property> getNodeProperties(CommandContext ctx) {
        ModelNode request = initRequest(ctx);
        if(request == null) {
            return Collections.emptyList();
        }
        request.get("operation").set("read-resource-description");

        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        if(!result.hasDefined("result")) {
            return Collections.emptyList();
        }
        result = result.get("result");
        if(!result.hasDefined("attributes")) {
            return Collections.emptyList();
        }

        return result.get("attributes").asPropertyList();
    }

    protected ModelNode initRequest(CommandContext ctx) {
        ModelNode request = new ModelNode();
        ModelNode address = request.get("address");

        if(ctx.isDomainMode()) {
            final String profileName = profile.getValue(ctx.getParsedArguments());
            if(profile == null) {
                ctx.printLine("--profile argument is required to get the node description.");
                return null;
            }
            address.add("profile", profileName);
        }

        final String type = nodePath.getValue(ctx.getParsedArguments());
        DefaultOperationCallbackHandler handler = new DefaultOperationCallbackHandler();
        try {
            DefaultOperationRequestParser.INSTANCE.parse(type, handler);
        } catch (CommandFormatException e) {
            throw new IllegalArgumentException("Failed to parse nodeType: " + e.getMessage());
        }

        OperationRequestAddress typeAddress = handler.getAddress();
        if(!typeAddress.endsOnType()) {
            return null;
        }

        final String typeName = typeAddress.toParentNode().getType();
        for(OperationRequestAddress.Node node : typeAddress) {
            address.add(node.getType(), node.getName());
        }
        address.add(typeName, "?");
        return request;
    }

    protected boolean validateInput(CommandContext ctx, String profileName, String typePath, String propertyName) {

        ModelNode request = new ModelNode();
        ModelNode address = request.get("address");

        if(profileName != null) {
            address.add("profile", profileName);
        }

        DefaultOperationCallbackHandler handler = new DefaultOperationCallbackHandler();
        try {
            DefaultOperationRequestParser.INSTANCE.parse(typePath, handler);
        } catch (CommandFormatException e) {
            ctx.printLine("Failed to validate input: " + e.getLocalizedMessage());
            return false;
        }

        OperationRequestAddress typeAddress = handler.getAddress();
        if(!typeAddress.endsOnType()) {
            ctx.printLine("Node path '" + typePath + "' doesn't appear to end on a type.");
            return false;
        }

        final String typeName = typeAddress.toParentNode().getType();
        for(OperationRequestAddress.Node node : typeAddress) {
            address.add(node.getType(), node.getName());
        }

        request.get("operation").set("read-children-types");

        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            ctx.printLine("Failed to validate input: " + e.getLocalizedMessage());
            return false;
        }
        if(!result.hasDefined("result")) {
            ctx.printLine("Failed to validate input: operation response doesn't contain result info.");
            return false;
        }

        boolean pathValid = false;
        for(ModelNode typeNode : result.get("result").asList()) {
            if(typeNode.asString().equals(typeName)) {
                pathValid = true;
                break;
            }
        }
        if(!pathValid) {
            ctx.printLine("Type '" + typeName + "' not found amoung child types of '" + ctx.getPrefixFormatter().format(typeAddress) + "'");
            return false;
        }

        address.add(typeName, "?");
        request.get("operation").set("read-resource-description");

        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            ctx.printLine(e.getLocalizedMessage());
            return false;
        }
        if(!result.hasDefined("result")) {
            ctx.printLine("Failed to validate input: operation response doesn't contain result info.");
            return false;
        }
        result = result.get("result");
        if(!result.hasDefined("attributes")) {
            ctx.printLine("Failed to validate input: description of attributes is missing for " + typePath);
            return false;
        }

        for(Property prop : result.get("attributes").asPropertyList()) {
            if(prop.getName().equals(propertyName)) {
                ModelNode value = prop.getValue();
                if(value.has("access-type") && "read-only".equals(value.get("access-type").asString())) {
                    return true;
                }
                ctx.printLine("Property " + propertyName + " is not read-only.");
                return false;
            }
        }
        ctx.printLine("Property '" + propertyName + "' wasn't found among the properties of " + typePath);
        return false;
    }
}
