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
import java.util.Iterator;
import java.util.List;

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CliEvent;
import org.jboss.as.cli.CliEventListener;
import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.RequestParameterArgument;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.HeadersCompleter;
import org.jboss.as.cli.parsing.ParserUtil;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class BaseOperationCommand extends CommandHandlerWithHelp implements OperationCommand, CliEventListener {

    protected List<RequestParameterArgument> params = new ArrayList<RequestParameterArgument>();
    protected OperationRequestAddress requiredAddress;

    private boolean dependsOnProfile;
    private Boolean addressAvailable;
    private String requiredType;

    protected ArgumentWithValue headers = new ArgumentWithValue(this, HeadersCompleter.INSTANCE, ArgumentValueConverter.HEADERS, "--headers");

    public BaseOperationCommand(CommandContext ctx, String command, boolean connectionRequired) {
        super(command, connectionRequired);
        ctx.addEventListener(this);
    }

    /**
     * Adds a node path which is required to exist before the command can be used.
     * @param requiredPath  node path which is required to exist before the command can be used.
     */
    protected void addRequiredPath(String requiredPath) {
        if(requiredPath == null) {
            throw new IllegalArgumentException("Required path can't be null.");
        }
        DefaultOperationRequestAddress requiredAddress = new DefaultOperationRequestAddress();
        CommandLineParser.CallbackHandler handler = new DefaultCallbackHandler(requiredAddress);
        try {
            ParserUtil.parseOperationRequest(requiredPath, handler);
        } catch (CommandFormatException e) {
            throw new IllegalArgumentException("Failed to parse nodeType: " + e.getMessage());
        }
        addRequiredPath(requiredAddress);
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

        final Iterator<Node> iterator = requiredAddress.iterator();
        if(iterator.hasNext()) {
            final String firstType = iterator.next().getType();
            dependsOnProfile = "subsystem".equals(firstType) || Util.PROFILE.equals(firstType);
        }
        if(requiredAddress.endsOnType()) {
            requiredType = requiredAddress.toParentNode().getType();
        }
    }

    protected boolean isDependsOnProfile() {
        return dependsOnProfile;
    }

    protected OperationRequestAddress getRequiredAddress() {
        return requiredAddress;
    }

    protected String getRequiredType() {
        return requiredType;
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        if(!super.isAvailable(ctx)) {
            return false;
        }
        if(requiredAddress == null) {
            return true;
        }

        if(dependsOnProfile && ctx.isDomainMode()) { // not checking address in all the profiles
            return true;
        }

        if(addressAvailable != null) {
            return addressAvailable.booleanValue();
        }

        ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            return false;
        }
        ModelNode request = new ModelNode();
        ModelNode address = request.get(Util.ADDRESS);
        for(OperationRequestAddress.Node node : requiredAddress) {
            address.add(node.getType(), node.getName());
        }

        if(requiredType == null) {
            request.get(Util.OPERATION).set(Util.VALIDATE_ADDRESS);
            ModelNode result;
            try {
                result = ctx.getModelControllerClient().execute(request);
            } catch (IOException e) {
                return false;
            }
            addressAvailable = Util.isSuccess(result);
        } else {
            request.get(Util.OPERATION).set(Util.READ_CHILDREN_TYPES);
            ModelNode result;
            try {
                result = ctx.getModelControllerClient().execute(request);
            } catch (IOException e) {
                return false;
            }
            addressAvailable = Util.listContains(result, requiredType);
        }
        return addressAvailable;
    }

    @Override
    public void cliEvent(CliEvent event, CommandContext ctx) {
        if(event == CliEvent.DISCONNECTED) {
            addressAvailable = null;
        }
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
            ctx.error(e1.getLocalizedMessage());
            return;
        }

        if(request == null) {
            ctx.error("Operation request wasn't built.");
            return;
        }
        addHeaders(ctx, request);

        ModelControllerClient client = ctx.getModelControllerClient();
        final ModelNode response;
        try {
            response = client.execute(request);
        } catch (Exception e) {
            ctx.error("Failed to perform operation: " + e.getLocalizedMessage());
            return;
        }
        handleResponse(ctx, response, Util.COMPOSITE.equals(request.get(Util.OPERATION).asString()));
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {
        final ModelNode request = buildRequestWithoutHeaders(ctx);
        addHeaders(ctx, request);
        return request;
    }

    protected abstract ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException;

    protected void addHeaders(CommandContext ctx, ModelNode request) throws CommandFormatException {
        if(!headers.isPresent(ctx.getParsedCommandLine())) {
            return;
        }
        final String headersValue = headers.getValue(ctx.getParsedCommandLine());
        final ModelNode headersNode = headers.getValueConverter().fromString(headersValue);
        final ModelNode opHeaders = request.get(Util.OPERATION_HEADERS);
        opHeaders.set(headersNode);
    }

    protected void handleResponse(CommandContext ctx, ModelNode response, boolean composite) {
        if (!Util.isSuccess(response)) {
            ctx.error(Util.getFailureDescription(response));
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
