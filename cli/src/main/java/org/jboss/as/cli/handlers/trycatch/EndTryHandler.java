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
package org.jboss.as.cli.handlers.trycatch;

import java.io.IOException;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class EndTryHandler extends CommandHandlerWithHelp {

    public EndTryHandler() {
        super("end-try", true);
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        try {
            final TryBlock tryBlock = TryBlock.get(ctx);
            return tryBlock != null && !tryBlock.isInTry();
        } catch (CommandLineException e) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final TryBlock tryBlock = TryBlock.remove(ctx);

        final BatchManager batchManager = ctx.getBatchManager();
        if(!batchManager.isBatchActive()) {
            if(tryBlock.isInCatch()) {
                throw new CommandLineException("catch block did not activate batch mode.");
            } else {
                throw new CommandLineException("finally block did not activate batch mode.");
            }
        }

        final Batch batch = batchManager.getActiveBatch();
        batchManager.discardActiveBatch();

        final ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            throw new CommandLineException("The connection to the controller has not been established.");
        }

        final ModelNode tryRequest = tryBlock.getTryRequest();
        if(tryRequest == null) {
            throw new CommandLineException("The try request is not available.");
        }

        ModelNode response;
        try {
            response = client.execute(tryRequest);
        } catch (IOException e) {
            throw new CommandLineException("try request failed", e);
        }

        CommandLineException catchError = null;
        if(!Util.isSuccess(response)) {
            ctx.printLine("try block failed: " + Util.getFailureDescription(response));
            ModelNode catchRequest = tryBlock.getCatchRequest();
            if(catchRequest == null && tryBlock.isInCatch() && batch.size() > 0) {
                catchRequest = batch.toRequest();
            }
            if(catchRequest != null) {
                try {
                    response = client.execute(catchRequest);
                } catch (IOException e) {
                    throw new CommandLineException("catch request failed", e);
                }
                if(!Util.isSuccess(response)) {
                    catchError = new CommandLineException("catch request failed: " + Util.getFailureDescription(response));
                }
            }
        }

        if(tryBlock.isInFinally() && batch.size() > 0) {
            final ModelNode finallyRequest = batch.toRequest();
            try {
                response = client.execute(finallyRequest);
            } catch (IOException e) {
                throw new CommandLineException("finally request failed", e);
            }
            if(!Util.isSuccess(response)) {
                throw new CommandLineException("finally request failed: " + Util.getFailureDescription(response));
            }
        }

        if(catchError != null) {
            throw catchError;
        }
    }
}
