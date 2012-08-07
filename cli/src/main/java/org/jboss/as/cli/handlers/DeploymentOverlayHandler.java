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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeploymentOverlayHandler extends CommandHandlerWithHelp {

    private final ArgumentWithValue action;
    private final ArgumentWithValue name;
    private final ArgumentWithValue content;
    private final ArgumentWithValue deployment;

    public DeploymentOverlayHandler(CommandContext ctx) {
        super("deployment-overlay", true);

        action = new ArgumentWithValue(this, new SimpleTabCompleter(new String[]{"add", "remove"}), 0, "--action");
        name = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                final ModelControllerClient client = ctx.getModelControllerClient();
                if(client == null) {
                    return Collections.emptyList();
                }
                final ModelNode op = new ModelNode();
                op.get(Util.OPERATION).set(Util.READ_CHILDREN_NAMES);
                op.get(Util.ADDRESS).setEmptyList();
                op.get(Util.CHILD_TYPE).set(Util.DEPLOYMENT_OVERLAY);
                final ModelNode response;
                try {
                    response = client.execute(op);
                } catch (IOException e) {
                    return Collections.emptyList();
                }
                final ModelNode result = response.get(Util.RESULT);
                if(!result.isDefined()) {
                    return Collections.emptyList();
                }
                final List<String> names = new ArrayList<String>();
                for(ModelNode node : result.asList()) {
                    names.add(node.asString());
                }
                return names;
            }}), 1, "--name");
        name.addRequiredPreceding(action);

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
        content = new ArgumentWithValue(this, new CommandLineCompleter(){
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                // TODO add support for quoted paths
                int i = buffer.lastIndexOf(',');
                i = buffer.indexOf('=', i + 1);
                if(i < 0) {
                    return -1;
                }
                final String path = buffer.substring(i + 1);
                int pathResult = pathCompleter.complete(ctx, path, 0, candidates);
                if(pathResult < 0) {
                    return -1;
                }
                return i + 1 + pathResult;
            }}, "--content");
        content.addRequiredPreceding(name);

        deployment = new ArgumentWithValue(this, new CommandLineCompleter(){
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                final ModelControllerClient client = ctx.getModelControllerClient();
                if(client == null) {
                    return -1;
                }
                final List<String> existing = Util.getDeployments(client);
                if(existing.isEmpty()) {
                    return buffer.length();
                }
                candidates.addAll(existing);
                if(buffer.isEmpty()) {
                    return 0;
                }
                final String[] specified = buffer.split(",+");
                candidates.removeAll(Arrays.asList(specified));
                if(buffer.charAt(buffer.length() - 1) == ',') {
                    return buffer.length();
                }
                final String chunk = specified[specified.length - 1];
                for(int i = 0; i < candidates.size(); ++i) {
                    if(!candidates.get(i).startsWith(chunk)) {
                        candidates.remove(i);
                    }
                }
                return buffer.length() - chunk.length();
            }}, "--deployment");
        deployment.addRequiredPreceding(name);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String action = this.action.getValue(args, true);
        if("add".equals(action)) {
            add(ctx);
        } else if("remove".equals(action)) {
            remove(ctx);
        } else {
            throw new CommandFormatException("Unrecognized action: '" + action + "'");
        }
    }

    protected void remove(CommandContext ctx) throws CommandLineException {

/*        ModelNode op = new ModelNode();
      ModelNode address = op.get(Util.ADDRESS);
      address.add(Util.DEPLOYMENT_OVERLAY, name);
      op.get(Util.OPERATION).set(Util.REMOVE);

        final ModelNode composite = new ModelNode();
        final OperationBuilder opBuilder = new OperationBuilder(composite);
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        ModelNode op = new ModelNode();
      ModelNode address = op.get(Util.ADDRESS);
      address.add(Util.DEPLOYMENT_OVERLAY, name);
      op.get(Util.OPERATION).set(Util.REMOVE);
      ctx.getModelControllerClient().execute(op);
*/
        throw new CommandLineException("remove is not yet implemented");
        }

    protected void add(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String name = this.name.getValue(args, true);
        final String contentStr = content.getValue(args, true);

        final String[] contentPairs = contentStr.split(",+");
        if(contentPairs.length == 0) {
            throw new CommandFormatException("Overlay content is not specified.");
        }
        final String[] contentNames = new String[contentPairs.length];
        final File[] contentPaths = new File[contentPairs.length];
        for(int i = 0; i < contentPairs.length; ++i) {
            final String pair = contentPairs[i];
            final int equalsIndex = pair.indexOf('=');
            if(equalsIndex < 0) {
                throw new CommandFormatException("Content pair is not following archive-path=fs-path format: '" + pair + "'");
            }
            contentNames[i] = pair.substring(0, equalsIndex);
            if(contentNames[i].length() == 0) {
                throw new CommandFormatException("The archive path is missing for the content '" + pair + "'");
            }
            final String path = pair.substring(equalsIndex + 1);
            if(path.length() == 0) {
                throw new CommandFormatException("The filesystem paths is missing for the content '" + pair + "'");
            }
            final File f = new File(path);
            if(!f.exists()) {
                throw new CommandFormatException("Content file doesn't exist " + f.getAbsolutePath());
            }
            contentPaths[i] = f;
        }

        final String[] deployments = deployment.getValue(args, true).split(",+");
        if(deployments.length == 0) {
            throw new CommandFormatException("Deployments are missing.");
        }

        final ModelControllerClient client = ctx.getModelControllerClient();

        // upload the content
        final List<ModelNode> stepResponses;
        {
            final ModelNode composite = new ModelNode();
            final OperationBuilder opBuilder = new OperationBuilder(composite);
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);
            for (int i = 0; i < contentPaths.length; ++i) {
                final ModelNode op = new ModelNode();
                op.get(Util.ADDRESS).setEmptyList();
                op.get(Util.OPERATION).set(Util.UPLOAD_DEPLOYMENT_STREAM);
                op.get(Util.INPUT_STREAM_INDEX).set(i);
                opBuilder.addFileAsAttachment(contentPaths[i]);
                steps.add(op);
            }
            final Operation compositeOp = opBuilder.build();
            final ModelNode response;
            try {
                response = client.execute(compositeOp);
            } catch (IOException e) {
                throw new CommandFormatException("Failed to upload content", e);
            } finally {
                try {
                    compositeOp.close();
                } catch (IOException e) {
                }
            }
            if(!response.hasDefined(Util.RESULT)) {
                throw new CommandFormatException("Upload response is missing result.");
            }
            stepResponses = response.get(Util.RESULT).asList();
        }

        // create the overlay and link it to the deployments
        {
            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);

            // add the overlay
            ModelNode op = new ModelNode();
            ModelNode address = op.get(Util.ADDRESS);
            address.add(Util.DEPLOYMENT_OVERLAY, name);
            op.get(Util.OPERATION).set(Util.ADD);
            steps.add(op);

            // add the content
            for (int i = 0; i < contentNames.length; ++i) {
                final String contentName = contentNames[i];
                ModelNode result = stepResponses.get(i);
                result = result.get("step-" + (i+1));
                if(!result.isDefined()) {
                    throw new CommandFormatException("Upload step response is missing expected step-" + (i+1) + " attribute: " + result);
                }
                result = result.get(Util.RESULT);
                if(!result.isDefined()) {
                    throw new CommandFormatException("Upload step response is missing result: " + result);
                }
                op = new ModelNode();
                address = op.get(Util.ADDRESS);
                address.add(Util.DEPLOYMENT_OVERLAY, name);
                address.add(Util.CONTENT, contentName);
                op.get(Util.OPERATION).set(Util.ADD);
                op.get(Util.CONTENT).set(result);
                steps.add(op);
            }

            // link the deployments
            for(String deployment : deployments) {
                op = new ModelNode();
                address = op.get(Util.ADDRESS);
                address.add(Util.DEPLOYMENT_OVERLAY_LINK, name + "-" + deployment);
                op.get(Util.OPERATION).set(Util.ADD);
                op.get(Util.DEPLOYMENT).set(deployment);
                op.get(Util.DEPLOYMENT_OVERLAY).set(name);
                steps.add(op);
            }

            final ModelNode response;
            try {
                response = client.execute(composite);
            } catch (IOException e) {
                // TODO rollback content upload?
                throw new CommandLineException("Failed to setup overlays", e);
            }
            ctx.printLine(response.toString());
        }
    }
}
