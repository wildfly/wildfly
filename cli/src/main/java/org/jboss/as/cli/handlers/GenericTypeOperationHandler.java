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


import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
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

/**
 *
 * @author Alexey Loubyansky
 */
public class GenericTypeOperationHandler extends BatchModeCommandHandler {

    protected final String type;
    protected final OperationRequestAddress nodePath;
    protected final ArgumentWithValue profile;
    protected final ArgumentWithValue name;
    protected final ArgumentWithValue operation;
    protected final ArgumentWithValue props;

    protected final List<String> typeOps;
    protected final List<String> excludeOps;

    public GenericTypeOperationHandler(String nodeType, String idName, List<String> typeOperations, List<String> excludeOperations) {

        super("generic-type-operation", true);

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

        this.typeOps = typeOperations;
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
            }), idName) {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };

        operation = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
                @Override
                public List<String> getAllCandidates(CommandContext ctx) {
                    final String theName = name.getValue(ctx.getParsedArguments());
                    if(theName == null) {
                        return typeOps;
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
                    address.toNode(type, theName);
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
        //operation.addRequiredPreceding(name);

        props = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {

                ParsedArguments args = ctx.getParsedArguments();

                final String theName = name.getValue(args);
                if(theName == null) {
                    return Collections.emptyList();
                }

                final String op = operation.getValue(args);
                if(op == null) {
                    return Collections.emptyList();
                }

                final List<String> allProps;
                if(typeOps.contains(op)) {
                    ModelNode request = new ModelNode();
                    ModelNode address = request.get("address");
                    if(ctx.isDomainMode()) {
                        final String profileName = profile.getValue(ctx.getParsedArguments());
                        if(profile == null) {
                            return Collections.emptyList();
                        }
                        address.add("profile", profileName);
                    }
                    for(OperationRequestAddress.Node node : nodePath) {
                        address.add(node.getType(), node.getName());
                    }
                    address.add(type, "*");
                    request.get("operation").set("read-operation-description");
                    request.get("name").set(op);

                    try {
                        ModelNode result = ctx.getModelControllerClient().execute(request);
                        if(!result.hasDefined("result")) {
                            return Collections.emptyList();
                        }
                        result = result.get("result");
                        if(!result.hasDefined("step-1")) {
                            return Collections.emptyList();
                        }
                        result = result.get("step-1");
                        if(!result.hasDefined("result")) {
                            return Collections.emptyList();
                        }
                        allProps = Util.getRequestPropertyNames(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Collections.emptyList();
                    }

                } else {
                    DefaultOperationRequestAddress address = new DefaultOperationRequestAddress();
                    if(ctx.isDomainMode()) {
                        final String profileName = profile.getValue(args);
                        if(profile == null) {
                            return Collections.emptyList();
                        }
                        address.toNode("profile=", profileName);
                    }
                    for(OperationRequestAddress.Node node : nodePath) {
                        address.toNode(node.getType(), node.getName());
                    }
                    address.toNode(type, theName);
                    allProps = ctx.getOperationCandidatesProvider().getPropertyNames(op, address);
                }


                if(allProps.size() > 0) {
                try {
                    Set<String> specified = args.getArgumentNames();
                    int i = 0;
                    while(i < allProps.size()) {
                        final String propName = "--" + allProps.get(i);
                        if(specified.contains(propName)) {
                            allProps.remove(i);
                        } else {
                            allProps.set(i, propName + "=");
                            ++i;
                        }
                    }
                } catch (CommandFormatException e) {
                    return Collections.emptyList();
                }
                }
                return allProps;
            }}), 2, "--props") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
            @Override
            public boolean isPresent(ParsedArguments args) throws CommandFormatException {
                return false;
            }
            };
            props.addRequiredPreceding(operation);
    }

    @Override
    public boolean hasArgument(String name) {
        return true;
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {

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

        final String operation = this.operation.getValue(ctx.getParsedArguments(), true);

        for(OperationRequestAddress.Node node : nodePath) {
            builder.addNode(node.getType(), node.getName());
        }
        builder.addNode(type, name);

        builder.setOperationName(operation);

        for(String argName : args.getArgumentNames()) {
            if(argName.equals("--profile")) {
                continue;
            }
            builder.addProperty(argName.substring(2), args.getArgument(argName));
        }

        return builder.buildRequest();
    }
}
