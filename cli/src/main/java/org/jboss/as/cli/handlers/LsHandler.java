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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

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

            // this is for correct parsing of escaped characters
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
                ctx.error(e.getLocalizedMessage());
            }
        } else {
            address = new DefaultOperationRequestAddress(ctx.getPrefix());
        }

        List<String> names = null;
        if(address.endsOnType()) {
            final String type = address.getNodeType();
            address.toParentNode();
            names = Util.getNodeNames(ctx.getModelControllerClient(), address, type);
        } else {
            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);

            {
                final ModelNode typesRequest = new ModelNode();
                typesRequest.get(Util.OPERATION).set(Util.READ_CHILDREN_TYPES);
                final ModelNode addressNode = typesRequest.get(Util.ADDRESS);
                if (address.isEmpty()) {
                    addressNode.setEmptyList();
                } else {
                    Iterator<Node> iterator = address.iterator();
                    while (iterator.hasNext()) {
                        OperationRequestAddress.Node node = iterator.next();
                        if (node.getName() != null) {
                            addressNode.add(node.getType(), node.getName());
                        } else if (iterator.hasNext()) {
                            throw new OperationFormatException("Expected a node name for type '" + node.getType()
                                    + "' in path '" + ctx.getPrefixFormatter().format(address) + "'");
                        }
                    }
                }
                steps.add(typesRequest);
            }

            {
                final ModelNode resourceRequest = new ModelNode();
                resourceRequest.get(Util.OPERATION).set(Util.READ_RESOURCE);
                final ModelNode addressNode = resourceRequest.get(Util.ADDRESS);
                if (address.isEmpty()) {
                    addressNode.setEmptyList();
                } else {
                    Iterator<Node> iterator = address.iterator();
                    while (iterator.hasNext()) {
                        OperationRequestAddress.Node node = iterator.next();
                        if (node.getName() != null) {
                            addressNode.add(node.getType(), node.getName());
                        } else if (iterator.hasNext()) {
                            throw new OperationFormatException("Expected a node name for type '" + node.getType()
                                    + "' in path '" + ctx.getPrefixFormatter().format(address) + "'");
                        }
                    }
                }
                resourceRequest.get(Util.INCLUDE_RUNTIME).set(Util.TRUE);
                steps.add(resourceRequest);
            }

            final String[] additionalProps;
            if(l.isPresent(parsedCmd)) {
                steps.add(Util.buildRequest(ctx, address, Util.READ_RESOURCE_DESCRIPTION));
                final Set<String> argNames = parsedCmd.getPropertyNames();
                if(argNames.size() > 1) {
                    additionalProps = new String[argNames.size() - 1];
                    int i = 0;
                    for(String arg : argNames) {
                        if(arg.equals(l.getFullName())) {
                            continue;
                        }
                        final String prop;
                        if(arg.length() > 1 && arg.charAt(0) == '-') {
                            if(arg.charAt(1) == '-') {
                                prop = arg.substring(2);
                            } else {
                                prop = arg.substring(1);
                            }
                        } else {
                            prop = arg;
                        }
                        additionalProps[i++] = prop;
                    }
                } else {
                    additionalProps = null;
                }
            } else {
                additionalProps = null;
            }

            try {
                ModelNode outcome = ctx.getModelControllerClient().execute(composite);
                if(Util.isSuccess(outcome)) {
                    if(outcome.hasDefined(Util.RESULT)) {
                        ModelNode resultNode = outcome.get(Util.RESULT);

                        ModelNode attrDescriptions = null;
                        ModelNode childDescriptions = null;
                        if(resultNode.hasDefined(Util.STEP_3)) {
                            final ModelNode stepOutcome = resultNode.get(Util.STEP_3);
                            if(Util.isSuccess(stepOutcome)) {
                                if(stepOutcome.hasDefined(Util.RESULT)) {
                                    final ModelNode descrResult = stepOutcome.get(Util.RESULT);
                                    if(descrResult.hasDefined(Util.ATTRIBUTES)) {
                                        attrDescriptions = descrResult.get(Util.ATTRIBUTES);
                                    }
                                    if(descrResult.hasDefined(Util.CHILDREN)) {
                                        childDescriptions = descrResult.get(Util.CHILDREN);
                                    }
                                } else {
                                    ctx.error("Result is not available for read-resource-description request: " + outcome);
                                }
                            } else {
                                ctx.error("Failed to get resource description: " + outcome);
                            }
                        }

                        List<String> typeNames = null;
                        if(resultNode.hasDefined(Util.STEP_1)) {
                            ModelNode typesOutcome = resultNode.get(Util.STEP_1);
                            if(Util.isSuccess(typesOutcome)) {
                                if(typesOutcome.hasDefined(Util.RESULT)) {
                                    final ModelNode resourceResult = typesOutcome.get(Util.RESULT);
                                    final List<ModelNode> types = resourceResult.asList();
                                    if (!types.isEmpty()) {
                                        typeNames = new ArrayList<String>();
                                        for (ModelNode type : types) {
                                            typeNames.add(type.asString());
                                        }
                                        if(childDescriptions == null && attrDescriptions == null) {
                                            names = typeNames;
                                        }
                                    }
                                } else {
                                    ctx.error("Result is not available for read-children-types request: " + outcome);
                                }
                            } else {
                                ctx.error("Failed to fetch type names: " + outcome);
                            }
                        } else {
                            ctx.error("The result for children type names is not available: " + outcome);
                        }

                        if(resultNode.hasDefined(Util.STEP_2)) {
                            ModelNode resourceOutcome = resultNode.get(Util.STEP_2);
                            if(Util.isSuccess(resourceOutcome)) {
                                if(resourceOutcome.hasDefined(Util.RESULT)) {
                                    final ModelNode resourceResult = resourceOutcome.get(Util.RESULT);
                                    final List<Property> props = resourceResult.asPropertyList();
                                    if (!props.isEmpty()) {
                                        final SimpleTable attrTable;
                                        if(attrDescriptions == null) {
                                            attrTable = null;
                                        } else {
                                            if(additionalProps != null) {
                                                String[] headers = new String[3 + additionalProps.length];
                                                headers[0] = "ATTRIBUTE";
                                                headers[1] = "VALUE";
                                                headers[2] = "TYPE";
                                                int i = 3;
                                                for(String additional : additionalProps) {
                                                    headers[i++] = additional.toUpperCase();
                                                }
                                                attrTable = new SimpleTable(headers);
                                            } else {
                                                attrTable = new SimpleTable(new String[]{"ATTRIBUTE", "VALUE", "TYPE"});
                                            }
                                        }
                                        SimpleTable childrenTable = childDescriptions == null ? null :
                                            new SimpleTable(new String[]{"CHILD", "MIN-OCCURS", "MAX-OCCURS"});
                                        if(typeNames == null && attrTable == null && childrenTable == null) {
                                            typeNames = new ArrayList<String>();
                                        }

                                        for (Property prop : props) {
                                            final StringBuilder buf = new StringBuilder();
                                            if(typeNames == null || !typeNames.contains(prop.getName())) {
                                                if(attrDescriptions == null) {
                                                    buf.append(prop.getName());
                                                    buf.append('=');
                                                    buf.append(prop.getValue().asString());
// TODO the value should be formatted nicer but the current formatter uses new lines for complex value which doesn't work here
//                                                    final ModelNode value = prop.getValue();
//                                                    ModelNodeFormatter.Factory.forType(value.getType()).format(buf, 0, value);
                                                    typeNames.add(buf.toString());
                                                    buf.setLength(0);
                                                } else {
                                                    final String[] line = new String[attrTable.columnsTotal()];
                                                    line[0] = prop.getName();
                                                    line[1] = prop.getValue().asString();
                                                    if(attrDescriptions.hasDefined(prop.getName())) {
                                                        final ModelNode attrDescr = attrDescriptions.get(prop.getName());
                                                        line[2] = getAsString(attrDescr, Util.TYPE);
                                                        if(additionalProps != null) {
                                                            int i = 3;
                                                            for(String additional : additionalProps) {
                                                                line[i++] = getAsString(attrDescr, additional);
                                                            }
                                                        }
                                                    } else {
                                                        for(int i = 2; i < line.length; ++i) {
                                                            line[i] = "n/a";
                                                        }
                                                    }
                                                    attrTable.addLine(line);
                                                }
                                            } else if(childDescriptions != null) {
                                                if(childDescriptions.hasDefined(prop.getName())) {
                                                    final ModelNode childDescr = childDescriptions.get(prop.getName());
                                                    final Integer maxOccurs = getAsInteger(childDescr, Util.MAX_OCCURS);
                                                    childrenTable.addLine(new String[]{prop.getName(),
                                                            getAsString(childDescr, Util.MIN_OCCURS),
                                                            maxOccurs == null ? "n/a" : (maxOccurs == Integer.MAX_VALUE ? "unbounded" : maxOccurs.toString())
                                                            });
                                                } else {
                                                    childrenTable.addLine(new String[]{prop.getName(), "n/a", "n/a"});
                                                }
                                            }
                                        }

                                        StringBuilder buf = null;
                                        if(attrTable != null && !attrTable.isEmpty()) {
                                            buf = new StringBuilder();
                                            attrTable.append(buf, true);
                                        }
                                        if(childrenTable != null && !childrenTable.isEmpty()) {
                                            if(buf == null) {
                                                buf = new StringBuilder();
                                            } else {
                                                buf.append("\n\n");
                                            }
                                            childrenTable.append(buf, true);
                                        }
                                        if(buf != null) {
                                            ctx.printLine(buf.toString());
                                        }
                                    }
                                } else {
                                    ctx.error("Result is not available for read-resource request: " + outcome);
                                }
                            } else {
                                ctx.error("Failed to fetch attributes: " + outcome);
                            }
                        } else {
                            ctx.error("The result for attributes is not available: " + outcome);
                        }
                    }
                } else {
                    ctx.error("Failed to fetch the list of children: " + outcome);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(names != null) {
            printList(ctx, names, l.isPresent(parsedCmd));
        }
    }

    protected String getAsString(final ModelNode attrDescr, String name) {
        if(attrDescr == null) {
            return "n/a";
        }
        return attrDescr.has(name) ? attrDescr.get(name).asString() : "n/a";
    }

    protected Integer getAsInteger(final ModelNode attrDescr, String name) {
        if(attrDescr == null) {
            return null;
        }
        return attrDescr.has(name) ? attrDescr.get(name).asInt() : null;
    }

    /*
    public static void main(String[] args) throws Exception {

        //System.out.printf("%-8s %-11s %-8s %-11s", "name", "type", "required", "access-type");
        SimpleTable t = new SimpleTable(new String[]{"NAME", "TYPE", "REQUIRED", "ACCESS-TYPE", "VALUE"});
        t.addLine(new String[]{"name1", "big_integer", "true", "read-write", "12"});
        t.addLine(new String[]{"some name", "int", "false", "read-only", null});
        System.out.println(t.toString());
    }
    */
}
