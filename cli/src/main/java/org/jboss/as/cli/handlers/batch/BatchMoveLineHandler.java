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
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;

/**
 *
 * @author Alexey Loubyansky
 */
public class BatchMoveLineHandler extends CommandHandlerWithHelp {

    public BatchMoveLineHandler() {
        super("batch-move-line");
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
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        BatchManager batchManager = ctx.getBatchManager();
        if(!batchManager.isBatchActive()) {
            ctx.error("No active batch.");
            return;
        }

        Batch batch = batchManager.getActiveBatch();
        final int batchSize = batch.size();
        if(batchSize == 0) {
            ctx.error("The batch is empty.");
            return;
        }

        List<String> arguments = ctx.getParsedCommandLine().getOtherProperties();
        if(arguments.isEmpty()) {
            ctx.error("Missing line number.");
            return;
        }

        if(arguments.size() != 2) {
            ctx.error("Expected two arguments but received: " + arguments);
            return;
        }

        String intStr = arguments.get(0);
        final int lineNumber;
        try {
            lineNumber = Integer.parseInt(intStr);
        } catch(NumberFormatException e) {
            ctx.error("Failed to parse line number '" + intStr + "': " + e.getLocalizedMessage());
            return;
        }

        if(lineNumber < 1 || lineNumber > batchSize) {
            ctx.error(lineNumber + " isn't in range [1.." + batchSize + "].");
            return;
        }

        intStr = arguments.get(1);
        final int toLineNumber;
        try {
            toLineNumber = Integer.parseInt(intStr);
        } catch(NumberFormatException e) {
            ctx.error("Failed to parse line number '" + intStr + "': " + e.getLocalizedMessage());
            return;
        }

        if(toLineNumber < 1 || toLineNumber > batchSize) {
            ctx.error(toLineNumber + " isn't in range [1.." + batchSize + "].");
            return;
        }

        batch.move(lineNumber - 1, toLineNumber - 1);
    }

    @Override
    public boolean hasArgument(int index) {
        return index < 2;
    }
}
