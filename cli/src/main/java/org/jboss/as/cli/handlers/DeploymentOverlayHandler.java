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
import org.jboss.as.cli.impl.ArgumentWithoutValue;
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

    private final ArgumentWithoutValue l;
    private final ArgumentWithValue action;
    private final ArgumentWithValue name;
    private final ArgumentWithValue content;
    private final ArgumentWithValue serverGroup;
    private final ArgumentWithValue deployment;

    public DeploymentOverlayHandler(CommandContext ctx) {
        super("deployment-overlay", true);

        l = new ArgumentWithoutValue(this, "-l") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null || "list-content".equals(actionStr) || "list-deployments".equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };

        action = new ArgumentWithValue(this, new SimpleTabCompleter(
                new String[]{"add", "link", "list-content", "list-deployments", "remove", "upload"}), 0, "--action");

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
            }}), "--name");
        name.addRequiredPreceding(action);

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
        content = new ArgumentWithValue(this, new CommandLineCompleter(){
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if ("add".equals(actionStr) || "upload".equals(actionStr)) {
                    // TODO add support for quoted paths
                    int i = buffer.lastIndexOf(',');
                    i = buffer.indexOf('=', i + 1);
                    if (i < 0) {
                        return -1;
                    }
                    final String path = buffer.substring(i + 1);
                    int pathResult = pathCompleter.complete(ctx, path, 0, candidates);
                    if (pathResult < 0) {
                        return -1;
                    }
                    return i + 1 + pathResult;
                } else if("remove".equals(actionStr)) {
                    final String nameStr = name.getValue(ctx.getParsedCommandLine());
                    if(nameStr == null) {
                        return -1;
                    }
                    final List<String> existing;
                    try {
                        existing = loadContentFor(ctx.getModelControllerClient(), nameStr);
                    } catch (CommandLineException e) {
                        return -1;
                    }
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
                } else {
                    return -1;
                }
            }}, "--content") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null) {
                    return false;
                }
                if("add".equals(actionStr) || "upload".equals(actionStr) || "remove".equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        content.addRequiredPreceding(name);
        content.addCantAppearAfter(l);

        serverGroup = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                return Util.getServerGroups(ctx.getModelControllerClient());
            }}) , "--server-group") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null) {
                    return false;
                }
                if("add".equals(actionStr) || "link".equals(actionStr)
                        || "remove".equals(actionStr) || "list-deployments".equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        serverGroup.addRequiredPreceding(name);

        deployment = new ArgumentWithValue(this, new CommandLineCompleter(){
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                final ModelControllerClient client = ctx.getModelControllerClient();
                if(client == null) {
                    return -1;
                }
                // TODO in domain mode it should consult the specified server group for deployments
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                final List<String> existing;
                if("add".equals(actionStr) || "link".equals(actionStr)) {
                    existing = Util.getDeployments(client);
                } else if("remove".equals(actionStr)) {
                    try {
                        final String nameStr = name.getValue(ctx.getParsedCommandLine());
                        if(nameStr == null) {
                            return -1;
                        }
                        final String sg = serverGroup.getValue(ctx.getParsedCommandLine());
                        if(ctx.isDomainMode() && sg == null) {
                            return -1;
                        }
                        existing = loadLinkedDeployments(client, nameStr, sg);
                    } catch (CommandLineException e) {
                        return -1;
                    }
                } else {
                    return -1;
                }

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
            }
        }, "--deployment") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode()) {
                    if(serverGroup.isPresent(ctx.getParsedCommandLine())) {
                        return super.canAppearNext(ctx);
                    }
                    return false;
                }
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null) {
                    return false;
                }
                if("add".equals(actionStr) || "link".equals(actionStr) || "remove".equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        deployment.addRequiredPreceding(name);
        deployment.addCantAppearAfter(l);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        if(!args.hasProperties() || l.isPresent(args) && args.getPropertyNames().isEmpty() && args.getOtherProperties().size() == 1) {
            final ModelNode op = new ModelNode();
            op.get(Util.ADDRESS).setEmptyList();
            op.get(Util.OPERATION).set(Util.READ_CHILDREN_NAMES);
            op.get(Util.CHILD_TYPE).set(Util.DEPLOYMENT_OVERLAY);
            final ModelNode response;
            try {
                response = ctx.getModelControllerClient().execute(op);
            } catch (IOException e) {
                throw new CommandLineException("Failed to execute " + Util.READ_CHILDREN_NAMES, e);
            }
            final ModelNode result = response.get(Util.RESULT);
            if(!result.isDefined()) {
                final String descr = Util.getFailureDescription(response);
                if(descr != null) {
                    throw new CommandLineException(descr);
                }
                throw new CommandLineException("The response of " + Util.READ_CHILDREN_NAMES + " is missing result: " + response);
            }

            if(l.isPresent(args)) {
                for(ModelNode node : result.asList()) {
                    ctx.printLine(node.asString());
                }
            } else {
                final List<String> names = new ArrayList<String>();
                for(ModelNode node : result.asList()) {
                    names.add(node.asString());
                }
                ctx.printColumns(names);
            }
            return;
        }

        final String action = this.action.getValue(args, true);
        if("add".equals(action)) {
            add(ctx);
        } else if("remove".equals(action)) {
            remove(ctx);
        } else if("upload".equals(action)) {
            upload(ctx);
        } else if("list-content".equals(action)) {
            listContent(ctx);
        } else if("list-deployments".equals(action)) {
            listDeployments(ctx);
        } else if("link".equals(action)) {
            link(ctx);
        } else {
            throw new CommandFormatException("Unrecognized action: '" + action + "'");
        }
    }

    protected void listDeployments(CommandContext ctx) throws CommandLineException {

        final ModelControllerClient client = ctx.getModelControllerClient();
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String name = this.name.getValue(args, true);
        if(name == null) {
            throw new CommandFormatException(this.name + " is missing value.");
        }
        final String sg = serverGroup.getValue(ctx.getParsedCommandLine());
        if(ctx.isDomainMode() && sg == null) {
            throw new CommandFormatException(serverGroup.getFullName() + " is missing value.");
        }
        final List<String> content = loadLinkedDeployments(client, name, sg);
        if(l.isPresent(args)) {
            for(String contentPath : content) {
                ctx.printLine(contentPath);
            }
        } else {
            ctx.printColumns(content);
        }
    }

    protected void listContent(CommandContext ctx) throws CommandLineException {

        final ModelControllerClient client = ctx.getModelControllerClient();
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String name = this.name.getValue(args, true);
        if(name == null) {
            throw new CommandFormatException(this.name.getFullName() + " is missing value.");
        }
        final List<String> content = loadContentFor(client, name);
        if(l.isPresent(args)) {
            for(String contentPath : content) {
                ctx.printLine(contentPath);
            }
        } else {
            ctx.printColumns(content);
        }
    }

    protected void remove(CommandContext ctx) throws CommandLineException {

        final ModelControllerClient client = ctx.getModelControllerClient();

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String name = this.name.getValue(args, true);
        if(name == null) {
            throw new CommandFormatException(this.name + " is missing value.");
        }
        final String contentStr = content.getValue(args);
        final String deploymentStr = deployment.getValue(args);
        final String sg = serverGroup.getValue(args);

        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        if(deploymentStr != null || contentStr == null) {
            // remove the overlay links

            if(ctx.isDomainMode()) {
                if(deploymentStr == null) {
                    final List<String> sgNames = sg == null ? Util.getServerGroups(client) : Collections.singletonList(sg);
                    // remove all
                    for(String sgName : sgNames) {
                        final List<String> deployments = loadLinkedDeployments(client, name, sgName);
                        if(!deployments.isEmpty()) {
                            addRemoveDeploymentSteps(name, sgName, deployments, steps);
                            final ModelNode op = new ModelNode();
                            final ModelNode addr = op.get(Util.ADDRESS);
                            addr.add(Util.SERVER_GROUP, sgName);
                            addr.add(Util.DEPLOYMENT_OVERLAY, name);
                            op.get(Util.OPERATION).set(Util.REMOVE);
                            steps.add(op);
                        }
                    }
                } else {
                    // in the domain mode, there must be a server group
                    if(ctx.isDomainMode() && sg == null) {
                        throw new CommandFormatException(serverGroup.getFullName() + " is missing.");
                    }
                    final List<String> deployments = Arrays.asList(deploymentStr.split(",+"));
                    addRemoveDeploymentSteps(name, sg, deployments, steps);
                }
            } else {
                final List<String> overlays;
                if(deploymentStr == null) {
                    // remove all
                    overlays = loadLinkedDeployments(client, name, null);
                } else {
                    // in the domain mode, there must server group
                    if(ctx.isDomainMode() && sg == null) {
                        throw new CommandFormatException(serverGroup.getFullName() + " is missing.");
                    }
                    overlays = Arrays.asList(deploymentStr.split(",+"));
                }
                addRemoveDeploymentSteps(name, sg, overlays, steps);
            }
        }

        if(contentStr != null || deploymentStr == null && sg == null) {
            // determine the content to be removed

            final List<String> contentList;
            if(contentStr == null) {
                contentList = loadContentFor(client, name);
            } else {
                contentList = java.util.Arrays.asList(contentStr.split(",+"));
            }

            for(String content : contentList) {
                final ModelNode op = new ModelNode();
                ModelNode addr = op.get(Util.ADDRESS);
                addr.add(Util.DEPLOYMENT_OVERLAY, name);
                addr.add(Util.CONTENT, content);
                op.get(Util.OPERATION).set(Util.REMOVE);
                steps.add(op);
            }
        }

        if(contentStr == null && deploymentStr == null && sg == null) {
            final ModelNode op = new ModelNode();
            op.get(Util.ADDRESS).add(Util.DEPLOYMENT_OVERLAY, name);
            op.get(Util.OPERATION).set(Util.REMOVE);
            steps.add(op);
        }

        try {
            final ModelNode result = client.execute(composite);
            if (!Util.isSuccess(result)) {
                throw new CommandFormatException(Util.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new CommandFormatException("Failed to remove overlay", e);
        }
    }

    protected List<String> loadContentFor(final ModelControllerClient client, final String overlay) throws CommandLineException {
        final List<String> contentList;
        final ModelNode op = new ModelNode();
        op.get(Util.ADDRESS).add(Util.DEPLOYMENT_OVERLAY, overlay);
        op.get(Util.OPERATION).set(Util.READ_CHILDREN_NAMES);
        op.get(Util.CHILD_TYPE).set(Util.CONTENT);
        final ModelNode response;
        try {
            response = client.execute(op);
        } catch (IOException e) {
            throw new CommandLineException("Failed to load the list of the existing content for overlay " + overlay, e);
        }

        final ModelNode result = response.get(Util.RESULT);
        if(!result.isDefined()) {
            throw new CommandLineException("Failed to load the list of the existing content for overlay " + overlay + ": " + response);
        }
        contentList = new ArrayList<String>();
        for(ModelNode node : result.asList()) {
            contentList.add(node.asString());
        }
        return contentList;
    }

    protected List<String> loadLinkedDeployments(final ModelControllerClient client, String overlay, String serverGroup) throws CommandLineException {
        final ModelNode op = new ModelNode();
        final ModelNode addr = op.get(Util.ADDRESS);
        if(serverGroup != null) {
            addr.add(Util.SERVER_GROUP, serverGroup);
        }
        addr.add(Util.DEPLOYMENT_OVERLAY, overlay);
        op.get(Util.OPERATION).set(Util.READ_CHILDREN_NAMES);
        op.get(Util.CHILD_TYPE).set(Util.DEPLOYMENT);
        final ModelNode response;
        try {
            response = client.execute(op);
        } catch (IOException e) {
            throw new CommandLineException("Failed to load the list of deployments for overlay " + overlay, e);
        }

        final ModelNode result = response.get(Util.RESULT);
        if(!result.isDefined()) {
            final String descr = Util.getFailureDescription(response);
            if(descr != null && descr.contains("JBAS014807")) {
                // resource doesn't exist
                return Collections.emptyList();
            }
            throw new CommandLineException("Failed to load the list of deployments for overlay " + overlay + ": " + response);
        }
        final List<String> contentList = new ArrayList<String>();
        for(ModelNode node : result.asList()) {
            contentList.add(node.asString());
        }
        return contentList;
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

        final String sg = serverGroup.getValue(args);
        if(ctx.isDomainMode() && sg == null) {
            throw new CommandFormatException(serverGroup.getFullName() + " is missing");
        }
        final String deploymentsStr = deployment.getValue(args);
        final String[] deployments;
        if(deploymentsStr == null) {
            deployments = null;
        } else {
            deployments = deploymentsStr.split(",+");
        }

        final ModelControllerClient client = ctx.getModelControllerClient();

        // upload the content
        final List<ModelNode> uploadResponses;
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
            uploadResponses = response.get(Util.RESULT).asList();
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
                ModelNode result = uploadResponses.get(i);
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

            if(deployments != null && deployments.length > 0) {
                if(sg != null) {
                    // here we don't need a separate check whether the overlay is linked
                    // from the server group since it is created in the same op.
                    op = new ModelNode();
                    address = op.get(Util.ADDRESS);
                    address.add(Util.SERVER_GROUP, sg);
                    address.add(Util.DEPLOYMENT_OVERLAY, name);
                    op.get(Util.OPERATION).set(Util.ADD);
                    steps.add(op);
                }
                // link the deployments
                addLinkDeploymentSteps(name, sg, deployments, steps);
            }

            try {
                final ModelNode result = client.execute(composite);
                if (!Util.isSuccess(result)) {
                    throw new CommandFormatException(Util.getFailureDescription(result));
                }
            } catch (IOException e) {
                throw new CommandFormatException("Failed to add overlay", e);
            }
        }
    }

    protected void link(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String name = this.name.getValue(args, true);
        final String deploymentsStr = deployment.getValue(args, true);
        final String sg = serverGroup.getValue(args);
        if(ctx.isDomainMode() && sg == null) {
            throw new CommandFormatException(serverGroup.getFullName() + " is missing value.");
        }
        final String[] deployments = deploymentsStr.split(",+");

        if(deployments.length == 0) {
            throw new CommandFormatException("Missing value for " + deployment.getFullName() + ": '" + deploymentsStr + "'");
        }

        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        if(sg != null && !Util.isValidPath(ctx.getModelControllerClient(), Util.SERVER_GROUP, sg, Util.DEPLOYMENT_OVERLAY, name)) {
            final ModelNode op = new ModelNode();
            final ModelNode address = op.get(Util.ADDRESS);
            address.add(Util.SERVER_GROUP, sg);
            address.add(Util.DEPLOYMENT_OVERLAY, name);
            op.get(Util.OPERATION).set(Util.ADD);
            steps.add(op);
        }

        // link the deployments
        addLinkDeploymentSteps(name, sg, deployments, steps);

        try {
            final ModelNode result = ctx.getModelControllerClient().execute(composite);
            if (!Util.isSuccess(result)) {
                throw new CommandFormatException(Util.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new CommandFormatException("Failed to link overlay", e);
        }
    }

    protected void upload(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String name = this.name.getValue(args, true);
        if(!Util.isValidPath(ctx.getModelControllerClient(), Util.DEPLOYMENT_OVERLAY, name)) {
            throw new CommandLineException("Deployment overlay " + name + " does not exist.");
        }
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

        final String deploymentsStr = deployment.getValue(args);
        if(deploymentsStr != null) {
            throw new CommandFormatException(deployment.getFullName() + " can't be used in combination with upload.");
        }

        final ModelControllerClient client = ctx.getModelControllerClient();

        // upload the content
        final List<ModelNode> uploadResponses;
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
                final String descr = Util.getFailureDescription(response);
                if(descr == null) {
                    throw new CommandLineException("Upload response is missing result.");
                } else {
                    throw new CommandLineException(descr);
                }
            }
            uploadResponses = response.get(Util.RESULT).asList();
        }

        // create the overlay and link it to the deployments
        {
            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);

            // add the content
            for (int i = 0; i < contentNames.length; ++i) {
                final String contentName = contentNames[i];
                ModelNode result = uploadResponses.get(i);
                result = result.get("step-" + (i+1));
                if(!result.isDefined()) {
                    throw new CommandLineException("Upload step response is missing expected step-" + (i+1) + " attribute: " + result);
                }
                result = result.get(Util.RESULT);
                if(!result.isDefined()) {
                    throw new CommandLineException("Upload step response is missing result: " + result);
                }
                final ModelNode op = new ModelNode();
                final ModelNode address = op.get(Util.ADDRESS);
                address.add(Util.DEPLOYMENT_OVERLAY, name);
                address.add(Util.CONTENT, contentName);
                op.get(Util.OPERATION).set(Util.ADD);
                op.get(Util.CONTENT).set(result);
                steps.add(op);
            }

            try {
                final ModelNode result = client.execute(composite);
                if (!Util.isSuccess(result)) {
                    throw new CommandFormatException(Util.getFailureDescription(result));
                }
            } catch (IOException e) {
                throw new CommandFormatException("Failed to add overlay", e);
            }
        }
    }

    protected void addLinkDeploymentSteps(final String overlayName, final String serverGroup, final String[] deployments,
            final ModelNode steps) {
        for(String deployment : deployments) {
            final ModelNode op = new ModelNode();
            final ModelNode address = op.get(Util.ADDRESS);
            if(serverGroup != null) {
                address.add(Util.SERVER_GROUP, serverGroup);
            }
            address.add(Util.DEPLOYMENT_OVERLAY, overlayName);
            address.add(Util.DEPLOYMENT, deployment);
            op.get(Util.OPERATION).set(Util.ADD);
            steps.add(op);
        }
    }

    protected void addRemoveDeploymentSteps(final String overlayName, String serverGroup,
            final List<String> deployments, final ModelNode steps) {
        for(String deploymentName : deployments) {
            final ModelNode op = new ModelNode();
            final ModelNode addr = op.get(Util.ADDRESS);
            if(serverGroup != null) {
                addr.add(Util.SERVER_GROUP, serverGroup);
            }
            addr.add(Util.DEPLOYMENT_OVERLAY, overlayName);
            addr.add(Util.DEPLOYMENT, deploymentName);
            op.get(Util.OPERATION).set(Util.REMOVE);
            steps.add(op);
        }
    }
}
