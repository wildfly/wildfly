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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
                ctx.printLine(e.getLocalizedMessage());
            }
        } else {
            address = new DefaultOperationRequestAddress(ctx.getPrefix());
        }

        final List<String> names;
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
                steps.add(resourceRequest);
            }

            List<String> result = null;
            try {
                ModelNode outcome = ctx.getModelControllerClient().execute(composite);
                if(Util.isSuccess(outcome)) {
                    if(outcome.hasDefined(Util.RESULT)) {
                        ModelNode resultNode = outcome.get(Util.RESULT);
                        if(resultNode.hasDefined(Util.STEP_1)) {
                            ModelNode typesOutcome = resultNode.get(Util.STEP_1);
                            if(Util.isSuccess(typesOutcome)) {
                                if(typesOutcome.hasDefined(Util.RESULT)) {
                                    final ModelNode resourceResult = typesOutcome.get(Util.RESULT);
                                    final List<Property> props = resourceResult.asPropertyList();
                                    if (!props.isEmpty()) {
                                        result = new ArrayList<String>();
                                        for (Property prop : props) {
                                            if(result == null || !result.contains(prop.getName())) {
                                                result.add(prop.getName());
                                            }
                                        }
                                    }
                                } else {
                                    ctx.printLine("Result is not available for read-children-types request: " + outcome);
                                }
                            } else {
                                ctx.printLine("Failed to fetch type names: " + outcome);
                            }
                        } else {
                            ctx.printLine("The result for children type names is not available: " + outcome);
                        }
                        if(resultNode.hasDefined(Util.STEP_2)) {
                            ModelNode resourceOutcome = resultNode.get(Util.STEP_2);
                            if(Util.isSuccess(resourceOutcome)) {
                                if(resourceOutcome.hasDefined(Util.RESULT)) {
                                    final ModelNode resourceResult = resourceOutcome.get(Util.RESULT);
                                    final List<Property> props = resourceResult.asPropertyList();
                                    if (!props.isEmpty()) {
                                        final List<String> attrs = new ArrayList<String>();
                                        for (Property prop : props) {
                                            final StringBuilder buf = new StringBuilder();
                                            if(result == null || !result.contains(prop.getName())) {
                                                buf.append(prop.getName());
                                                buf.append('=');
                                                buf.append(prop.getValue().asString());
// TODO the value should be formatted nicer but the current fomatter uses new lines for complex value which doesn't work here
//                                                final ModelNode value = prop.getValue();
//                                                ModelNodeFormatter.Factory.forType(value.getType()).format(buf, 0, value);
                                                attrs.add(buf.toString());
                                                buf.setLength(0);
                                            }
                                        }
                                        if(result == null) {
                                            result = attrs;
                                        } else {
                                            result.addAll(attrs);
                                        }
                                    }
                                } else {
                                    ctx.printLine("Result is not available for read-resource request: " + outcome);
                                }
                            } else {
                                ctx.printLine("Failed to fetch attributes: " + outcome);
                            }
                        } else {
                            ctx.printLine("The result for attributes is not available: " + outcome);
                        }
                    }
                } else {
                    ctx.printLine("Failed to fetch the list of children: " + outcome);
                }
            } catch (Exception e) {
            }
            names = result == null ? Collections.<String>emptyList() : result;
        }

        printList(ctx, names, l.isPresent(parsedCmd));
    }
}
