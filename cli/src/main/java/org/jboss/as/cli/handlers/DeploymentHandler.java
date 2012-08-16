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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.util.StrictSizeTable;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.spi.MountHandle;


/**
 * Base class for deploy and undeploy handlers containing common code
 * for these handlers.
 *
 * @author Alexey Loubyansky
 */
public abstract class DeploymentHandler extends BatchModeCommandHandler {

    static final String CLI_ARCHIVE_SUFFIX = ".cli";

    public DeploymentHandler(CommandContext ctx, String command, boolean connectionRequired) {
        super(ctx, command, connectionRequired);
    }

    protected void listDeployments(CommandContext ctx, boolean l) throws CommandFormatException {
        if(!l) {
            printList(ctx, Util.getDeployments(ctx.getModelControllerClient()), l);
            return;
        }
        final ModelControllerClient client = ctx.getModelControllerClient();
        final List<String> names = Util.getDeployments(client);
        if(names.isEmpty()) {
            return;
        }

        final StrictSizeTable table = new StrictSizeTable(names.size());
        final List<Property> descriptions = getDeploymentDescriptions(ctx, names).asPropertyList();
        for(Property prop : descriptions) {
            final ModelNode step = prop.getValue();
            if(step.hasDefined(Util.RESULT)) {
                final ModelNode result = step.get(Util.RESULT);
                table.addCell(Util.NAME, result.get(Util.NAME).asString());
                table.addCell(Util.RUNTIME_NAME, result.get(Util.RUNTIME_NAME).asString());
                if(result.has(Util.ENABLED)) {
                    table.addCell(Util.ENABLED, result.get(Util.ENABLED).asString());
                }
                if(result.has(Util.STATUS)) {
                    table.addCell(Util.STATUS, result.get(Util.STATUS).asString());
                }
            }
            if(!table.isAtLastRow()) {
                table.nextRow();
            }
        }
        throw new CommandFormatException(table.toString());
    }

    protected ModelNode getDeploymentDescriptions(CommandContext ctx, List<String> names) throws CommandFormatException {
        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);
        for(String name : names) {
            final ModelNode deploymentResource = buildReadDeploymentResourceRequest(name);
            if(deploymentResource != null) {
                steps.add(deploymentResource);
            }// else it's illegal state
        }
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(composite);
        } catch (IOException e) {
            throw new CommandFormatException("Failed to execute operation request.", e);
        }
        if (!result.hasDefined(Util.RESULT)) {
            return null;
        }
        return result.get(Util.RESULT);
    }

    protected ModelNode buildReadDeploymentResourceRequest(String name) {
        ModelNode request = new ModelNode();
        ModelNode address = request.get(Util.ADDRESS);
        address.add(Util.DEPLOYMENT, name);
        request.get(Util.OPERATION).set(Util.READ_RESOURCE);
        request.get(Util.INCLUDE_RUNTIME).set(true);
        return request;
    }

    protected MountHandle extractArchive(File archive) throws IOException {
        return ((MountHandle)VFS.mountZipExpanded(archive, VFS.getChild("cli"),
                TempFileProvider.create("cli", Executors.newSingleThreadScheduledExecutor())));
    }

    protected String activateNewBatch(CommandContext ctx) {
        String currentBatch = null;
        BatchManager batchManager = ctx.getBatchManager();
        if (batchManager.isBatchActive()) {
            currentBatch = "batch" + System.currentTimeMillis();
            batchManager.holdbackActiveBatch(currentBatch);
        }
        batchManager.activateNewBatch();
        return currentBatch;
    }

    protected void discardBatch(CommandContext ctx, String holdbackBatch) {
        BatchManager batchManager = ctx.getBatchManager();
        batchManager.discardActiveBatch();
        if (holdbackBatch != null) {
            batchManager.activateHeldbackBatch(holdbackBatch);
        }
    }

    protected boolean isCliArchive(File f) {
        if (f == null || f.isDirectory() || !f.getName().endsWith(CLI_ARCHIVE_SUFFIX)) {
            return false;
        } else {
            return true;
        }
    }
}