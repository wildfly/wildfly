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
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
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
            ctx.printLine("You are disconnected at the moment." +
                    " Type 'connect' to connect to the server" +
                    " or 'help' for the list of supported commands.");
            return;
        }

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder(ctx.getPrefix());
        try {
            ctx.getOperationRequestParser().parse(ctx.getArgumentsString(), builder);
            ModelNode request = builder.buildRequest();
            ModelNode result = client.execute(request);
            ctx.printLine(result.toString());
        } catch(CommandFormatException e) {
            ctx.printLine(e.getLocalizedMessage());
        } catch(NoSuchElementException e) {
            ctx.printLine("ModelNode request is incomplete: " + e.getMessage());
        } catch (CancellationException e) {
            ctx.printLine("The result couldn't be retrieved (perhaps the task was cancelled: " + e.getLocalizedMessage());
        } catch (IOException e) {
            ctx.printLine("Communication error: " + e.getLocalizedMessage());
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
    public CommandLineCompleter getArgumentCompleter() {
        return OperationRequestCompleter.INSTANCE;
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws OperationFormatException {
        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder(ctx.getPrefix());
        ctx.getOperationRequestParser().parse(ctx.getArgumentsString(), builder);
        return builder.buildRequest();
    }

    @Override
    public boolean hasArgument(String name) {
        return false;
    }

    @Override
    public boolean hasArgument(int index) {
        return false;
    }
}
