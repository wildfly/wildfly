/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.ModelNodeFormatter;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;


/**
 *
 * @author Alexey Loubyansky
 */
public class GenericTypeOperationHandler extends BatchModeCommandHandler {

    protected final String commandName;
    protected final String idProperty;
    protected final String nodeType;
    protected final ArgumentWithValue profile;
    protected final ArgumentWithValue name;
    protected final ArgumentWithValue operation;

    protected final Set<String> excludedOps;

    // help arguments
    protected final ArgumentWithoutValue helpProperties;
    protected final ArgumentWithoutValue helpCommands;

    // these are caching vars
    private final Map<String, CommandArgument> staticArgs = new HashMap<String, CommandArgument>();

    private Map<String, ArgumentValueConverter> propConverters;
    private Map<String, CommandLineCompleter> valueCompleters;

    private Map<String, OperationCommandWithDescription> customHandlers;

    private WritePropertyHandler writePropHandler;
    private Map<String, OperationCommand> opHandlers;

    public GenericTypeOperationHandler(CommandContext ctx, String nodeType, String idProperty) {
        this(ctx, nodeType, idProperty, "read-attribute", "read-children-names", "read-children-resources",
                "read-children-types", "read-operation-description", "read-operation-names",
                "read-resource-description", "validate-address", "write-attribute", "undefine-attribute", "whoami");
    }

