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
import java.util.List;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.RequestParameterArgument;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.parsing.ParserUtil;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class BaseOperationCommand extends CommandHandlerWithHelp implements OperationCommand {

    protected List<RequestParameterArgument> params = new ArrayList<RequestParameterArgument>();
    protected OperationRequestAddress requiredAddress;

    public BaseOperationCommand(String command) {
        super(command, true);
    }

    public BaseOperationCommand(String command, boolean connectionRequired) {
        super(command, connectionRequired);
    }

    /**
     * Adds a node path which is required to exist before the command can be used.
     * @param requiredPath  node path which is required to exist before the command can be used.
     */
    protected void addRequiredPath(String requiredPath) {
        if(requiredPath == null) {
            throw new IllegalArgumentException("Required path can't be null.");
        }
        // there perhaps could be more but for now only one is allowed
        if(requiredAddress != null) {
            throw new IllegalStateException("Only one required address is allowed, atm.");
        }
        requiredAddress = new DefaultOperationRequestAddress();
        CommandLineParser.CallbackHandler handler = new DefaultCallbackHandler(requiredAddress);
        try {
            ParserUtil.parseOperationRequest(requiredPath, handler);
        } catch (CommandFormatException e) {
            throw new IllegalArgumentException("Failed to parse nodeType: " + e.getMessage());
        }
    }

    /**
     * Adds a node path which is required to exist before the command can be used.
     * @param requiredPath  node path which is required to exist before the command can be used.
     */
    protected void addRequiredPath(OperationRequestAddress requiredPath) {
        if(requiredPath == null) {
            throw new IllegalArgumentException("Required path can't be null.");
        }
        // there perhaps could be more but for now only one is allowed
        if(requiredAddress != null) {
            throw new IllegalStateException("Only one required address is allowed, atm.");
        }
        requiredAddress = requiredPath;
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        if(!super.isAvailable(ctx)) {
            return false;
        }
        if(requiredAddress == null) {
            return true;
        }
        ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            return false;
        }
        if(ctx.isDomainMode()) {
            return true;
        }
        ModelNode request = new ModelNode();
        ModelNode address = request.get(Util.ADDRESS);
        for(OperationRequestAddress.Node node : requiredAddress) {
            address.add(node.getType(), node.getName());
        }
        request.get(Util.OPERATION).set(Util.VALIDATE_ADDRESS);
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            return false;
        }
        return Util.isSuccess(result);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        ModelNode request;
        try {
            request = buildRequest(ctx);
        } catch (CommandFormatException e1) {
            ctx.printLine(e1.getLocalizedMessage());
            return;
        }

        if(request == null) {
            ctx.printLine("Operation request wasn't built.");
            return;
        }

        ModelControllerClient client = ctx.getModelControllerClient();
        final ModelNode result;
        try {
            result = client.execute(request);
        } catch (Exception e) {
            ctx.printLine("Failed to perform operation: " + e.getLocalizedMessage());
            return;
        }
        handleResponse(ctx, result, Util.COMPOSITE.equals(request.get(Util.OPERATION).asString()));
    }

    protected void handleResponse(CommandContext ctx, ModelNode result, boolean composite) {
        if (!Util.isSuccess(result)) {
            ctx.printLine(Util.getFailureDescription(result));
            return;
        }
    }

    @Override
    public void addArgument(CommandArgument arg) {
        super.addArgument(arg);
        if(arg instanceof RequestParameterArgument) {
            params.add((RequestParameterArgument) arg);
        }
    }

    protected void setParams(CommandContext ctx, ModelNode request) throws CommandFormatException {
        for(RequestParameterArgument arg : params) {
            arg.set(ctx.getParsedCommandLine(), request);
        }
    }
}
