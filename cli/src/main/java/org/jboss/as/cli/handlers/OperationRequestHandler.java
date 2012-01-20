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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * The operation request handler.
 *
 * @author Alexey Loubyansky
 */
public class OperationRequestHandler implements CommandHandler, OperationCommand {

    @Override
    public boolean isBatchMode() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandHandler#handle(org.jboss.as.cli.CommandContext)
     */
    @Override
    public void handle(CommandContext ctx) {

        ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            ctx.error("You are disconnected at the moment." +
                    " Type 'connect' to connect to the server" +
                    " or 'help' for the list of supported commands.");
            return;
        }

        ModelNode request = (ModelNode) ctx.get("OP_REQ");
        if(request == null) {
            ctx.error("Parsed request isn't available.");
            return;
        }

        try {
            validateRequest(ctx, request);
        } catch(CommandFormatException e) {
            ctx.error(e.getLocalizedMessage());
            return;
        }

        try {
            ModelNode result = client.execute(request);
            ctx.printLine(result.toString());
        } catch(NoSuchElementException e) {
            ctx.error("ModelNode request is incomplete: " + e.getMessage());
        } catch (CancellationException e) {
            ctx.error("The result couldn't be retrieved (perhaps the task was cancelled: " + e.getLocalizedMessage());
        } catch (IOException e) {
            ctx.error("Communication error: " + e.getLocalizedMessage());
            ctx.disconnectController();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        return true;
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {
        return ((DefaultCallbackHandler)ctx.getParsedCommandLine()).toOperationRequest(ctx);
    }

    @Override
    public boolean hasArgument(String name) {
        return false;
    }

    @Override
    public boolean hasArgument(int index) {
        return false;
    }

    @Override
    public List<CommandArgument> getArguments(CommandContext ctx) {
        return Collections.emptyList();
    }

    private void validateRequest(CommandContext ctx, ModelNode request) throws CommandFormatException {

        final ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            throw new CommandFormatException("No connection to the controller.");
        }

        final Set<String> keys = request.keys();

        if(!keys.contains(Util.OPERATION)) {
            throw new CommandFormatException("Request is missing the operation name.");
        }
        final String operationName = request.get(Util.OPERATION).asString();

        if(!keys.contains(Util.ADDRESS)) {
            throw new CommandFormatException("Request is missing the address part.");
        }
        final ModelNode address = request.get(Util.ADDRESS);

        if(keys.size() == 2) { // no props
            return;
        }

        final ModelNode opDescrReq = new ModelNode();
        opDescrReq.get(Util.ADDRESS).set(address);
        opDescrReq.get(Util.OPERATION).set(Util.READ_OPERATION_DESCRIPTION);
        opDescrReq.get(Util.NAME).set(operationName);

        final ModelNode outcome;
        try {
            outcome = client.execute(opDescrReq);
        } catch(Exception e) {
            throw new CommandFormatException("Failed to perform " + Util.READ_OPERATION_DESCRIPTION + " to validate the request: " + e.getLocalizedMessage());
        }
        if (!Util.isSuccess(outcome)) {
            throw new CommandFormatException("Failed to get the list of the operation properties: \"" + Util.getFailureDescription(outcome) + '\"');
        }

        if(!outcome.has(Util.RESULT)) {
            throw new CommandFormatException("Failed to perform " + Util.READ_OPERATION_DESCRIPTION + " to validate the request: result is not available.");
        }
        final ModelNode result = outcome.get(Util.RESULT);
        if(!result.hasDefined(Util.REQUEST_PROPERTIES)) {
            throw new CommandFormatException("Operation '" + operationName + "' does not expect any property.");
        }
        final Set<String> definedProps = result.get("request-properties").keys();
        if(definedProps.isEmpty()) {
            throw new CommandFormatException("Operation '" + operationName + "' does not expect any property.");
        }

        int skipped = 0;
        for(String prop : keys) {
            if(skipped < 2 && (prop.equals(Util.ADDRESS) || prop.equals(Util.OPERATION))) {
                ++skipped;
                continue;
            }
            if(!definedProps.contains(prop)) {
                if(!Util.OPERATION_HEADERS.equals(prop)) {
                    throw new CommandFormatException("'" + prop + "' is not found among the supported properties: " + definedProps);
                }
            }
        }
    }
}
