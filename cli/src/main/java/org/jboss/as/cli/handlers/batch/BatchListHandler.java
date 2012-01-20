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
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;

/**
 *
 * @author Alexey Loubyansky
 */
public class BatchListHandler extends CommandHandlerWithHelp {

    public BatchListHandler() {
        super("batch-list");
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
        Batch activeBatch = batchManager.getActiveBatch();
        List<BatchedCommand> commands = activeBatch.getCommands();
        if (!commands.isEmpty()) {
            for (int i = 0; i < commands.size(); ++i) {
                BatchedCommand cmd = commands.get(i);
                ctx.printLine("#" + (i + 1) + ' ' + cmd.getCommand());
            }
        } else {
            ctx.printLine("The batch is empty.");
        }
    }

}
