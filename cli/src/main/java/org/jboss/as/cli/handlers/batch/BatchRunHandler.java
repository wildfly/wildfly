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
package org.jboss.as.cli.handlers.batch;

import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class BatchRunHandler extends CommandHandlerWithHelp {

    public BatchRunHandler() {
        super("batch-run", true);
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        if(!super.isAvailable(ctx)) {
            return false;
        }
        return ctx.isBatchMode();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) {

        BatchManager batchManager = ctx.getBatchManager();
        if(!batchManager.isBatchActive()) {
            ctx.error("No active batch.");
            return;
        }

        Batch batch = batchManager.getActiveBatch();
        List<BatchedCommand> currentBatch = batch.getCommands();
        if(currentBatch.isEmpty()) {
            ctx.error("The batch is empty.");
            batchManager.discardActiveBatch();
            return;
        }

        ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        ModelNode steps = composite.get(Util.STEPS);

        for(BatchedCommand cmd : currentBatch) {
            steps.add(cmd.getRequest());
        }

        try {
            ModelNode result = ctx.getModelControllerClient().execute(composite);
            if(Util.isSuccess(result)) {
                batchManager.discardActiveBatch();
                ctx.printLine("The batch executed successfully.");
            } else {
                ctx.error("Failed to execute batch: " + Util.getFailureDescription(result));
            }
        } catch (Exception e) {
            ctx.error("Failed to execute batch: " + e.getLocalizedMessage());
        }
    }
}
