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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultOperationCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author Alexey Loubyansky
 */
public class GenericTypeOperationHandler extends BatchModeCommandHandler {

    protected final String commandName;
    protected final String type;
    protected final String idProperty;
    protected final OperationRequestAddress nodePath;
    protected final ArgumentWithValue profile;
    protected final ArgumentWithValue name;
    protected final ArgumentWithValue operation;

    protected final List<String> excludeOps;

    // help arguments
    protected final ArgumentWithoutValue helpProperties;
    protected final ArgumentWithoutValue helpCommands;

    protected final CommandLineCompleter genericCompleter;

    public GenericTypeOperationHandler(String nodeType, String idProperty) {
        this(nodeType, idProperty, Arrays.asList("read-attribute", "read-children-names", "read-children-resources",
                "read-children-types", "read-operation-description", "read-operation-names", "read-resource",
                "read-resource-description", "validate-address", "write-attribute"));
    }

    public GenericTypeOperationHandler(String nodeType, String idProperty, List<String> excludeOperations) {

        super("generic-type-operation", true);

        helpArg.setExclusive(false);
        nodePath = new DefaultOperationRequestAddress();
        OperationRequestParser.CallbackHandler handler = new DefaultOperationCallbackHandler(nodePath);
        try {
            DefaultOperationRequestParser.INSTANCE.parse(nodeType, handler);
        } catch (CommandFormatException e) {
            throw new IllegalArgumentException("Failed to parse nodeType: " + e.getMessage());
        }

        if(!nodePath.endsOnType()) {
            throw new IllegalArgumentException("The node path doesn't end on a type: '" + nodeType + "'");
        }
        this.type = nodePath.getNodeType();
        nodePath.toParentNode();
        this.commandName = type;
        this.idProperty = idProperty;

        this.excludeOps = excludeOperations;

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
                return super.canAppearNext(ctx);
            }
        };
        profile.addCantAppearAfter(helpArg);

        operation = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
                @Override
                public List<String> getAllCandidates(CommandContext ctx) {

                    final boolean writeAttribute;
                    try {
                        writeAttribute = name.isPresent(ctx.getParsedArguments());
                    } catch (CommandFormatException e) {
                        return Collections.emptyList();
                    }

                    if(writeAttribute) {
                        Set<String> specified;
                        try {
                            specified = ctx.getParsedArguments().getArgumentNames();
                        } catch (CommandFormatException e) {
                            return Collections.emptyList();
                        }
                        final List<String> theProps = new ArrayList<String>();
                        for(Property prop : getNodeProperties(ctx)) {
                            final String propName = "--" + prop.getName();
                            if(!specified.contains(propName) && prop.getValue().has("access-type") &&
                                    prop.getValue().get("access-type").asString().contains("write")) {
                                theProps.add(propName + "=");
                            }
                        }
                        return theProps;
                    }

                    DefaultOperationRequestAddress address = new DefaultOperationRequestAddress();
                    if(ctx.isDomainMode()) {
                        final String profileName = profile.getValue(ctx.getParsedArguments());
                        if(profile == null) {
                            return Collections.emptyList();
                        }
                        address.toNode("profile", profileName);
                    }

                    for(OperationRequestAddress.Node node : nodePath) {
                        address.toNode(node.getType(), node.getName());
                    }
                    address.toNode(type, "?");
                    List<String> ops = ctx.getOperationCandidatesProvider().getOperationNames(address);
                    ops.removeAll(excludeOps);
                    return ops;
                }}), 0, "--operation") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
        operation.addCantAppearAfter(helpArg);

        name = new ArgumentWithValue(this, new DefaultCompleter(new DefaultCompleter.CandidatesProvider() {
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                ModelControllerClient client = ctx.getModelControllerClient();
                if (client == null) {
                    return Collections.emptyList();
                    }

                DefaultOperationRequestAddress address = new DefaultOperationRequestAddress();
                if(ctx.isDomainMode()) {
                    final String profileName = profile.getValue(ctx.getParsedArguments());
                    if(profile == null) {
                        return Collections.emptyList();
                    }
                    address.toNode("profile", profileName);
                }

                for(OperationRequestAddress.Node node : nodePath) {
                    address.toNode(node.getType(), node.getName());
                }

                return Util.getNodeNames(ctx.getModelControllerClient(), address, type);
                }
            }), "--" + idProperty) {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
        name.addCantAppearAfter(helpArg);

        helpArg.addCantAppearAfter(name);

        helpProperties = new ArgumentWithoutValue(this, "--properties");
        helpProperties.addRequiredPreceding(helpArg);
        helpProperties.addCantAppearAfter(operation);

        helpCommands = new ArgumentWithoutValue(this, "--commands");
        helpCommands.addRequiredPreceding(helpArg);
        helpCommands.addCantAppearAfter(operation);
        helpCommands.addCantAppearAfter(helpProperties);
        helpProperties.addCantAppearAfter(helpCommands);


        ///

        genericCompleter = new BaseArgumentTabCompleter(){
            private final List<CommandArgument> staticArgs = new ArrayList<CommandArgument>();
            {
                staticArgs.add(helpArg);
                staticArgs.add(helpCommands);
                staticArgs.add(helpProperties);
                staticArgs.add(profile);
                staticArgs.add(name);
                staticArgs.add(operation);
            }

            private List<CommandArgument> nodeProps;
            private Map<String, List<CommandArgument>> propsByOp;

            @Override
            protected Iterable<CommandArgument> getAllArguments(CommandContext ctx) {

                ParsedArguments args = ctx.getParsedArguments();

                final String theName = name.getValue(args);
                if(theName == null) {
                    return staticArgs;
                }

                final String op = operation.getValue(args);
                if(op == null) {
                    // list node properties
                    if(nodeProps == null) {
                        nodeProps = new ArrayList<CommandArgument>();
                        for(Property prop : getNodeProperties(ctx)) {
                            final ModelNode propDescr = prop.getValue();
                            if(propDescr.has("access-type") && "read-write".equals(propDescr.get("access-type").asString())) {
                                nodeProps.add(new ArgumentWithValue(GenericTypeOperationHandler.this, "--" + prop.getName()));
                            }
                        }
                    }
                    return nodeProps;
                } else {
                    // list operation properties
                    if(propsByOp == null) {
                        propsByOp = new HashMap<String, List<CommandArgument>>();
                    }
                    List<CommandArgument> opProps = propsByOp.get(op);
                    if(opProps == null) {
                        final ModelNode descr;
                        try {
                            descr = getOperationDescription(ctx, op);
                        } catch (IOException e1) {
                            return Collections.emptyList();
                        }

                        if(descr == null || !descr.has("request-properties")) {
                            opProps = Collections.emptyList();
                        } else {
                            opProps = new ArrayList<CommandArgument>();
                            for (Property prop : descr.get("request-properties").asPropertyList()) {
                                opProps.add(new ArgumentWithValue(GenericTypeOperationHandler.this, "--" + prop.getName()));
                            }
                        }
                        propsByOp.put(op, opProps);
                    }
                    return opProps;
                }
            }};
    }

    @Override
    public boolean hasArgument(String name) {
        return true;
    }

    @Override
    public boolean hasArgument(int index) {
        return true;
    }

    @Override
    public CommandLineCompleter getArgumentCompleter() {
        return genericCompleter;
    }

    protected BaseArgumentTabCompleter initArgumentCompleter() {
        return null;
    }

    public void addArgument(CommandArgument arg) {
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {
        final String operation = this.operation.getValue(ctx.getParsedArguments());
        if(operation == null) {
            return buildWritePropertyRequest(ctx);
        }
        return buildOperationRequest(ctx, operation);
    }

    protected ModelNode buildWritePropertyRequest(CommandContext ctx) throws CommandFormatException {

        final String name = this.name.getValue(ctx.getParsedArguments(), true);

        ModelNode composite = new ModelNode();
        composite.get("operation").set("composite");
        composite.get("address").setEmptyList();
        ModelNode steps = composite.get("steps");

        ParsedArguments args = ctx.getParsedArguments();

        final String profile;
        if(ctx.isDomainMode()) {
            profile = this.profile.getValue(args);
            if(profile == null) {
                throw new OperationFormatException("--profile argument value is missing.");
            }
        } else {
            profile = null;
        }

        for(String argName : args.getArgumentNames()) {
            if(argName.equals("--profile") || this.name.getFullName().equals(argName)) {
                continue;
            }

            final String valueString = args.getArgument(argName);
            if(valueString == null) {
                continue;
            }

            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            if (profile != null) {
                builder.addNode("profile", profile);
            }

            for(OperationRequestAddress.Node node : nodePath) {
                builder.addNode(node.getType(), node.getName());
            }
            builder.addNode(type, name);
            builder.setOperationName("write-attribute");
            if(argName.charAt(1) == '-') {
                argName = argName.substring(2);
            } else {
                argName = argName.substring(1);
            }
            builder.addProperty("name", argName);

            // TODO this is just a guess and might not always work
            if(argName.endsWith("properties")) {

                ModelNode nodeValue = new ModelNode();
                String[] props = valueString.split(",");
                for(String prop : props) {
                    int equals = prop.indexOf('=');
                    if(equals == -1) {
                        throw new CommandFormatException("Property '" + prop + "' in '" + valueString + "' is missing the equals sign.");
                    }
                    String propName = prop.substring(0, equals);
                    if(propName.isEmpty()) {
                        throw new CommandFormatException("Property name is missing for '" + prop + "' in '" + valueString + "'");
                    }
                    nodeValue.add(propName, prop.substring(equals + 1));
                }
                builder.getModelNode().get("value").set(nodeValue);
            } else {
                builder.addProperty("value", valueString);
            }

            steps.add(builder.buildRequest());
        }

        return composite;
    }

    protected ModelNode buildOperationRequest(CommandContext ctx, final String operation) throws CommandFormatException {

        ParsedArguments args = ctx.getParsedArguments();

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        if(ctx.isDomainMode()) {
            final String profile = this.profile.getValue(args);
            if(profile == null) {
                throw new OperationFormatException("Required argument --profile is missing.");
            }
            builder.addNode("profile", profile);
        }

        final String name = this.name.getValue(ctx.getParsedArguments(), true);

        for(OperationRequestAddress.Node node : nodePath) {
            builder.addNode(node.getType(), node.getName());
        }
        builder.addNode(type, name);

        builder.setOperationName(operation);

        for(String argName : args.getArgumentNames()) {
            if(argName.equals("--profile")) {
                continue;
            }

            final String valueString = args.getArgument(argName);
            if(valueString == null) {
                continue;
            }

            if(argName.charAt(1) == '-') {
                argName = argName.substring(2);
            } else {
                argName = argName.substring(1);
            }

            // TODO this is just a guess and might not always work
            if(argName.endsWith("properties")) {

                ModelNode nodeValue = new ModelNode();
                String[] props = valueString.split(",");
                for(String prop : props) {
                    int equals = prop.indexOf('=');
                    if(equals == -1) {
                        throw new CommandFormatException("Property '" + prop + "' in '" + valueString + "' is missing the equals sign.");
                    }
                    String propName = prop.substring(0, equals);
                    if(propName.isEmpty()) {
                        throw new CommandFormatException("Property name is missing for '" + prop + "' in '" + valueString + "'");
                    }
                    nodeValue.add(propName, prop.substring(equals + 1));
                }
                builder.getModelNode().get(argName).set(nodeValue);
            } else {
                builder.addProperty(argName, valueString);
            }
        }

        return builder.buildRequest();
    }

    protected void printHelp(CommandContext ctx) {

        ParsedArguments args = ctx.getParsedArguments();
        try {
            if(helpProperties.isPresent(args)) {
                printAttributes(ctx);
                return;
            }
        } catch (CommandFormatException e) {
            ctx.printLine(e.getLocalizedMessage());
            return;
        }

        try {
            if(helpCommands.isPresent(args)) {
                printCommands(ctx);
                return;
            }
        } catch (CommandFormatException e) {
            ctx.printLine(e.getLocalizedMessage());
            return;
        }

        final String operationName = operation.getValue(args);
        if(operationName == null) {
            printNodeDescription(ctx);
            return;
        }

        try {
            ModelNode result = getOperationDescription(ctx, operationName);
            if(!result.hasDefined("description")) {
                ctx.printLine("Operation description is not available.");
                return;
            }

            final StringBuilder buf = new StringBuilder();
            buf.append("Operation description:\n\n\t");
            buf.append(result.get("description").asString());
            buf.append("\n\nProperties:");
            ctx.printLine(buf.toString());

            boolean idPropListed = false;
            if(result.has("request-properties")) {
                result = result.get("request-properties");
                for (Property attr : result.asPropertyList()) {
                    if(!idPropListed) {
                        idPropListed = idProperty.equals(attr.getName());
                    }
                    final ModelNode value = attr.getValue();

                    final boolean required = value.has("required") ? value.get("required").asBoolean() : false;

                    final String type = value.has("type") ? value.get("type").asString() : "no type info";

                    final StringBuilder descr = new StringBuilder();
                    descr.append("\n --");
                    descr.append(attr.getName());

                    final int length = descr.length();
                    int newLength = Math.max(24, ((length + 4) / 4) * 4);
                    descr.setLength(newLength);
                    for (int i = length; i < newLength; ++i) {
                        descr.setCharAt(i, ' ');
                    }

                    descr.append("- ");

                    if (value.has("description")) {
                        descr.append('(');
                        descr.append(type).append(',');
                        if (required) {
                            descr.append("required");
                        } else {
                            descr.append("optional");
                        }
                        descr.append(") ");
                        descr.append(value.get("description").asString());
                    } else {
                        descr.append("no description");
                    }
                    ctx.printLine(descr.toString());
                }
            }

            if(!idPropListed) {
                final StringBuilder descr = new StringBuilder();
                descr.append("\n --");
                descr.append(idProperty);

                final int length = descr.length();
                int newLength = Math.max(24, ((length + 4) / 4) * 4);
                descr.setLength(newLength);
                for (int i = length; i < newLength; ++i) {
                    descr.setCharAt(i, ' ');
                }

                descr.append("- identifies the instance to perform the operation on.");
                ctx.printLine(descr.toString());
            }
        } catch (Exception e) {
        }
    }

    protected void printNodeDescription(CommandContext ctx) {
        ModelNode request = initRequest(ctx);
        if(request == null) {
            return;
        }
        request.get("operation").set("read-resource-description");

        try {
            ModelNode result = ctx.getModelControllerClient().execute(request);
            if(!result.hasDefined("result")) {
                ctx.printLine("Node description is not available.");
                return;
            }
            result = result.get("result");
            if(!result.hasDefined("description")) {
                ctx.printLine("Node description is not available.");
                return;
            }
            ctx.printLine(result.get("description").asString());
        } catch (Exception e) {
        }
    }

    protected void printAttributes(CommandContext ctx) {

        final List<Property> props;
        try {
            props = getNodeProperties(ctx);
        } catch(Exception e) {
            ctx.printLine("Failed to obtain the list or properties: " + e.getLocalizedMessage());
            return;
        }

        for (Property attr : props) {
            final ModelNode value = attr.getValue();
            // filter metrics
            if (value.has("access-type") && "metric".equals(value.get("access-type").asString())) {
                continue;
            }

            final boolean required = value.has("required") ? value.get("required").asBoolean() : false;

            final StringBuilder descr = new StringBuilder();
            descr.append("\n ");
            descr.append(attr.getName());

            final int length = descr.length();
            int newLength = Math.max(24, ((length + 4) / 4) * 4);
            descr.setLength(newLength);
            for (int i = length; i < newLength; ++i) {
                descr.setCharAt(i, ' ');
            }

            descr.append("- ");

            if (value.has("description")) {
                descr.append('(');
                if (required) {
                    descr.append("required");
                } else {
                    descr.append("optional");
                }
                descr.append(") ");
                descr.append(value.get("description").asString());
            } else {
                descr.append("no description");
            }
            ctx.printLine(descr.toString());
        }
    }

    protected void printCommands(CommandContext ctx) {
        ModelNode request = initRequest(ctx);
        if(request == null) {
            return;
        }
        request.get("operation").set("read-operation-names");

        try {
            ModelNode result = ctx.getModelControllerClient().execute(request);
            if(!result.hasDefined("result")) {
                ctx.printLine("Operation names aren't available.");
                return;
            }
            final List<String> list = Util.getList(result);
            list.removeAll(this.excludeOps);
            list.add("To read the description of a specific command execute '" + this.commandName + " command_name --help'.");
            for(String name : list) {
                ctx.printLine(name);
            }
        } catch (Exception e) {
        }
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

    protected ModelNode getOperationDescription(CommandContext ctx, String operationName) throws IOException {
        ModelNode request = initRequest(ctx);
        if(request == null) {
            return null;
        }
        request.get("operation").set("read-operation-description");
        request.get("name").set(operationName);

        ModelNode result = ctx.getModelControllerClient().execute(request);
        if (!result.hasDefined("result")) {
            return null;
        }
        return result.get("result");
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
        for(OperationRequestAddress.Node node : nodePath) {
            address.add(node.getType(), node.getName());
        }
        address.add(type, "?");
        return request;
    }
}
