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
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.handlers.BaseOperationCommand;
import org.jboss.as.cli.handlers.DefaultFilenameTabCompleter;
import org.jboss.as.cli.handlers.FilenameTabCompleter;
import org.jboss.as.cli.handlers.WindowsFilenameTabCompleter;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class BatchRunHandler extends BaseOperationCommand {

    private final ArgumentWithValue file;

    public BatchRunHandler(CommandContext ctx) {
        super(ctx, "batch-run", true);

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
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        boolean failed = false;
        try {
            super.doHandle(ctx);
            ctx.printLine("The batch executed successfully");
        } catch(CommandLineException e) {
            failed = true;
            throw e;
        } finally{
            if(!failed) {
                if(ctx.getBatchManager().isBatchActive()) {
                    ctx.getBatchManager().discardActiveBatch();
                }
            }
        }
    }

    @Override
    protected ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {

        final String path = file.getValue(ctx.getParsedCommandLine());

        final BatchManager batchManager = ctx.getBatchManager();
        if(batchManager.isBatchActive()) {
            if(path != null) {
                throw new CommandFormatException("--file is not allowed in the batch mode.");
            }
            final Batch batch = batchManager.getActiveBatch();
            List<BatchedCommand> currentBatch = batch.getCommands();
            if(currentBatch.isEmpty()) {
                batchManager.discardActiveBatch();
                throw new CommandFormatException("The batch is empty.");
            }
            return batch.toRequest();
        }

        if(path != null) {
            final File f = new File(path);
            if(!f.exists()) {
                throw new CommandFormatException("File " + f.getAbsolutePath() + " does not exist.");
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
                return batch.toRequest();
            } catch(IOException e) {
                throw new CommandFormatException("Failed to read file " + f.getAbsolutePath(), e);
            } catch(CommandFormatException e) {
                throw new CommandFormatException("Failed to create batch from " + f.getAbsolutePath(), e);
            } finally {
                batchManager.discardActiveBatch();
                if(baseDir != null) {
                    ctx.setCurrentDir(currentDir);
                }
                if(reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {}
                }
            }
        }

        throw new CommandFormatException("Without arguments the command can be executed only in the batch mode.");
    }
}
