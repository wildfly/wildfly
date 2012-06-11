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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * A command that it is composed of a several operations
 * performed against the same resource.
 *
 * @author Alexey Loubyansky
 */
public class ResourceCompositeOperationHandler extends BaseOperationCommand {

//    private final String commandName;
    private final String[] ops;

    private final Map<String, Map<String, ArgumentWithValue>> opArgs = new HashMap<String, Map<String, ArgumentWithValue>>();

    protected final String idProperty;

    protected final ArgumentWithValue name;
    protected final ArgumentWithValue profile;

    private Map<String, ArgumentValueConverter> propConverters;
    private Map<String, CommandLineCompleter> valueCompleters;

    private final Map<String, CommandArgument> staticArgs = new HashMap<String, CommandArgument>();

    public ResourceCompositeOperationHandler(CommandContext ctx,
            String command,
            String nodeType,
            String idProperty,
            String... operations) {
        super(ctx, command, true);

        if(command == null) {
            throw new IllegalArgumentException("Command name can't be null.");
        }
  //      this.commandName = command;

        if(operations == null || operations.length == 0) {
            throw new IllegalArgumentException("There must be at least one operation.");
        }
        ops = operations;

        this.idProperty = idProperty;

        addRequiredPath(nodeType);

        profile = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                return Util.getNodeNames(ctx.getModelControllerClient(), null, Util.PROFILE);
            }}), "--profile") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!isDependsOnProfile()) {
                    return false;
                }
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
                if(isDependsOnProfile() && ctx.isDomainMode()) {
                    final String profileName = profile.getValue(ctx.getParsedCommandLine());
                    if(profile == null) {
                        return Collections.emptyList();
                    }
                    address.toNode("profile", profileName);
                }
                for(OperationRequestAddress.Node node : getRequiredAddress()) {
                    address.toNode(node.getType(), node.getName());
                }
                return Util.getNodeNames(ctx.getModelControllerClient(), address, getRequiredType());
                }
            }), (idProperty == null ? "--name" : "--" + idProperty)) {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(isDependsOnProfile() && ctx.isDomainMode() && !profile.isValueComplete(ctx.getParsedCommandLine())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
        name.addCantAppearAfter(helpArg);
        helpArg.addCantAppearAfter(name);

        staticArgs.put(helpArg.getFullName(), helpArg);
        staticArgs.put(profile.getFullName(), profile);
        staticArgs.put(name.getFullName(), name);
    }

    public void addValueConverter(String propertyName, ArgumentValueConverter converter) {
        if(propConverters == null) {
            propConverters = new HashMap<String, ArgumentValueConverter>();
        }
        propConverters.put(propertyName, converter);
    }

    public void addValueCompleter(String propertyName, CommandLineCompleter completer) {
        if(valueCompleters == null) {
            valueCompleters = new HashMap<String, CommandLineCompleter>();
        }
        valueCompleters.put(propertyName, completer);
    }

    @Override
    protected ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {

        final ModelNode address = buildOperationAddress(ctx);

        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        final ParsedCommandLine parsedArgs = ctx.getParsedCommandLine();

        for(String opName : this.ops) {
            final ModelNode req = new ModelNode();
            req.get(Util.OPERATION).set(opName);
            req.get(Util.ADDRESS).set(address);

            Map<String, ArgumentWithValue> opArgs;
            try {
                opArgs = getOperationArguments(ctx, opName);
            } catch (CommandFormatException e) {
                throw e;
            } catch (CommandLineException e) {
                throw new CommandFormatException("Failed to read " + opName + " arguments.", e);
            }
            for(ArgumentWithValue arg : opArgs.values()) {

                final String argName = arg.getFullName();
                final String propName;
                if(argName.charAt(1) == '-') {
                    propName = argName.substring(2);
                } else {
                    propName = argName.substring(1);
                }

                final String valueString = arg.getValue(parsedArgs);
                if(valueString != null) {
                    ModelNode nodeValue = arg.getValueConverter().fromString(valueString);
                    req.get(propName).set(nodeValue);
                }
            }
            steps.add(req);
        }

        return composite;
    }

    protected ModelNode buildOperationAddress(CommandContext ctx) throws CommandFormatException {

        final String name = ResourceCompositeOperationHandler.this.name.getValue(ctx.getParsedCommandLine(), true);

        ModelNode address = new ModelNode();
        if(isDependsOnProfile() && ctx.isDomainMode()) {
            final String profile = ResourceCompositeOperationHandler.this.profile.getValue(ctx.getParsedCommandLine());
            if(profile == null) {
                throw new OperationFormatException("Required argument --profile is missing.");
            }
            address.add(Util.PROFILE, profile);
        }

        for(OperationRequestAddress.Node node : getRequiredAddress()) {
            address.add(node.getType(), node.getName());
        }
        address.add(getRequiredType(), name);
        return address;
    }

    @Override
    public CommandArgument getArgument(CommandContext ctx, String name) {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        try {
            if(!this.name.isValueComplete(args)) {
                return staticArgs.get(name);
            }
        } catch (CommandFormatException e) {
            return null;
        }
        return getAllArguments(ctx).get(name);
    }

    @Override
    public Collection<CommandArgument> getArguments(CommandContext ctx) {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        try {
            if(!this.name.isValueComplete(args)) {
                return staticArgs.values();
            }
        } catch (CommandFormatException e) {
            return null;
        }
        return getAllArguments(ctx).values();
    }

    @Override
    protected void recognizeArguments(CommandContext ctx) throws CommandFormatException {
        final Map<String, CommandArgument> allArgs = getAllArguments(ctx);
        if(!allArgs.keySet().containsAll(ctx.getParsedCommandLine().getPropertyNames())) {
            final Set<String> unrecognized = new HashSet<String>(ctx.getParsedCommandLine().getPropertyNames());
            unrecognized.removeAll(allArgs.keySet());
            throw new CommandFormatException("Unrecognized arguments: " + unrecognized);
        }
    }

    private Map<String,CommandArgument> allArgs;
    protected Map<String, CommandArgument> getAllArguments(CommandContext ctx) {
        if(allArgs == null) {
            allArgs = loadArguments(ctx);
            allArgs.putAll(staticArgs);
        }
        return allArgs;
    }

    protected Map<String, CommandArgument> loadArguments(CommandContext ctx) {
        final Map<String, CommandArgument> allArgs = new HashMap<String, CommandArgument>();
        for(String opName : ops) {
            try {
                allArgs.putAll(getOperationArguments(ctx, opName));
            } catch (CommandLineException e) {
                return Collections.emptyMap();
            }
        }
        return allArgs;
    }

    protected Map<String, ArgumentWithValue> getOperationArguments(CommandContext ctx, String opName) throws CommandLineException {
        Map<String, ArgumentWithValue> args = opArgs.get(opName);
        if(args != null) {
            return args;
        }

        final ModelNode descr = getOperationDescription(ctx, opName);
        if(descr.has(Util.REQUEST_PROPERTIES)) {
            args = new HashMap<String,ArgumentWithValue>();
            final List<Property> propList = descr.get(Util.REQUEST_PROPERTIES).asPropertyList();
            for (Property prop : propList) {
                final ModelNode propDescr = prop.getValue();
                ModelType type = null;
                CommandLineCompleter valueCompleter = null;
                ArgumentValueConverter valueConverter = null;
                if(propConverters != null) {
                    valueConverter = propConverters.get(prop.getName());
                }
                if(valueCompleters != null) {
                    valueCompleter = valueCompleters.get(prop.getName());
                }
                if(valueConverter == null) {
                    valueConverter = ArgumentValueConverter.DEFAULT;
                    if(propDescr.has(Util.TYPE)) {
                        type = propDescr.get(Util.TYPE).asType();
                        if(ModelType.BOOLEAN == type) {
                            if(valueCompleter == null) {
                                valueCompleter = SimpleTabCompleter.BOOLEAN;
                            }
                        } else if(prop.getName().endsWith("properties")) { // TODO this is bad but can't rely on proper descriptions
                            valueConverter = ArgumentValueConverter.PROPERTIES;
                        } else if(ModelType.LIST == type) {
                            if(propDescr.hasDefined(Util.VALUE_TYPE) && propDescr.get(Util.VALUE_TYPE).asType() == ModelType.PROPERTY) {
                                valueConverter = ArgumentValueConverter.PROPERTIES;
                            } else {
                                valueConverter = ArgumentValueConverter.LIST;
                            }
                        } else if(ModelType.OBJECT == type) {
                            valueConverter = ArgumentValueConverter.OBJECT;
                        }
                    }
                }
                final ArgumentWithValue arg = new ArgumentWithValue(ResourceCompositeOperationHandler.this, valueCompleter, valueConverter, "--" + prop.getName());
                args.put(arg.getFullName(), arg);
            }
        } else {
            args = Collections.emptyMap();
        }
        opArgs.put(opName, args);
        return args;
    }

    protected ModelNode getOperationDescription(CommandContext ctx, String operationName) throws CommandLineException {
        final ModelNode request = initRequest(ctx);
        request.get(Util.OPERATION).set(Util.READ_OPERATION_DESCRIPTION);
        request.get(Util.NAME).set(operationName);
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            throw new CommandLineException("Failed to execute read-operation-description.", e);
        }
        if (!result.hasDefined(Util.RESULT)) {
            throw new CommandLineException("Operation description received no result.");
        }
        return result.get(Util.RESULT);
    }

    protected ModelNode initRequest(CommandContext ctx) throws CommandLineException {
        ModelNode request = new ModelNode();
        ModelNode address = request.get(Util.ADDRESS);
        if(isDependsOnProfile() && ctx.isDomainMode()) {
            final String profileName = profile.getValue(ctx.getParsedCommandLine());
            if(profileName == null) {
                throw new CommandLineException("WARNING: --profile argument is required for the complete description.");
            }
            address.add(Util.PROFILE, profileName);
        }
        for(OperationRequestAddress.Node node : getRequiredAddress()) {
            address.add(node.getType(), node.getName());
        }
        address.add(getRequiredType(), "?");
        return request;
    }
}
