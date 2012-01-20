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
import org.jboss.as.cli.impl.ArgumentWithValue;


/**
 *
 * @author Alexey Loubyansky
 */
public class BatchEditLineHandler extends CommandHandlerWithHelp {

    private ArgumentWithValue ln;

    public BatchEditLineHandler() {
        super("batch-edit-line");

            ln = new ArgumentWithValue(this, 0, "--line-number");
            ln.addCantAppearAfter(helpArg);

            ArgumentWithValue line = new ArgumentWithValue(this, new CommandLineCompleter() {
                @Override
                public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                    final String lnStr = ln.getValue(ctx.getParsedCommandLine());
                    if(lnStr == null) {
                        return -1;
                    }

                    final String originalLine = ctx.getParsedCommandLine().getOriginalLine();
                    boolean skipWS;
                    int wordCount;
                    if(Character.isWhitespace(originalLine.charAt(0))) {
                        skipWS = true;
                        wordCount = 0;
                    } else {
                        skipWS = false;
                        wordCount = 1;
                    }
                    int cmdStart = 1;
                    while(cmdStart < originalLine.length()) {
                        if(skipWS) {
                            if(!Character.isWhitespace(originalLine.charAt(cmdStart))) {
                                skipWS = false;
                                ++wordCount;
                                if(wordCount == 3) {
                                    break;
                                }
                            }
                        } else if(Character.isWhitespace(originalLine.charAt(cmdStart))) {
                            skipWS = true;
                        }
                        ++cmdStart;
                    }

                    final String cmd;
                    if(wordCount == 2) {
                        cmd = "";
                    } else if(wordCount != 3) {
                        return -1;
                    } else {
                        cmd = originalLine.substring(cmdStart);
                    }

                    int cmdResult = ctx.getDefaultCommandCompleter().complete(ctx, cmd, 0, candidates);
                    if(cmdResult < 0) {
                        return cmdResult;
                    }

                    // escaping index correction
                    int escapeCorrection = 0;
                    int start = originalLine.length() - 1 - buffer.length();
                    while(start - escapeCorrection >= 0) {
                        final char ch = originalLine.charAt(start - escapeCorrection);
                        if(Character.isWhitespace(ch) || ch == '=') {
                            break;
                        }
                        ++escapeCorrection;
                    }

                    return buffer.length() + escapeCorrection - (cmd.length() - cmdResult);
                }}, Integer.MAX_VALUE, "--line") {
            };
            line.addRequiredPreceding(ln);
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
        final int batchSize = batch.size();
        if(batchSize == 0) {
            ctx.error("The batch is empty.");
            return;
        }

        String argsStr = ctx.getArgumentsString();
        if(argsStr == null) {
            ctx.error("Missing line number.");
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
            ctx.error("Missing the new command line after the index.");
            return;
        }

        String intStr = argsStr.substring(0, i);
        int lineNumber;
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

        String editedLine = argsStr.substring(i).trim();
        if(editedLine.length() == 0) {
            ctx.error("Missing the new command line after the index.");
            return;
        }

        if(editedLine.charAt(0) == '"') {
            if(editedLine.length() > 1 && editedLine.charAt(editedLine.length() - 1) == '"') {
                editedLine = editedLine.substring(1, editedLine.length() - 1);
            }
        }

        try {
            BatchedCommand newCmd = ctx.toBatchedCommand(editedLine);
            batch.set(lineNumber - 1, newCmd);
            ctx.printLine("#" + lineNumber + " " + newCmd.getCommand());
        } catch (CommandFormatException e) {
            ctx.error("Failed to process command line '" + editedLine + "': " + e.getLocalizedMessage());
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
