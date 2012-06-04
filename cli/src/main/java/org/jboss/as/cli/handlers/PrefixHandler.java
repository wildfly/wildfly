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

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class PrefixHandler extends CommandHandlerWithHelp {

    private final ArgumentWithValue nodePath;

    public PrefixHandler() {
        this("cn");
    }

    public PrefixHandler(String command) {
        super(command, true);
        nodePath = new ArgumentWithValue(this, OperationRequestCompleter.ARG_VALUE_COMPLETER, 0, "--node-path");
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final String nodePath = this.nodePath.getValue(ctx.getParsedCommandLine());

        OperationRequestAddress prefix = ctx.getCurrentNodePath();

        if(nodePath == null) {
            ctx.printLine(ctx.getNodePathFormatter().format(prefix));
            return;
        }

        final OperationRequestAddress tmp = new DefaultOperationRequestAddress(prefix);
        ctx.getCommandLineParser().parse(ctx.getArgumentsString(), new DefaultCallbackHandler(tmp));
        assertValid(ctx, tmp);

        if(tmp.isEmpty()) {
            prefix.reset();
        } else {
            prefix.reset();
            for(OperationRequestAddress.Node node : tmp) {
                if(node.getName() != null) {
                    prefix.toNode(node.getType(), node.getName());
                } else {
                    prefix.toNodeType(node.getType());
                }
            }
        }
    }

    protected void assertValid(CommandContext ctx, OperationRequestAddress addr) throws CommandLineException {
        ModelNode req = new ModelNode();
        req.get(Util.ADDRESS).setEmptyList();
        req.get(Util.OPERATION).set(Util.VALIDATE_ADDRESS);
        final ModelNode addressValue = req.get(Util.VALUE);
        String lastType = null;
        if(addr.isEmpty()) {
            addressValue.setEmptyList();
        } else {
            for(OperationRequestAddress.Node node : addr) {
                if(node.getName() != null) {
                    addressValue.add(node.getType(), node.getName());
                } else {
                    lastType = node.getType();
                }
            }
        }
        ModelNode response;
        try {
            response = ctx.getModelControllerClient().execute(req);
        } catch (IOException e) {
            throw new CommandLineException("Failed to validate address.", e);
        }
        ModelNode result = response.get(Util.RESULT);
        if(!result.isDefined()) {
            throw new CommandLineException("Failed to validate address: the response from the controller doesn't contain result.");
        }
        final ModelNode valid = result.get(Util.VALID);
        if(!valid.isDefined()) {
            throw new CommandLineException("Failed to validate address: the result doesn't contain 'valid' property.");
        }
        if(!valid.asBoolean()) {
            final String msg;
            if(result.hasDefined(Util.PROBLEM)) {
                msg = result.get(Util.PROBLEM).asString();
            } else {
                msg = "Invalid target address.";
            }
            throw new CommandLineException(msg);
        }

        if(lastType != null) {
            req = new ModelNode();
            req.get(Util.OPERATION).set(Util.READ_CHILDREN_TYPES);
            final ModelNode addrNode = req.get(Util.ADDRESS);
            if(addr.isEmpty()) {
                addrNode.setEmptyList();
            } else {
                for(OperationRequestAddress.Node node : addr) {
                    if(node.getName() != null) {
                        addrNode.add(node.getType(), node.getName());
                    }
                }
            }
            try {
                response = ctx.getModelControllerClient().execute(req);
            } catch (IOException e) {
                throw new CommandLineException("Failed to validate address.", e);
            }
            result = response.get(Util.RESULT);
            if(!result.isDefined()) {
                throw new CommandLineException("Failed to validate address: the response from the controller doesn't contain result.");
            }
            for(ModelNode type : result.asList()) {
                if(lastType.equals(type.asString())) {
                    return;
                }
            }
            throw new CommandLineException("Invalid target address: " + lastType + " doesn't exist.");
        }
    }
}
