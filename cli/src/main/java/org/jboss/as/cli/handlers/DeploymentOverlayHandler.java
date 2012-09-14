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
import org.jboss.as.cli.impl.CommaSeparatedCompleter;
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

    private static final String ADD = "add";
    private static final String LINK = "link";
    private static final String LIST_CONTENT = "list-content";
    private static final String LIST_LINKS = "list-links";
    private static final String REDEPLOY_AFFECTED = "redeploy-affected";
    private static final String REMOVE = "remove";
    private static final String UPLOAD = "upload";

    private final ArgumentWithoutValue l;
    private final ArgumentWithValue action;
    private final ArgumentWithValue name;
    private final ArgumentWithValue content;
    private final ArgumentWithValue serverGroups;
    //private final ArgumentWithValue allServerGroups;
    private final ArgumentWithoutValue allRelevantServerGroups;
    private final ArgumentWithValue deployments;
    //private final ArgumentWithValue wildcardDeployments;
    //private final ArgumentWithValue redeployAffected;

    public DeploymentOverlayHandler(CommandContext ctx) {
        super("deployment-overlay", true);

        l = new ArgumentWithoutValue(this, "-l") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null || LIST_CONTENT.equals(actionStr) || LIST_LINKS.equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };

        action = new ArgumentWithValue(this, new SimpleTabCompleter(
                new String[]{ADD, LINK, LIST_CONTENT, LIST_LINKS, REDEPLOY_AFFECTED, REMOVE, UPLOAD}), 0, "--action");

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
                if (ADD.equals(actionStr) || UPLOAD.equals(actionStr)) {
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
                } else if(REMOVE.equals(actionStr)) {
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
                if(ADD.equals(actionStr) || UPLOAD.equals(actionStr) || REMOVE.equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        content.addRequiredPreceding(name);
        content.addCantAppearAfter(l);

        serverGroups = new ArgumentWithValue(this, new CommaSeparatedCompleter() {
            @Override
            protected Collection<String> getAllCandidates(CommandContext ctx) {
                return Util.getServerGroups(ctx.getModelControllerClient());
            }} , "--server-groups") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null) {
                    return false;
                }
                if(ADD.equals(actionStr) || LINK.equals(actionStr)
                        || REMOVE.equals(actionStr) || LIST_LINKS.equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        serverGroups.addRequiredPreceding(name);

        allRelevantServerGroups = new ArgumentWithoutValue(this, "--all-relevant-server-groups") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null) {
                    return false;
                }
                if(REMOVE.equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        allRelevantServerGroups.addRequiredPreceding(name);
        allRelevantServerGroups.addCantAppearAfter(serverGroups);
        serverGroups.addCantAppearAfter(allRelevantServerGroups);

        deployments = new ArgumentWithValue(this, new CommaSeparatedCompleter() {
            @Override
            protected Collection<String> getAllCandidates(CommandContext ctx) {
                return Util.getDeployments(ctx.getModelControllerClient());
            }}, "--deployments") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode()) {
                    if(serverGroups.isPresent(ctx.getParsedCommandLine())) {
                        return super.canAppearNext(ctx);
                    }
                    return false;
                }
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null) {
                    return false;
                }
                if(ADD.equals(actionStr) || LINK.equals(actionStr) || REMOVE.equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        deployments.addRequiredPreceding(name);
        deployments.addCantAppearAfter(l);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        if(!args.hasProperties() || l.isPresent(args) && args.getOtherProperties().isEmpty() && args.getPropertyNames().size() == 1) {
            // list registered overlays
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
        if(ADD.equals(action)) {
            add(ctx);
        } else if(REMOVE.equals(action)) {
            remove(ctx);
        } else if(UPLOAD.equals(action)) {
            upload(ctx);
        } else if(LIST_CONTENT.equals(action)) {
            listContent(ctx);
        } else if(LIST_LINKS.equals(action)) {
            listLinks(ctx);
        } else if(LINK.equals(action)) {
            link(ctx);
        } else {
            throw new CommandFormatException("Unrecognized action: '" + action + "'");
        }
    }

    protected void listLinks(CommandContext ctx) throws CommandLineException {

        final ModelControllerClient client = ctx.getModelControllerClient();
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String name = this.name.getValue(args, true);
        if(name == null) {
            throw new CommandFormatException(this.name + " is missing value.");
        }
        final String sg = serverGroups.getValue(ctx.getParsedCommandLine());
        if(ctx.isDomainMode()) {
            final List<String> groups;
            if(sg == null) {
                //throw new CommandFormatException(serverGroups.getFullName() + " is missing value.");
                groups = Util.getServerGroups(client);
            } else {
                groups = Arrays.asList(sg.split(",+"));
            }
            if(groups.size() == 0) {
                throw new CommandFormatException(serverGroups.getFullName() + " is missing value.");
            }
            for(String group : groups) {
                ctx.printLine("SERVER GROUP: " + group + Util.LINE_SEPARATOR);
                final List<String> links = loadLinkedDeployments(client, name, group);
                if(links.isEmpty()) {
                    ctx.printLine("n/a");
                } else {
                    ctx.printColumns(links);
                }
                ctx.printLine("");
            }
        } else {
            final List<String> content = loadLinkedDeployments(client, name, sg);
            if (l.isPresent(args)) {
                for (String contentPath : content) {
                    ctx.printLine(contentPath);
                }
            } else {
                ctx.printColumns(content);
            }
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
        final String deploymentStr = deployments.getValue(args);
        final String sgStr = serverGroups.getValue(args);
        final List<String> sg;
        if(sgStr == null) {
            if(allRelevantServerGroups.isPresent(args)) {
                sg = Util.getServerGroups(client);
            } else {
                sg = null;
            }
        } else {
            sg = Arrays.asList(sgStr.split(",+"));
            if(sg.isEmpty()) {
                throw new CommandFormatException(serverGroups.getFullName() + " is missing value.");
            }
        }

        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        if(deploymentStr != null || contentStr == null) {
            // remove the overlay links

            if(ctx.isDomainMode()) {
                if(deploymentStr == null) {
                    final List<String> sgNames = sg == null ? Util.getServerGroups(client) : sg;
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
                    if(ctx.isDomainMode() && sg == null) {
                        throw new CommandFormatException(serverGroups.getFullName() + " or " + allRelevantServerGroups.getFullName() + " is required.");
                    }
                    final List<String> deployments = Arrays.asList(deploymentStr.split(",+"));
                    for(String group : sg) {
                        addRemoveDeploymentSteps(name, group, deployments, steps);
                    }
                }
            } else {
                final List<String> overlays;
                if(deploymentStr == null) {
                    // remove all
                    overlays = loadLinkedDeployments(client, name, null);
                } else {
                    overlays = Arrays.asList(deploymentStr.split(",+"));
                }
                addRemoveDeploymentSteps(name, null, overlays, steps);
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

        final String sgStr = serverGroups.getValue(args);
        final String deploymentsStr = deployments.getValue(args);
        final String[] deployments;
        if(deploymentsStr == null) {
            if(sgStr != null) {
                throw new CommandFormatException(serverGroups.getFullName() + " is specified but " + this.deployments.getFullName() + " is not.");
            }
            deployments = null;
        } else {
            deployments = deploymentsStr.split(",+");
        }

        final String[] sg;
        if(deployments != null && ctx.isDomainMode()) {
            if(sgStr == null) {
                throw new CommandFormatException(serverGroups.getFullName() + " is missing.");
            }
            sg = sgStr.split(",+");
            if(sg.length == 0) {
                throw new CommandFormatException(serverGroups.getFullName() + " is missing value.");
            }
        } else {
            sg = null;
        }

        final ModelControllerClient client = ctx.getModelControllerClient();


        // create the overlay and link it to the deployments
        {
            final ModelNode composite = new ModelNode();
            final OperationBuilder opBuilder = new OperationBuilder(composite, true);
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
                op = new ModelNode();
                address = op.get(Util.ADDRESS);
                address.add(Util.DEPLOYMENT_OVERLAY, name);
                address.add(Util.CONTENT, contentName);
                op.get(Util.OPERATION).set(Util.ADD);
                op.get(Util.CONTENT).get(Util.INPUT_STREAM_INDEX).set(i);
                opBuilder.addFileAsAttachment(contentPaths[i]);
                steps.add(op);
            }

            if(deployments != null && deployments.length > 0) {
                if(sg != null) {
                    // here we don't need a separate check whether the overlay is linked
                    // from the server group since it is created in the same op.
                    for(String group : sg) {
                        op = new ModelNode();
                        address = op.get(Util.ADDRESS);
                        address.add(Util.SERVER_GROUP, group);
                        address.add(Util.DEPLOYMENT_OVERLAY, name);
                        op.get(Util.OPERATION).set(Util.ADD);
                        steps.add(op);
                        addLinkDeploymentSteps(name, group, deployments, steps);
                    }
                } else {
                    addLinkDeploymentSteps(name, null, deployments, steps);
                }
            }

            try {
                final ModelNode result = client.execute(opBuilder.build());
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
        final String deploymentsStr = deployments.getValue(args, true);
        final String sgStr = serverGroups.getValue(args);
        final String[] sg;
        if(ctx.isDomainMode()) {
            if(sgStr == null) {
                throw new CommandFormatException(serverGroups.getFullName() + " is missing value.");
            }
            sg = sgStr.split(",+");
            if(sg.length == 0) {
                throw new CommandFormatException(serverGroups.getFullName() + " is missing value.");
            }
        } else {
            sg = null;
        }
        final String[] deployments = deploymentsStr.split(",+");

        if(deployments.length == 0) {
            throw new CommandFormatException("Missing value for " + this.deployments.getFullName() + ": '" + deploymentsStr + "'");
        }

        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        if(sg != null) {
            for(String group : sg) {
                if(!Util.isValidPath(ctx.getModelControllerClient(), Util.SERVER_GROUP, group, Util.DEPLOYMENT_OVERLAY, name)) {
                    final ModelNode op = new ModelNode();
                    final ModelNode address = op.get(Util.ADDRESS);
                    address.add(Util.SERVER_GROUP, group);
                    address.add(Util.DEPLOYMENT_OVERLAY, name);
                    op.get(Util.OPERATION).set(Util.ADD);
                    steps.add(op);
                }
                addLinkDeploymentSteps(name, group, deployments, steps);
            }
        } else {
            addLinkDeploymentSteps(name, null, deployments, steps);
        }

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

        final String deploymentsStr = deployments.getValue(args);
        if(deploymentsStr != null) {
            throw new CommandFormatException(deployments.getFullName() + " can't be used in combination with upload.");
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
