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

package org.jboss.as.patching.cli;

import java.io.File;
import java.io.FileInputStream;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.BaseOperationCommand;
import org.jboss.as.cli.handlers.DefaultFilenameTabCompleter;
import org.jboss.as.cli.handlers.FilenameTabCompleter;
import org.jboss.as.cli.handlers.WindowsFilenameTabCompleter;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.FileSystemPathArgument;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class PatchHandler extends BaseOperationCommand {

    static final String PATCH = "patch";
    private static final String PATCHING = "patching";

    private final ArgumentWithoutValue path;
    private final ArgumentWithValue host;

    public PatchHandler(final CommandContext context) {
        super(context, PATCH, true);

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(context) : new DefaultFilenameTabCompleter(context);
        path = new FileSystemPathArgument(this, pathCompleter, 0, "--path");
        host = new ArgumentWithValue(this, new DefaultCompleter(CandidatesProviders.HOSTS), "--host") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                return ctx.isDomainMode() && super.canAppearNext(ctx);
            }
        };
    }

    @Override
    protected ModelNode buildRequestWithoutHeaders(final CommandContext ctx) throws CommandFormatException {

        final ModelNode address = new ModelNode();
        if (ctx.isDomainMode()) {
            address.get(Util.HOST).set(host.getValue(ctx.getParsedCommandLine(), true));
        }
        address.get(Util.CORE_SERVICE).set(PATCHING);

        final ModelNode request = new ModelNode();
        request.get(Util.ADDRESS).set(address);
        request.get(Util.OPERATION).set(PATCH);
        return request;
    }

    protected byte[] readBytes(File f) throws OperationFormatException {
        byte[] bytes;
        FileInputStream is = null;
        try {
            is = new FileInputStream(f);
            bytes = new byte[(int) f.length()];
            int read = is.read(bytes);
            if(read != bytes.length) {
                throw new OperationFormatException("Failed to read bytes from " + f.getAbsolutePath() + ": " + read + " from " + f.length());
            }
        } catch (Exception e) {
            throw new OperationFormatException("Failed to read file " + f.getAbsolutePath(), e);
        } finally {
            StreamUtils.safeClose(is);
        }
        return bytes;
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {
        ParsedCommandLine args = ctx.getParsedCommandLine();

        final String path = this.path.getValue(args, true);

        final File f = new File(path);
        if(!f.exists()) {
            // i18n?
            throw new CommandFormatException("Path " + f.getAbsolutePath() + " doesn't exist.");
        }
        if(f.isDirectory()) {
            throw new CommandFormatException(f.getAbsolutePath() + " is a directory.");
        }

        ModelNode request = buildRequest(ctx);

        execute(ctx, request, f, false);
    }

    protected void execute(CommandContext ctx, ModelNode request, File f, boolean unmanaged) throws CommandFormatException {

        addHeaders(ctx, request);

        ModelNode result;
        try {
            if(!unmanaged) {
                OperationBuilder op = new OperationBuilder(request);
                op.addFileAsAttachment(f);
                request.get(Util.CONTENT).get(0).get(Util.INPUT_STREAM_INDEX).set(0);
                Operation operation = op.build();
                result = ctx.getModelControllerClient().execute(operation);
                operation.close();
            } else {
                result = ctx.getModelControllerClient().execute(request);
            }
        } catch (Exception e) {
            throw new CommandFormatException("Failed to add the deployment content to the repository: " + e.getLocalizedMessage());
        }
        if (!Util.isSuccess(result)) {
            throw new CommandFormatException(Util.getFailureDescription(result));
        }
        ctx.printLine(result.toJSONString(false));
    }
}
