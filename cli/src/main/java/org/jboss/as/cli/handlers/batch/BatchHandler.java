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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.handlers.DefaultFilenameTabCompleter;
import org.jboss.as.cli.handlers.FilenameTabCompleter;
import org.jboss.as.cli.handlers.WindowsFilenameTabCompleter;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 *
 * @author Alexey Loubyansky
 */
public class BatchHandler extends CommandHandlerWithHelp {

    private final ArgumentWithoutValue l;
    private final ArgumentWithValue name;
    private final ArgumentWithValue file;

    public BatchHandler(CommandContext ctx) {
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
        name.setExclusive(true);

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
        file = new ArgumentWithValue(this, pathCompleter, "--file") {
            @Override
            public String getValue(ParsedCommandLine args) {
                String value = super.getValue(args);
                if(value != null) {
                    if(value.length() >= 0 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                        value = value.substring(1, value.length() - 1);
                    }
                    value = pathCompleter.translatePath(value);
                }
                return value;
            }
        };
        file.setExclusive(true);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

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
            throw new CommandLineException("Can't start a new batch while in batch mode.");
        }

        final String path = file.getValue(ctx.getParsedCommandLine());
        if(path != null) {
            final File f = new File(path);
            if(!f.exists()) {
                throw new CommandLineException("File " + f.getAbsolutePath() + " does not exist.");
            }

            final File currentDir = ctx.getCurrentDir();
            final File baseDir = f.getParentFile();
            if(baseDir != null) {
                ctx.setCurrentDir(baseDir);
            }

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(f));
                String line = reader.readLine();
                batchManager.activateNewBatch();
                final Batch batch = batchManager.getActiveBatch();
                while(line != null) {
                    batch.add(ctx.toBatchedCommand(line));
                    line = reader.readLine();
                }
            } catch(IOException e) {
                batchManager.discardActiveBatch();
                throw new CommandLineException("Failed to read file " + f.getAbsolutePath(), e);
            } catch(CommandFormatException e) {
                batchManager.discardActiveBatch();
                throw new CommandLineException("Failed to create batch from " + f.getAbsolutePath(), e);
            } finally {
                if(baseDir != null) {
                    ctx.setCurrentDir(currentDir);
                }
                if(reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {}
                }
            }
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
            throw new CommandLineException("'" + name + "' not found among the held back batches.");
        } else {
            activated = batchManager.activateNewBatch();
        }

        if(!activated) {
            // that's more like illegal state
            throw new CommandLineException("Failed to activate batch.");
        }
    }
}
