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
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author Alexey Loubyansky
 */
public class ReadOperationHandler extends BaseOperationCommand {

    private final ArgumentWithValue node;
    private final ArgumentWithValue name;

    public ReadOperationHandler(CommandContext ctx) {
        super(ctx, "read-operation", true);

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
                    req.get(Util.OPERATION).set(Util.READ_OPERATION_NAMES);
                    try {
                        final ModelNode response = ctx.getModelControllerClient().execute(req);
                        return Util.getList(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (CommandFormatException e) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();
            }}), 0, "--name");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.OperationCommand#buildRequest(org.jboss.as.cli.CommandContext)
     */
    @Override
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {

        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        final String name = this.name.getValue(parsedCmd);
        if(name == null || name.isEmpty()) {
            final OperationRequestAddress address = getAddress(ctx);
            return Util.buildRequest(ctx, address, Util.READ_OPERATION_NAMES);
        }

        final OperationRequestAddress address = getAddress(ctx);
        ModelNode req = Util.buildRequest(ctx, address, Util.READ_OPERATION_DESCRIPTION);
        req.get(Util.NAME).set(name);
        return req;
    }

    protected void handleResponse(CommandContext ctx, ModelNode response, boolean composite) throws CommandFormatException {
        if (!Util.isSuccess(response)) {
            throw new CommandFormatException(Util.getFailureDescription(response));
        }
        if(!response.hasDefined(Util.RESULT)) {
            return;
        }

        boolean opDescr;
        try {
            opDescr = name.isPresent(ctx.getParsedCommandLine());
        } catch (CommandFormatException e) {
            throw new CommandFormatException("Failed to read argument " + name.getFullName() + ": " + e.getLocalizedMessage());
        }

        if(opDescr) {
            final ModelNode result = response.get(Util.RESULT);

            if(result.has(Util.DESCRIPTION)) {
                ctx.printLine("\n\t" + result.get(Util.DESCRIPTION).asString());
            } else {
                ctx.printLine("Operation description is not available.");
            }

            final StringBuilder buf = new StringBuilder();
            buf.append("\n\nPARAMETERS\n");
            if(result.has(Util.REQUEST_PROPERTIES)) {
                final List<Property> props = result.get(Util.REQUEST_PROPERTIES).asPropertyList();
                if(props.isEmpty()) {
                    buf.append("\n\tn/a\n");
                } else {
                    for(Property prop : props) {
                        buf.append('\n');
                        buf.append(prop.getName()).append("\n\n");

                        final List<Property> propProps = prop.getValue().asPropertyList();
                        final SimpleTable table = new SimpleTable(2);
                        for(Property propProp : propProps) {
                            if(propProp.getName().equals(Util.DESCRIPTION)) {
                                buf.append('\t').append(propProp.getValue().asString()).append("\n\n");
                            } else if(!propProp.getName().equals(Util.VALUE_TYPE)) {
                                // TODO not detailing the value-type here, it's readability/formatting issue
                                table.addLine(new String[]{'\t' + propProp.getName() + ':', propProp.getValue().asString()});
                            }
                        }
                        table.append(buf, false);
                        buf.append('\n');
                    }
                }
            } else {
                buf.append("\n\tn/a\n");
            }
            ctx.printLine(buf.toString());

            buf.setLength(0);
            buf.append("\nRESPONSE\n");

            if(result.has(Util.REPLY_PROPERTIES)) {
                final List<Property> props = result.get(Util.REPLY_PROPERTIES).asPropertyList();
                if(props.isEmpty()) {
                    buf.append("\n\tn/a\n");
                } else {
                    buf.append('\n');

                    final SimpleTable table = new SimpleTable(2);
                    StringBuilder vtBuf = null;
                    for(Property prop : props) {
                        if(prop.getName().equals(Util.DESCRIPTION)) {
                            buf.append('\t').append(prop.getValue().asString()).append("\n\n");
                        } else if(prop.getName().equals(Util.VALUE_TYPE)) {
                            final List<Property> vtProps = prop.getValue().asPropertyList();
                            if(!vtProps.isEmpty()) {
                                vtBuf = new StringBuilder();
                                for(Property vtProp : vtProps) {
                                    vtBuf.append('\n').append(vtProp.getName()).append("\n\n");
                                    final List<Property> vtPropProps = vtProp.getValue().asPropertyList();
                                    final SimpleTable vtTable = new SimpleTable(2);
                                    for(Property vtPropProp : vtPropProps) {
                                        if(vtPropProp.getName().equals(Util.DESCRIPTION)) {
                                            vtBuf.append('\t').append(vtPropProp.getValue().asString()).append("\n\n");
                                        } else if(!vtPropProp.getName().equals(Util.VALUE_TYPE)) {
                                            // TODO not detailing the value-type here, it's readability/formatting issue
                                            vtTable.addLine(new String[]{'\t' + vtPropProp.getName() + ':', vtPropProp.getValue().asString()});
                                        }
                                    }
                                    vtTable.append(vtBuf, false);
                                    vtBuf.append('\n');
                                }
                            }
                        } else {
                            table.addLine(new String[]{'\t' + prop.getName() + ':', prop.getValue().asString()});
                        }
                    }
                    table.append(buf, false);
                    buf.append('\n');

                    if(vtBuf != null) {
                        buf.append(vtBuf);
                    }
                }
            } else {
                buf.append("\n\tn/a\n");
            }
            ctx.printLine(buf.toString());
        } else {
            ctx.printColumns(Util.getList(response));
        }
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
