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
import java.util.Set;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.ModelNodeFormatter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ReadAttributeHandler extends BaseOperationCommand {

    private final ArgumentWithValue node;
    private final ArgumentWithValue name;
    private final ArgumentWithValue includeDefaults;
    private final ArgumentWithoutValue verbose;

    public ReadAttributeHandler(CommandContext ctx) {
        super(ctx, "read-attribute", true);

        node = new ArgumentWithValue(this, OperationRequestCompleter.ARG_VALUE_COMPLETER, "--node");

        name = new ArgumentWithValue(this, new DefaultCompleter(new DefaultCompleter.CandidatesProvider() {
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                try {
                    final OperationRequestAddress address = getAddress(ctx);
                    final ModelNode req = new ModelNode();
                    if(address.isEmpty()) {
                        req.get(Util.ADDRESS).setEmptyList();
                    } else {
                        if(address.endsOnType()) {
                            return Collections.emptyList();
                        }
                        final ModelNode addrNode = req.get(Util.ADDRESS);
                        for(OperationRequestAddress.Node node : address) {
                            addrNode.add(node.getType(), node.getName());
                        }
                    }
                    req.get(Util.OPERATION).set(Util.READ_RESOURCE_DESCRIPTION);
                    try {
                        final ModelNode response = ctx.getModelControllerClient().execute(req);
                        if(Util.isSuccess(response)) {
                            if(response.hasDefined(Util.RESULT)) {
                                final ModelNode result = response.get(Util.RESULT);
                                if(result.hasDefined(Util.ATTRIBUTES)) {
                                    Set<String> attributes = result.get(Util.ATTRIBUTES).keys();
                                    if(attributes.isEmpty()) {
                                        return Collections.emptyList();
                                    }
                                    final List<String> candidates = new ArrayList<String>(attributes.size());
                                    candidates.addAll(attributes);
                                    return candidates;
                                } else {
                                    return Collections.emptyList();
                                }
                            } else {
                                return Collections.emptyList();
                            }
                        } else {
                            return Collections.emptyList();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (CommandFormatException e) {
                    ctx.printLine(e.getLocalizedMessage());
                    return Collections.emptyList();
                }
                return Collections.emptyList();
            }}), 0, "--name");

        includeDefaults = new ArgumentWithValue(this, SimpleTabCompleter.BOOLEAN, "--include-defaults");

        verbose = new ArgumentWithoutValue(this, "--verbose", "-v");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.OperationCommand#buildRequest(org.jboss.as.cli.CommandContext)
     */
    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {

        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        final String name = this.name.getValue(parsedCmd);
        if(name == null || name.isEmpty()) {
            throw new CommandFormatException("Required argument " + this.name.getFullName() + " is not specified.");
        }

        final OperationRequestAddress address = getAddress(ctx);
        ModelNode req = Util.buildRequest(ctx, address, Util.READ_ATTRIBUTE);
        req.get(Util.NAME).set(name);

        final String includeDefaults = this.includeDefaults.getValue(parsedCmd);
        if(includeDefaults != null && !includeDefaults.isEmpty()) {
            req.get(Util.INCLUDE_DEFAULTS).set(includeDefaults);
        }

        if(verbose.isPresent(parsedCmd)) {
            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);
            steps.add(req);
            steps.add(Util.buildRequest(ctx, address, Util.READ_RESOURCE_DESCRIPTION));
            req = composite;
        }

        return req;
    }

    protected void handleResponse(CommandContext ctx, ModelNode response, boolean composite) {
        if (!Util.isSuccess(response)) {
            ctx.printLine(Util.getFailureDescription(response));
            return;
        }
        if(!response.hasDefined(Util.RESULT)) {
            return;
        }

        final ModelNode result = response.get(Util.RESULT);

        if(composite) {
            final SimpleTable table = new SimpleTable(2);
            final StringBuilder valueBuf = new StringBuilder();
            if(result.hasDefined(Util.STEP_1)) {
                final ModelNode stepOutcome = result.get(Util.STEP_1);
                if(Util.isSuccess(stepOutcome)) {
                    if(stepOutcome.hasDefined(Util.RESULT)) {
                        final ModelNode valueResult = stepOutcome.get(Util.RESULT);
                        final ModelNodeFormatter formatter = ModelNodeFormatter.Factory.forType(valueResult.getType());
                        formatter.format(valueBuf, 0, valueResult);
                    } else {
                        valueBuf.append("n/a");
                    }
                    table.addLine(new String[]{"value", valueBuf.toString()});
                } else {
                    ctx.printLine("Failed to get resource description: " + response);
                }
            }

            if(result.hasDefined(Util.STEP_2)) {
                final ModelNode stepOutcome = result.get(Util.STEP_2);
                if(Util.isSuccess(stepOutcome)) {
                    if(stepOutcome.hasDefined(Util.RESULT)) {
                        final ModelNode descrResult = stepOutcome.get(Util.RESULT);
                        if(descrResult.hasDefined(Util.ATTRIBUTES)) {
                            ModelNode attributes = descrResult.get(Util.ATTRIBUTES);
                            final String name = this.name.getValue(ctx.getParsedCommandLine());
                            if(name == null) {
                                ctx.printLine("Attribute name is not available in handleResponse.");
                            } else if(attributes.hasDefined(name)) {
                                final ModelNode descr = attributes.get(name);
                                for(String prop : descr.keys()) {
                                    table.addLine(new String[]{prop, descr.get(prop).asString()});
                                }
                            } else {
                                ctx.printLine("Attribute description is not available.");
                            }
                        } else {
                            ctx.printLine("The resource doesn't provide attribute descriptions.");
                        }
                    } else {
                        ctx.printLine("Result is not available for read-resource-description request: " + response);
                    }
                } else {
                    ctx.printLine("Failed to get resource description: " + response);
                }
            }
            ctx.printLine(table.toString(true));
        } else {
            final ModelNodeFormatter formatter = ModelNodeFormatter.Factory.forType(result.getType());
            final StringBuilder buf = new StringBuilder();
            formatter.format(buf, 0, result);
            ctx.printLine(buf.toString());
        }
    }

    protected StringBuilder formatResponse(CommandContext ctx, ModelNode opResponse, boolean composite, StringBuilder buf) {
        if(!opResponse.hasDefined(Util.RESULT)) {
            return null;
        }
        final ModelNode result = opResponse.get(Util.RESULT);
        if(composite) {
            final Set<String> keys;
            try {
                keys = result.keys();
            } catch(Exception e) {
                ctx.printLine("Failed to get step results from a composite operation response " + opResponse);
                e.printStackTrace();
                return null;
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

    protected OperationRequestAddress getAddress(CommandContext ctx) throws CommandFormatException {
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final OperationRequestAddress address;
        if (node.isPresent(args)) {
            address = new DefaultOperationRequestAddress(ctx.getPrefix());
            CommandLineParser.CallbackHandler handler = new DefaultCallbackHandler(address);

            // this is for correct parsing of escaped characters
            String nodePath = args.getOriginalLine();
            int nodeArgInd = nodePath.indexOf(" --node=");
            if(nodeArgInd < 0) {
                throw new CommandFormatException("Couldn't locate ' --node=' in the line: '" + nodePath + "'");
            }

            int nodeArgEndInd = nodeArgInd + 8;
            do {
                nodeArgEndInd = nodePath.indexOf(' ', nodeArgEndInd);
                if(nodeArgEndInd < 0) {
                    nodeArgEndInd = nodePath.length();
                } else if(nodePath.charAt(nodeArgEndInd - 1) == '\\') {
                    ++nodeArgEndInd;
                } else {
                    break;
                }
            } while(nodeArgEndInd < nodePath.length());
            nodePath = nodePath.substring(nodeArgInd + 8, nodeArgEndInd);
            ctx.getCommandLineParser().parse(nodePath, handler);
        } else {
            address = new DefaultOperationRequestAddress(ctx.getPrefix());
        }
        return address;
    }
}
