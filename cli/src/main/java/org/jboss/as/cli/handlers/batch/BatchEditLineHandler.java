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

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;

/**
 *
 * @author Alexey Loubyansky
 */
public class BatchEditLineHandler extends CommandHandlerWithHelp {

    private final CommandLineCompleter argCompleter;

    public BatchEditLineHandler() {
        super("batch-edit-line");

        argCompleter = new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

                final BatchManager batchManager = ctx.getBatchManager();
                if(!batchManager.isBatchActive()) {
                    return -1;
                }

                int nextCharIndex = 0;
                while (nextCharIndex < buffer.length()) {
                    if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                        break;
                    }
                    ++nextCharIndex;
                }

                if(nextCharIndex == buffer.length()) {
                    candidates.add("--help");
                    return nextCharIndex;
                }

                int nextWsIndex = nextCharIndex + 1;
                while(nextWsIndex < buffer.length()) {
                    if(Character.isWhitespace(buffer.charAt(nextWsIndex))) {
                        break;
                    }
                    ++nextWsIndex;
                }

                if(nextWsIndex == buffer.length()) {
                    return -1;
                }

                String lineNumberStr = buffer.substring(nextCharIndex, nextWsIndex);
                if("--help".startsWith(lineNumberStr)) {
                    candidates.add("--help");
                    return nextCharIndex;
                }

                final int lineNumber;
                try {
                    lineNumber = Integer.parseInt(lineNumberStr);
                } catch(NumberFormatException e) {
                    return -1;
                }

                final Batch batch = batchManager.getActiveBatch();
                int batchSize = batch.size();

                if(lineNumber < 1 || lineNumber > batchSize) {
                    return -1;
                }

                nextCharIndex = nextWsIndex + 1;
                while (nextCharIndex < buffer.length()) {
                    if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                        break;
                    }
                    ++nextCharIndex;
                }

                String cmd = buffer.substring(nextCharIndex);
                if("--help".startsWith(cmd)) {
                    candidates.add("--help");
                }

                int cmdResult = ctx.getDefaultCommandCompleter().complete(ctx, cmd, 0, candidates);

                final String batchedCmd = batch.getCommands().get(lineNumber - 1).getCommand();
                if(cmd.isEmpty() || batchedCmd.startsWith(cmd)) {
                    if(cmdResult > -1) {
                        candidates.add(batchedCmd.substring(cmdResult).trim());
                    } else {
                        candidates.add(batchedCmd);
                    }
                }

                if(cmdResult < 0) {
                    return candidates.isEmpty() ? -1 : nextCharIndex;
                }
                return nextCharIndex + cmdResult;
            }};
    }

    @Override
    public CommandLineCompleter getArgumentCompleter() {
        return argCompleter;
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
            ctx.printLine("No active batch.");
            return;
        }

        Batch batch = batchManager.getActiveBatch();
        final int batchSize = batch.size();
        if(batchSize == 0) {
            ctx.printLine("The batch is empty.");
            return;
        }

        String argsStr = ctx.getArgumentsString();
        if(argsStr == null) {
            ctx.printLine("Missing line number.");
            return;
        }

        int i = 0;
        while(i < argsStr.length()) {
            if(Character.isWhitespace(argsStr.charAt(i))) {
                break;
            }
            ++i;
        }

        if(i == argsStr.length()) {
            ctx.printLine("Missing the new command line after the index.");
            return;
        }

        String intStr = argsStr.substring(0, i);
        int lineNumber;
        try {
            lineNumber = Integer.parseInt(intStr);
        } catch(NumberFormatException e) {
            ctx.printLine("Failed to parse line number '" + intStr + "': " + e.getLocalizedMessage());
            return;
        }

        if(lineNumber < 1 || lineNumber > batchSize) {
            ctx.printLine(lineNumber + " isn't in range [1.." + batchSize + "].");
            return;
        }

        String editedLine = argsStr.substring(i).trim();
        if(editedLine.length() == 0) {
            ctx.printLine("Missing the new command line after the index.");
            return;
        }

        try {
            BatchedCommand newCmd = ctx.toBatchedCommand(editedLine);
            batch.set(lineNumber - 1, newCmd);
            ctx.printLine("#" + lineNumber + " " + newCmd.getCommand());
        } catch (CommandFormatException e) {
            ctx.printLine("Failed to process command line '" + editedLine + "': " + e.getLocalizedMessage());
        }
    }

    /**
     * It has to accept everything since we don't know what kind of command will be edited.
     */
    @Override
    public boolean hasArgument(int index) {
        return true;
    }

    @Override
    public boolean hasArgument(String name) {
        return true;
    }
}
