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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;

/**
 *
 * @author Alexey Loubyansky
 */
public class BatchHandler extends CommandHandlerWithHelp {

    private final ArgumentWithoutValue l;
    private final ArgumentWithValue name;

    public BatchHandler() {
        super("batch");

        l = new ArgumentWithoutValue(this, "-l");
        l.setExclusive(true);

        name = new ArgumentWithValue(this, new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

                BatchManager batchManager = ctx.getBatchManager();
                Set<String> names = batchManager.getHeldbackNames();
                if(names.isEmpty()) {
                    return -1;
                }

                int nextCharIndex = 0;
                while (nextCharIndex < buffer.length()) {
                    if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                        break;
                    }
                    ++nextCharIndex;
                }

                String chunk = buffer.substring(nextCharIndex).trim();
                for(String name : names) {
                    if(name != null && name.startsWith(chunk)) {
                        candidates.add(name);
                    }
                }
                Collections.sort(candidates);
                return nextCharIndex;

            }}, 0, "--name");
        name.addCantAppearAfter(l);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        BatchManager batchManager = ctx.getBatchManager();

        if(l.isPresent(ctx.getParsedCommandLine())) {
            Set<String> heldbackNames = batchManager.getHeldbackNames();
            if(!heldbackNames.isEmpty()) {
                List<String> names = new ArrayList<String>(heldbackNames.size());
                for (String name : heldbackNames) {
                    names.add(name == null ? "<unnamed>" : name);
                }
                Collections.sort(names);
                for (String name : names) {
                    ctx.printLine(name);
                }
            }
            return;
        }

        if(batchManager.isBatchActive()) {
            ctx.error("Can't start a new batch while in batch mode.");
            return;
        }

        final String name = this.name.getValue(ctx.getParsedCommandLine());

        boolean activated;
        if(batchManager.isHeldback(name)) {
            activated = batchManager.activateHeldbackBatch(name);
            if (activated) {
                final String msg = name == null ? "Re-activated batch" : "Re-activated batch '" + name + "'";
                ctx.printLine(msg);
                List<BatchedCommand> batch = batchManager.getActiveBatch().getCommands();
                if (!batch.isEmpty()) {
                    for (int i = 0; i < batch.size(); ++i) {
                        BatchedCommand cmd = batch.get(i);
                        ctx.printLine("#" + (i + 1) + ' ' + cmd.getCommand());
                    }
                }
            }
        } else if(name != null) {
            ctx.error("'" + name + "' not found among the held back batches.");
            return;
        } else {
            activated = batchManager.activateNewBatch();
        }

        if(!activated) {
            // that's more like illegal state
            ctx.error("Failed to activate batch.");
        }
    }
}