    public GenericTypeOperationHandler(CommandContext ctx, String nodeType, String idProperty, String... excludeOperations) {

        super(ctx, "generic-type-operation", true);

        if(nodeType == null || nodeType.isEmpty()) {
            throw new IllegalArgumentException("Node type is " + (nodeType == null ? "null." : "empty."));
        }

        if(nodeType.startsWith("/profile=") || nodeType.startsWith("profile=")) {
            int nextSep = nodeType.indexOf('/', 7);
            if(nextSep < 0) {
                throw new IllegalArgumentException("Failed to determine the path after the profile in '" + nodeType + "'.");
            }
            nodeType = nodeType.substring(nextSep);
            this.nodeType = nodeType;
        } else {
            this.nodeType = nodeType;
        }

        helpArg = new ArgumentWithoutValue(this, "--help", "-h");

        addRequiredPath(nodeType);
        this.commandName = getRequiredType();
        if(this.commandName == null) {
            throw new IllegalArgumentException("The node path doesn't end on a type: '" + nodeType + "'");
        }
        this.idProperty = idProperty;

        if(excludeOperations != null) {
            this.excludedOps = new HashSet<String>(Arrays.asList(excludeOperations));
        } else {
            excludedOps = Collections.emptySet();
        }

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
        //profile.addCantAppearAfter(helpArg);

        operation = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
                @Override
                public Collection<String> getAllCandidates(CommandContext ctx) {
                    DefaultOperationRequestAddress address = new DefaultOperationRequestAddress();
                    if(isDependsOnProfile() && ctx.isDomainMode()) {
                        final String profileName = profile.getValue(ctx.getParsedCommandLine());
                        if(profileName == null) {
                            return Collections.emptyList();
                        }
                        address.toNode("profile", profileName);
                    }
                    for(OperationRequestAddress.Node node : getRequiredAddress()) {
                        address.toNode(node.getType(), node.getName());
                    }
                    address.toNode(getRequiredType(), "?");
                    Collection<String> ops = ctx.getOperationCandidatesProvider().getOperationNames(ctx, address);
                    ops.removeAll(excludedOps);
                    if(customHandlers != null) {
                        ops.addAll(customHandlers.keySet());
                    }
                    return ops;
                }}), 0, "--operation") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(isDependsOnProfile() && ctx.isDomainMode() && !profile.isValueComplete(ctx.getParsedCommandLine())) {
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

        helpProperties = new ArgumentWithoutValue(this, "--properties");
        helpProperties.addRequiredPreceding(helpArg);
        helpProperties.addCantAppearAfter(operation);

        helpCommands = new ArgumentWithoutValue(this, "--commands");
        helpCommands.addRequiredPreceding(helpArg);
        helpCommands.addCantAppearAfter(operation);
        helpCommands.addCantAppearAfter(helpProperties);
        helpProperties.addCantAppearAfter(helpCommands);


        ///
        staticArgs.put(helpArg.getFullName(), helpArg);
        staticArgs.put(helpCommands.getFullName(), helpCommands);
        staticArgs.put(helpProperties.getFullName(), helpProperties);
        staticArgs.put(profile.getFullName(), profile);
        staticArgs.put(name.getFullName(), name);
        staticArgs.put(operation.getFullName(), operation);
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

    public void addHandler(String name, OperationCommandWithDescription handler) {
        if(customHandlers == null) {
            customHandlers = new HashMap<String, OperationCommandWithDescription>();
        }
        customHandlers.put(name, handler);
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
        final String op = operation.getValue(args);
        return getHandler(ctx, op).getArgument(ctx, name);
    }

    @Override
    public Collection<CommandArgument> getArguments(CommandContext ctx) {
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        try {
            if(!name.isValueComplete(args)) {
                return staticArgs.values();
            }
        } catch (CommandFormatException e) {
            return Collections.emptyList();
        }
        final String op = operation.getValue(args);
        return getHandler(ctx, op).getArguments(ctx);
    }

    private OperationCommand getHandler(CommandContext ctx, String op) {
        if(op == null) {
            if(writePropHandler == null) {
                writePropHandler = new WritePropertyHandler();
                List<Property> propList;
                try {
                    propList = getNodeProperties(ctx);
                } catch (CommandFormatException e) {
                    propList = Collections.emptyList();
                }
                for(int i = 0; i < propList.size(); ++i) {
                    final Property prop = propList.get(i);
                    final ModelNode propDescr = prop.getValue();
                    if(propDescr.has(Util.ACCESS_TYPE) && Util.READ_WRITE.equals(propDescr.get(Util.ACCESS_TYPE).asString())) {
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
                        final CommandArgument arg = new ArgumentWithValue(GenericTypeOperationHandler.this, valueCompleter, valueConverter, "--" + prop.getName());
                        writePropHandler.addArgument(arg);
                    }
                }
            }
            return writePropHandler;
        } else {
            if(customHandlers != null && customHandlers.containsKey(op)) {
                final OperationCommand opHandler = customHandlers.get(op);
                if(opHandler != null) {
                    return opHandler;
                }
            }

            if(opHandlers != null) {
                OperationCommand opHandler = opHandlers.get(op);
                if(opHandler != null) {
                    return opHandler;
                }
            }

            final ModelNode descr;
            try {
                descr = getOperationDescription(ctx, op);
            } catch (CommandLineException e) {
                return null;
            }

            if(opHandlers == null) {
                opHandlers = new HashMap<String, OperationCommand>();
            }
            final OpHandler opHandler = new OpHandler(op);
            opHandlers.put(op, opHandler);
            opHandler.addArgument(this.headers);

            if(descr != null && descr.has(Util.REQUEST_PROPERTIES)) {
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
                    final CommandArgument arg = new ArgumentWithValue(GenericTypeOperationHandler.this, valueCompleter, valueConverter, "--" + prop.getName());
                    opHandler.addArgument(arg);
                }
            }
            return opHandler;
        }
    }

    @Override
    public boolean hasArgument(CommandContext ctx, String name) {
        return true;
    }

    @Override
    public boolean hasArgument(CommandContext ctx, int index) {
        return true;
    }

    public void addArgument(CommandArgument arg) {
    }

    // TODO
    protected void recognizeArguments(CommandContext ctx) throws CommandFormatException {
    }

    @Override
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {
        final String operation = this.operation.getValue(ctx.getParsedCommandLine());
        final OperationCommand opHandler = getHandler(ctx, operation);
        if(opHandler == null) {
            throw new CommandFormatException("Unexpected command '" + operation + "'");
        }
        return opHandler.buildRequest(ctx);
    }

    @Override
    protected void handleResponse(CommandContext ctx, ModelNode opResponse, boolean composite) throws CommandFormatException {
        //System.out.println(opResponse);
        if (!Util.isSuccess(opResponse)) {
            throw new CommandFormatException(Util.getFailureDescription(opResponse));
        }
        final StringBuilder buf = formatResponse(ctx, opResponse, composite, null);
        if(buf != null) {
            ctx.printLine(buf.toString());
        }
    }

    protected StringBuilder formatResponse(CommandContext ctx, ModelNode opResponse, boolean composite, StringBuilder buf) throws CommandFormatException {
        if(!opResponse.hasDefined(Util.RESULT)) {
            return null;
        }
        final ModelNode result = opResponse.get(Util.RESULT);
        if(composite) {
            final Set<String> keys;
            try {
                keys = result.keys();
            } catch(Exception e) {
                throw new CommandFormatException("Failed to get step results from a composite operation response " + opResponse);
            }
            for(String key : keys) {
                final ModelNode stepResponse = result.get(key);
                buf = formatResponse(ctx, stepResponse, false, buf); // TODO nested composite ops aren't expected for now
            }
        } else {
            final ModelNodeFormatter formatter = ModelNodeFormatter.Factory.forType(result.getType());
            if(buf == null) {
                buf = new StringBuilder();
            }
            formatter.format(buf, 0, result);
        }
        return buf;
    }

    @Override
    protected void printHelp(CommandContext ctx) throws CommandLineException {

        ParsedCommandLine args = ctx.getParsedCommandLine();
        if(helpProperties.isPresent(args)) {
            printProperties(ctx, getNodeProperties(ctx));
            return;
        }

        if(helpCommands.isPresent(args)) {
            printSupportedCommands(ctx);
            return;
        }

        final String operationName = operation.getValue(args);
        if(operationName == null) {
            printNodeDescription(ctx);
            return;
        }

/*        if(customHandlers != null && customHandlers.containsKey(operationName)) {
            OperationCommand operationCommand = customHandlers.get(operationName);
            operationCommand.handle(ctx);
            return;
        }
*/
        final ModelNode result = getOperationDescription(ctx, operationName);
        if(!result.hasDefined(Util.DESCRIPTION)) {
            throw new CommandLineException("Operation description is not available.");
        }

        final StringBuilder buf = new StringBuilder();
        buf.append("\nDESCRIPTION:\n\n");
        buf.append(result.get(Util.DESCRIPTION).asString());
        ctx.printLine(buf.toString());

        if(result.hasDefined(Util.REQUEST_PROPERTIES)) {
            printProperties(ctx, result.get(Util.REQUEST_PROPERTIES).asPropertyList());
        } else {
            printProperties(ctx, Collections.<Property>emptyList());
        }
    }

    protected void printProperties(CommandContext ctx, List<Property> props) {
        final Map<String, StringBuilder> requiredProps = new LinkedHashMap<String,StringBuilder>();
        requiredProps.put(this.name.getFullName(), new StringBuilder().append("Required argument in commands which identifies the instance to execute the command against."));
        final Map<String, StringBuilder> optionalProps = new LinkedHashMap<String, StringBuilder>();

        String accessType = null;
        for (Property attr : props) {
            final ModelNode value = attr.getValue();

            // filter metrics
            if (value.has(Util.ACCESS_TYPE)) {
                accessType = value.get(Util.ACCESS_TYPE).asString();
//                if("metric".equals(accessType)) {
//                    continue;
//                }
            }

            final boolean required = value.hasDefined("required") ? value.get("required").asBoolean() : false;
            final StringBuilder descr = new StringBuilder();

            final String type = value.has(Util.TYPE) ? value.get(Util.TYPE).asString() : "no type info";
            if (value.hasDefined(Util.DESCRIPTION)) {
                descr.append('(');
                descr.append(type);
                if(accessType != null) {
                    descr.append(',').append(accessType);
                }
                descr.append(") ");
                descr.append(value.get("description").asString());
            } else if(descr.length() == 0) {
                descr.append("no description.");
            }

            if(required) {
                if(idProperty != null && idProperty.equals(attr.getName())) {
                    if(descr.charAt(descr.length() - 1) != '.') {
                        descr.append('.');
                    }
                    requiredProps.get(this.name.getFullName()).insert(0, ' ').insert(0, descr.toString());
                } else {
                    requiredProps.put("--" + attr.getName(), descr);
                }
            } else {
                optionalProps.put("--" + attr.getName(), descr);
            }
        }

        ctx.printLine("\n");
        if(accessType == null) {
            ctx.printLine("REQUIRED ARGUMENTS:\n");
        }
        for(String argName : requiredProps.keySet()) {
            final StringBuilder prop = new StringBuilder();
            prop.append(' ').append(argName);
            int spaces = 28 - prop.length();
            do {
                prop.append(' ');
                --spaces;
            } while(spaces >= 0);
            prop.append("- ").append(requiredProps.get(argName));
            ctx.printLine(prop.toString());
        }

        if(!optionalProps.isEmpty()) {
            if(accessType == null ) {
                ctx.printLine("\n\nOPTIONAL ARGUMENTS:\n");
            }
            for(String argName : optionalProps.keySet()) {
                final StringBuilder prop = new StringBuilder();
                prop.append(' ').append(argName);
                int spaces = 28 - prop.length();
                do {
                    prop.append(' ');
                    --spaces;
                } while(spaces >= 0);
                prop.append("- ").append(optionalProps.get(argName));
                ctx.printLine(prop.toString());
            }
        }
    }

    protected void printNodeDescription(CommandContext ctx) throws CommandFormatException {

        final StringBuilder buf = new StringBuilder();

        buf.append("\nSYNOPSIS\n\n");
        buf.append(commandName).append(" --help [--properties | --commands] |\n");
        if(isDependsOnProfile() && ctx.isDomainMode()) {
            for(int i = 0; i <= commandName.length(); ++i) {
                buf.append(' ');
            }
            buf.append("--profile=<profile_name>\n");
        }
        for(int i = 0; i <= commandName.length(); ++i) {
            buf.append(' ');
        }
        buf.append('(').append(name.getFullName()).append("=<resource_id> (--<property>=<value>)*) |\n");
        for(int i = 0; i <= commandName.length(); ++i) {
            buf.append(' ');
        }
        buf.append("(<command> ").append(name.getFullName()).append("=<resource_id> (--<parameter>=<value>)*)");

        buf.append('\n');
        for(int i = 0; i <= commandName.length(); ++i) {
            buf.append(' ');
        }
        buf.append("[--headers={<operation_header> (;<operation_header>)*}]");

        buf.append("\n\nDESCRIPTION\n\n");
        buf.append("The command is used to manage resources of type " + this.nodeType + ".");

        buf.append("\n\nRESOURCE DESCRIPTION\n\n");

        if(isDependsOnProfile() && ctx.isDomainMode() && profile.getValue(ctx.getParsedCommandLine()) == null) {
            buf.append("(Execute '");
            buf.append(commandName).append(" --profile=<profile_name> --help' to include the resource description here.)");
        } else {
            ModelNode request = initRequest(ctx);
            if(request == null) {
                return;
            }
            request.get(Util.OPERATION).set(Util.READ_RESOURCE_DESCRIPTION);
            ModelNode result = null;
            try {
                result = ctx.getModelControllerClient().execute(request);
                if(!result.hasDefined(Util.RESULT)) {
                    throw new CommandFormatException("Node description is not available.");
                }
                result = result.get(Util.RESULT);
                if(!result.hasDefined(Util.DESCRIPTION)) {
                    throw new CommandFormatException("Node description is not available.");
                }
            } catch (Exception e) {
            }

            if(result != null) {
                buf.append(result.get(Util.DESCRIPTION).asString());
            } else {
                buf.append("N/A. Please, open a jira issue at https://issues.jboss.org/browse/AS7 to get this fixed. Thanks!");
            }
        }

        buf.append("\n\nARGUMENTS\n");

        buf.append("\n--help                - prints this content.");
        buf.append("\n--help --properties   - prints the list of the resource properties including their access-type");
        buf.append("\n                        (read/write/metric), value type, and the description.");
        buf.append("\n--help --commands     - prints the list of the commands available for the resource.");
        buf.append("\n                        To get the complete description of a specific command (including its parameters,");
        buf.append("\n                        their types and descriptions), execute ").append(commandName).append(" <command> --help.");

        if(isDependsOnProfile() && ctx.isDomainMode()) {
            buf.append("\n\n--profile    - the name of the profile the target resource belongs to.");
        }

        buf.append("\n\n").append(name.getFullName()).append("   - ");
        if(idProperty == null) {
            buf.append("is the name of the resource that completes the path ").append(nodeType).append(" and \n");
        } else {
            buf.append("corresponds to a property of the resource which \n");
        }
        for(int i = 0; i < name.getFullName().length() + 5; ++i) {
            buf.append(' ');
        }
        buf.append("is used to identify the resource against which the command should be executed.");

        buf.append("\n\n<property>   - property name of the resource whose value should be updated.");
        buf.append("\n               For a complete list of available property names, their types and descriptions,");
        buf.append("\n               execute ").append(commandName).append(" --help --properties.");

        buf.append("\n\n<command>    - command name provided by the resource. For a complete list of available commands,");
        buf.append("\n               execute ").append(commandName).append(" --help --commands.");

        buf.append("\n\n<parameter>  - parameter name of the <command> provided by the resource.");
        buf.append("\n               For a complete list of available parameter names of a specific <command>,");
        buf.append("\n               their types and descriptions, execute ").append(commandName).append(" <command> --help.");

        buf.append("\n\n--headers    - a list of operation headers separated by a semicolon. For the list of supported");
        buf.append("\n               headers, please, refer to the domain management documentation or use tab-completion.");

        ctx.printLine(buf.toString());
    }

    protected void printSupportedCommands(CommandContext ctx) throws CommandLineException {
        final List<String> list = getSupportedCommands(ctx);
        list.add("To read the description of a specific command execute '" + this.commandName + " command_name --help'.");
        for(String name : list) {
            ctx.printLine(name);
        }
    }

    protected List<String> getSupportedCommands(CommandContext ctx) throws CommandLineException {
        final ModelNode request = initRequest(ctx);
        request.get(Util.OPERATION).set(Util.READ_OPERATION_NAMES);
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            throw new CommandLineException("Failed to load a list of commands.", e);
        }
        if (!result.hasDefined(Util.RESULT)) {
            throw new CommandLineException("Operation names aren't available.");
        }
        final List<ModelNode> nodeList = result.get(Util.RESULT).asList();
        final List<String> supportedCommands = new ArrayList<String>(nodeList.size());
        if(!nodeList.isEmpty()) {
            for(ModelNode node : nodeList) {
                final String opName = node.asString();
                if(!excludedOps.contains(opName) && (customHandlers == null || !customHandlers.containsKey(opName))) {
                    supportedCommands.add(opName);
                }
            }
        }
        if(customHandlers != null) {
            supportedCommands.addAll(customHandlers.keySet());
        }
        Collections.sort(supportedCommands);
        return supportedCommands;
    }

    protected List<Property> getNodeProperties(CommandContext ctx) throws CommandFormatException {
        ModelNode request = initRequest(ctx);
        if(request == null) {
            return Collections.emptyList();
        }
        request.get(Util.OPERATION).set(Util.READ_RESOURCE_DESCRIPTION);
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        if(!result.hasDefined(Util.RESULT)) {
            return Collections.emptyList();
        }
        result = result.get(Util.RESULT);
        if(!result.hasDefined(Util.ATTRIBUTES)) {
            return Collections.emptyList();
        }
        return result.get(Util.ATTRIBUTES).asPropertyList();
    }

    protected ModelNode getOperationDescription(CommandContext ctx, String operationName) throws CommandLineException {
        if(customHandlers != null) {
            final OperationCommandWithDescription handler = customHandlers.get(operationName);
            if(handler != null) {
                return handler.getOperationDescription(ctx);
            }
        }
        ModelNode request = initRequest(ctx);
        if(request == null) {
            return null;
        }
        request.get(Util.OPERATION).set(Util.READ_OPERATION_DESCRIPTION);
        request.get(Util.NAME).set(operationName);
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            throw new CommandFormatException("Failed to execute read-operation-description.", e);
        }
        if (!result.hasDefined(Util.RESULT)) {
            return null;
        }
        return result.get(Util.RESULT);
    }

    protected ModelNode initRequest(CommandContext ctx) throws CommandFormatException {
        ModelNode request = new ModelNode();
        ModelNode address = request.get(Util.ADDRESS);
        if(isDependsOnProfile() && ctx.isDomainMode()) {
            final String profileName = profile.getValue(ctx.getParsedCommandLine());
            if(profileName == null) {
                throw new CommandFormatException("WARNING: --profile argument is required for the complete description.");
            }
            address.add(Util.PROFILE, profileName);
        }
        for(OperationRequestAddress.Node node : getRequiredAddress()) {
            address.add(node.getType(), node.getName());
        }
        address.add(getRequiredType(), "?");
        return request;
    }

    private abstract class ActionHandler implements CommandHandler, OperationCommand {

        protected Map<String, CommandArgument> args = Collections.emptyMap();

        void addArgument(CommandArgument arg) {
            if(arg == null) {
                throw new IllegalArgumentException("Argument can't be null.");
            }
            if(args.isEmpty()) {
                args = new HashMap<String, CommandArgument>();
            }
            args.put(arg.getFullName(), arg);
        }
        @Override
        public boolean isAvailable(CommandContext ctx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isBatchMode(CommandContext ctx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void handle(CommandContext ctx) throws CommandLineException {
            throw new UnsupportedOperationException();
        }

        @Override
        public CommandArgument getArgument(CommandContext ctx, String name) {
            return args.get(name);
        }

        @Override
        public boolean hasArgument(CommandContext ctx, String name) {
            return args.containsKey(name);
        }

        @Override
        public boolean hasArgument(CommandContext ctx, int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<CommandArgument> getArguments(CommandContext ctx) {
            return args.values();
        }
    }

    private class WritePropertyHandler extends ActionHandler {

        @Override
        public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {

            final String name = GenericTypeOperationHandler.this.name.getValue(ctx.getParsedCommandLine(), true);

            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);

            final ParsedCommandLine args = ctx.getParsedCommandLine();

            final String profile;
            if(isDependsOnProfile() && ctx.isDomainMode()) {
                profile = GenericTypeOperationHandler.this.profile.getValue(args);
                if(profile == null) {
                    throw new OperationFormatException("--profile argument value is missing.");
                }
            } else {
                profile = null;
            }

            final Map<String,CommandArgument> nodeProps = this.args;
            for(String argName : args.getPropertyNames()) {
                if(isDependsOnProfile() && argName.equals("--profile") || GenericTypeOperationHandler.this.name.getFullName().equals(argName)) {
                    continue;
                }

                final ArgumentWithValue arg = (ArgumentWithValue) nodeProps.get(argName);
                if(arg == null) {
                    throw new CommandFormatException("Unrecognized argument name '" + argName + "'");
                }

                DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
                if (profile != null) {
                    builder.addNode(Util.PROFILE, profile);
                }

                for(OperationRequestAddress.Node node : getRequiredAddress()) {
                    builder.addNode(node.getType(), node.getName());
                }
                builder.addNode(getRequiredType(), name);
                builder.setOperationName(Util.WRITE_ATTRIBUTE);
                final String propName;
                if(argName.charAt(1) == '-') {
                    propName = argName.substring(2);
                } else {
                    propName = argName.substring(1);
                }
                builder.addProperty(Util.NAME, propName);

                final String valueString = args.getPropertyValue(argName);
                ModelNode nodeValue = arg.getValueConverter().fromString(valueString);
                builder.getModelNode().get(Util.VALUE).set(nodeValue);

                steps.add(builder.buildRequest());
            }

            return composite;
        }
    };

    class OpHandler extends ActionHandler {

        private final String opName;

        OpHandler(String opName) {
            super();
            if(opName == null || opName.isEmpty()) {
                throw new IllegalArgumentException("Operation name must a be non-null non-empty string.");
            }
            this.opName = opName;
        }

        @Override
        public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {
            final ParsedCommandLine parsedArgs = ctx.getParsedCommandLine();

            final ModelNode request = new ModelNode();
            final ModelNode address = request.get(Util.ADDRESS);
            if(isDependsOnProfile() && ctx.isDomainMode()) {
                final String profile = GenericTypeOperationHandler.this.profile.getValue(parsedArgs);
                if(profile == null) {
                    throw new OperationFormatException("Required argument --profile is missing.");
                }
                address.add(Util.PROFILE, profile);
            }

            final String name = GenericTypeOperationHandler.this.name.getValue(ctx.getParsedCommandLine(), true);

            for(OperationRequestAddress.Node node : getRequiredAddress()) {
                address.add(node.getType(), node.getName());
            }
            address.add(getRequiredType(), name);
            request.get(Util.OPERATION).set(opName);

            for(String argName : parsedArgs.getPropertyNames()) {
                if(isDependsOnProfile() && argName.equals("--profile")) {
                    continue;
                }

                if(this.args.isEmpty()) {
                    if(argName.equals(GenericTypeOperationHandler.this.name.getFullName())) {
                        continue;
                    }
                    throw new CommandFormatException("Command '" + operation + "' is not expected to have arguments other than " + GenericTypeOperationHandler.this.name.getFullName() + ".");
                }

                final ArgumentWithValue arg = (ArgumentWithValue) this.args.get(argName);
                if(arg == null) {
                    if(argName.equals(GenericTypeOperationHandler.this.name.getFullName())) {
                        continue;
                    }
                    throw new CommandFormatException("Unrecognized argument " + argName + " for command '" + operation + "'.");
                }

                final String propName;
                if(argName.charAt(1) == '-') {
                    propName = argName.substring(2);
                } else {
                    propName = argName.substring(1);
                }

                final String valueString = parsedArgs.getPropertyValue(argName);
                ModelNode nodeValue = arg.getValueConverter().fromString(valueString);
                request.get(propName).set(nodeValue);
            }
            return request;
        }
    };
}
