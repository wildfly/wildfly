/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cli.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

/**
 * @author btison
 *
 */
public class ArchiveHandler extends BatchModeCommandHandler {

    private final ArgumentWithoutValue path;
    private final ArgumentWithValue script;
    private final Random rng = new Random();

    public ArchiveHandler(CommandContext ctx) {
        super(ctx, "archive", true);
        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
        path = new ArgumentWithValue(this, pathCompleter, 0, "--path") {
            @Override
            public String getValue(ParsedCommandLine args, boolean required) throws CommandFormatException {
                String value = super.getValue(args, required);
                if(value != null) {
                    if(value.length() >= 0 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                        value = value.substring(1, value.length() - 1);
                    }
                    value = pathCompleter.translatePath(value);
                }
                return value;
            }
        };

        script = new ArgumentWithValue(this, "--script");
        path.addCantAppearAfter(script);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.OperationCommand#buildRequest(org.jboss.as.cli.CommandContext)
     */
    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {
        ParsedCommandLine args = ctx.getParsedCommandLine();
        final String path = this.path.getValue(args, true);

        final File archive;
        archive = new File(path);
        if(!archive.exists()) {
            throw new OperationFormatException("Path " + archive.getAbsolutePath() + " doesn't exist.");
        }
        if(archive.isDirectory()) {
            throw new OperationFormatException(archive.getAbsolutePath() + " is a directory.");
        }
        File root;
        try {
            root = extractArchive(archive);
        } catch (IOException e) {
            throw new OperationFormatException("Unable to extract archive '" + archive.getAbsolutePath() + "' to temporary location");
        }

        ctx.setCurrentDir(root);
        String holdbackBatch = activateNewBatch(ctx);

        try {
            String script = this.script.getValue(args);
            if (script == null) {
                script = "deploy.scr";
            }

            File scriptFile = new File(ctx.getCurrentDir(),script);
            if (!scriptFile.exists()) {
                throw new CommandFormatException("ERROR: script " + script + "' not found.");
            }
            ctx.printLine("Processing script '" + script + "'.");

            try {
                BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
                String line = reader.readLine();
                while (!ctx.isTerminated() && line != null) {
                    ctx.handle(line);
                    line = reader.readLine();
                }
            } catch (FileNotFoundException e) {
                throw new CommandFormatException("ERROR: script " + script + "' not found.");
            } catch (IOException e) {
                throw new CommandFormatException(e.getMessage());
            } catch (CommandLineException e) {
                throw new CommandFormatException(e.getMessage());
            }

            ModelNode composite = ctx.getBatchManager().getActiveBatch().toRequest();

            return composite;
        } finally {
            // reset current dir in context
            ctx.setCurrentDir(new File(""));
            discardBatch(ctx, holdbackBatch);
            recursiveDelete(root);
        }
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {
        ModelNode request;
        try {
            request = buildRequest(ctx);
        } catch (CommandFormatException e1) {
            ctx.error(e1.getLocalizedMessage());
            return;
        }

        if(request == null) {
            ctx.error("Operation request wasn't built.");
            return;
        }

        try {
            ModelNode result = ctx.getModelControllerClient().execute(request);
            if(Util.isSuccess(result)) {
                ctx.printLine("The archive script executed successfully.");
            } else {
                ctx.error("Failed to execute archive script: " + Util.getFailureDescription(result));
            }
        } catch (Exception e) {
            ctx.error("Failed to execute archive script: " + e.getLocalizedMessage());
        }
    }

    private File extractArchive(File archive) throws IOException {
        File systemTmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tempDir = new File(systemTmpDir,"cli-" + Long.toHexString(rng.nextLong()));
        tempDir.mkdir();

        JarFile jarFile = new JarFile(archive);
        for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
            JarEntry entry = entries.nextElement();
            File file = new File(tempDir, entry.getName());
            if (entry.isDirectory()) {
                file.mkdir();
                continue;
            }
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                int bufferSize = 65536;
                byte[] buf = new byte[bufferSize];
                int rc;
                is = jarFile.getInputStream(entry);
                fos = new FileOutputStream(file);
                while ((rc = is.read(buf)) > -1) {
                    fos.write(buf,0,rc);
                }
                fos.flush();
            } finally {
                StreamUtils.safeClose(is);
                StreamUtils.safeClose(fos);
            }
        }
        return tempDir;
    }

    private String activateNewBatch(CommandContext ctx) {
        String currentBatch = null;
        BatchManager batchManager = ctx.getBatchManager();
        if (batchManager.isBatchActive()) {
            currentBatch = "batch" + System.currentTimeMillis();
            batchManager.holdbackActiveBatch(currentBatch);
        }
        batchManager.activateNewBatch();
        return currentBatch;
    }

    private void discardBatch(CommandContext ctx, String holdbackBatch) {
        BatchManager batchManager = ctx.getBatchManager();
        batchManager.discardActiveBatch();
        if (holdbackBatch != null) {
            batchManager.activateHeldbackBatch(holdbackBatch);
        }
    }

    private void recursiveDelete(File root) {
       if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (File file : files) {
                recursiveDelete(file);
            }
        } else {
            root.delete();
        }
        return;
    }

}
