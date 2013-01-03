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
package org.jboss.as.cli.handlers.ifelse;

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
public class EndIfHandler extends CommandHandlerWithHelp {

    public EndIfHandler() {
        super("end-if", true);
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        try {
            final IfElseBlock ifBlock = IfElseBlock.get(ctx);
            return ifBlock != null;
        } catch (CommandLineException e) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final IfElseBlock ifBlock = IfElseBlock.remove(ctx);

        final BatchManager batchManager = ctx.getBatchManager();
        if(!batchManager.isBatchActive()) {
            if(ifBlock.isInIf()) {
                throw new CommandLineException("if block did not activate batch mode.");
            } else {
                throw new CommandLineException("else block did not activate batch mode.");
            }
        }

        final Batch batch = batchManager.getActiveBatch();
        batchManager.discardActiveBatch();

        final ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            throw new CommandLineException("The connection to the controller has not been established.");
        }

        final ModelNode conditionRequest = ifBlock.getConditionRequest();
        if(conditionRequest == null) {
            throw new CommandLineException("The condition request is not available.");
        }

        final Operation expression = ifBlock.getConditionExpression();
        if(expression == null) {
            throw new CommandLineException("The if expression is not available.");
        }

        ModelNode targetValue;
        try {
            targetValue = client.execute(conditionRequest);
        } catch (IOException e) {
            throw new CommandLineException("condition request failed", e);
        }

        final Object value = expression.resolveValue(ctx, targetValue);
        if(value == null) {
            throw new CommandLineException("if expression resolved to a null");
        }

        if(Boolean.TRUE.equals(value)) {
            ModelNode ifRequest = ifBlock.getIfRequest();
            if(ifRequest == null) {
                if(batch.size() == 0) {
                    throw new CommandLineException("if request is missing.");
                }
                ifRequest = batch.toRequest();
            }
            try {
                final ModelNode response = client.execute(ifRequest);
                if(!Util.isSuccess(response)) {
                    new CommandLineException("if request failed: " + Util.getFailureDescription(response));
                }
            } catch (IOException e) {
                throw new CommandLineException("if request failed", e);
            }
        } else if(ifBlock.isInElse()) {
            if(batch.size() == 0) {
                throw new CommandLineException("else block is empty.");
            }
            try {
                final ModelNode response = client.execute(batch.toRequest());
                if(!Util.isSuccess(response)) {
                    throw new CommandLineException("else request failed: " + Util.getFailureDescription(response));
                }
            } catch (IOException e) {
                throw new CommandLineException("else request failed", e);
            }
        }
    }
}
