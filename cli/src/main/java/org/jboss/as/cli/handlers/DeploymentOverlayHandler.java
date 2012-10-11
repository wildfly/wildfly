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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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

    private static final byte REDEPLOY_NONE = 0;
    private static final byte REDEPLOY_ONLY_AFFECTED = 1;
    private static final byte REDEPLOY_ALL = 2;

    private final ArgumentWithoutValue l;
    private final ArgumentWithValue action;
    private final ArgumentWithValue name;
    private final ArgumentWithValue content;
    private final ArgumentWithValue serverGroups;
    private final ArgumentWithoutValue allServerGroups;
    private final ArgumentWithoutValue allRelevantServerGroups;
    private final ArgumentWithValue deployments;
    private final ArgumentWithValue wildcards;
    private final ArgumentWithoutValue redeployAffected;

    private final FilenameTabCompleter pathCompleter;

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

        pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
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

        allServerGroups = new ArgumentWithoutValue(this, "--all-server-groups") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null) {
                    return false;
                }
                if(ADD.equals(actionStr) || LINK.equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        allServerGroups.addRequiredPreceding(name);
        allServerGroups.addCantAppearAfter(serverGroups);
        serverGroups.addCantAppearAfter(allServerGroups);

        deployments = new ArgumentWithValue(this, new CommaSeparatedCompleter() {
            @Override
            protected Collection<String> getAllCandidates(CommandContext ctx) {
                final String actionValue = action.getValue(ctx.getParsedCommandLine());
                final ModelControllerClient client = ctx.getModelControllerClient();
                if(REMOVE.equals(actionValue)) {
                    final String overlay = name.getValue(ctx.getParsedCommandLine());
                    if(overlay == null) {
                        return Collections.emptyList();
                    }
                    try {
                        if(ctx.isDomainMode()) {
                            final String groupsStr = serverGroups.getValue(ctx.getParsedCommandLine());
                            if(groupsStr != null) {
                                final String[] groups = groupsStr.split(",+");
                                if(groups.length == 1) {
                                    return loadLinks(client, overlay, groups[0]);
                                } else if(groups.length > 1) {
                                    final Set<String> commonLinks = new HashSet<String>();
                                    commonLinks.addAll(loadLinks(client, overlay, groups[0]));
                                    for(int i = 1; i < groups.length; ++i) {
                                        commonLinks.retainAll(loadLinks(client, overlay, groups[i]));
                                    }
                                    return commonLinks;
                                }
                            }
                        } else {
                            return loadLinks(client, overlay, null);
                        }
                    } catch(CommandLineException e) {
                    }
                    return Collections.emptyList();
                }
                return Util.getDeployments(client);
            }}, "--deployments") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode()) {
                    if(serverGroups.isPresent(ctx.getParsedCommandLine()) || allServerGroups.isPresent(ctx.getParsedCommandLine())) {
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

        wildcards = new ArgumentWithValue(this, new CommaSeparatedCompleter() {
            @Override
            protected Collection<String> getAllCandidates(CommandContext ctx) {
                return Util.getDeployments(ctx.getModelControllerClient());
            }}, "--wildcards") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode()) {
                    if(serverGroups.isPresent(ctx.getParsedCommandLine()) || allServerGroups.isPresent(ctx.getParsedCommandLine())) {
                        return super.canAppearNext(ctx);
                    }
                    return false;
                }
                final String actionStr = action.getValue(ctx.getParsedCommandLine());
                if(actionStr == null) {
                    return false;
                }
                if(ADD.equals(actionStr) || LINK.equals(actionStr)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        wildcards.addRequiredPreceding(name);
        wildcards.addCantAppearAfter(l);

        redeployAffected = new ArgumentWithoutValue(this, "--redeploy-affected") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(deployments.isPresent(ctx.getParsedCommandLine()) || wildcards.isPresent(ctx.getParsedCommandLine())) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
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
        } else if(REDEPLOY_AFFECTED.equals(action)) {
            redeployAffected(ctx);
        } else {
            throw new CommandFormatException("Unrecognized action: '" + action + "'");
        }
    }

    protected void redeployAffected(CommandContext ctx) throws CommandLineException {
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        assertNotPresent(serverGroups, args);
        assertNotPresent(allServerGroups, args);
        assertNotPresent(allRelevantServerGroups, args);
        assertNotPresent(content, args);
        assertNotPresent(deployments, args);
        assertNotPresent(wildcards, args);
        assertNotPresent(redeployAffected, args);

        final String overlay = name.getValue(args, true);
        final ModelControllerClient client = ctx.getModelControllerClient();

        final ModelNode redeployOp = new ModelNode();
        redeployOp.get(Util.OPERATION).set(Util.COMPOSITE);
        redeployOp.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = redeployOp.get(Util.STEPS);

        if(ctx.isDomainMode()) {
            for(String group : Util.getServerGroupsReferencingOverlay(overlay, client)) {
                addRemoveRedeployLinksSteps(client, steps, overlay, group, null, false, REDEPLOY_ALL);
            }
        } else {
            addRemoveRedeployLinksSteps(client, steps, overlay, null, null, false, REDEPLOY_ALL);
        }

        if(steps.asList().isEmpty()) {
            return;
        }

        try {
            ctx.printLine("redeploy request: " + redeployOp);
            final ModelNode result = client.execute(redeployOp);
            if (!Util.isSuccess(result)) {
                ctx.printLine(result.toString());
                throw new CommandLineException(Util.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new CommandFormatException("Failed to redeploy affected deployments", e);
        }

    }

    protected void listLinks(CommandContext ctx) throws CommandLineException {

        final ModelControllerClient client = ctx.getModelControllerClient();
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        assertNotPresent(allRelevantServerGroups, args);
        assertNotPresent(content, args);
        assertNotPresent(deployments, args);
        assertNotPresent(wildcards, args);
        assertNotPresent(redeployAffected, args);

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
                final List<String> links = loadLinks(client, name, group);
                if(!links.isEmpty()) {
                    ctx.printLine("SERVER GROUP: " + group + Util.LINE_SEPARATOR);
                    ctx.printColumns(links);
                    ctx.printLine("");
                }
            }
        } else {
            final List<String> content = loadLinks(client, name, sg);
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
        assertNotPresent(serverGroups, args);
        assertNotPresent(allServerGroups, args);
        assertNotPresent(allRelevantServerGroups, args);
        assertNotPresent(deployments, args);
        assertNotPresent(wildcards, args);
        assertNotPresent(content, args);
        assertNotPresent(redeployAffected, args);

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
        assertNotPresent(allServerGroups, args);

        final String name = this.name.getValue(args, true);
        if(name == null) {
            throw new CommandFormatException(this.name + " is missing value.");
        }
        final String contentStr = content.getValue(args);
        String deploymentStr = deployments.getValue(args);
        final String wildcardsStr = wildcards.getValue(args);
        if(wildcardsStr != null) {
            if(deploymentStr == null) {
                deploymentStr = wildcardsStr;
            } else {
                deploymentStr += ',' + wildcardsStr;
            }
        }
        final String sgStr = serverGroups.getValue(args);
        final List<String> sg;
        if(sgStr == null) {
            if(allRelevantServerGroups.isPresent(args)) {
                sg = Util.getServerGroupsReferencingOverlay(name, client);
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

        byte redeploy = this.redeployAffected.isPresent(args) ? REDEPLOY_ONLY_AFFECTED : REDEPLOY_NONE;

        // remove the content first and determine whether all the linked deployments
        // should be redeployed
        if(contentStr != null || deploymentStr == null && sg == null) {

            if(redeploy == REDEPLOY_ONLY_AFFECTED) {
                redeploy = REDEPLOY_ALL;
            }

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

        if(deploymentStr != null || contentStr == null) {
            // remove the overlay links

            if(ctx.isDomainMode()) {
                if(deploymentStr == null) {
                    final List<String> groups = sg == null ? Util.getServerGroupsReferencingOverlay(name, client) : sg;
                    for(String group : groups) {
                        addRemoveRedeployLinksSteps(client, steps, name, group, null, true, redeploy);
                    }
                } else {
                    if(ctx.isDomainMode() && sg == null) {
                        throw new CommandFormatException(serverGroups.getFullName() + " or " + allRelevantServerGroups.getFullName() + " is required.");
                    }
                    final List<String> links = Arrays.asList(deploymentStr.split(",+"));
                    for(String group : sg) {
                        addRemoveRedeployLinksSteps(client, steps, name, group, links, true, redeploy);
                    }
                }
            } else {
                if(deploymentStr == null) {
                    // remove all
                    addRemoveRedeployLinksSteps(client, steps, name, null, null, true, redeploy);
                } else {
                    final List<String> links = Arrays.asList(deploymentStr.split(",+"));
                    addRemoveRedeployLinksSteps(client, steps, name, null, links, true, redeploy);
                }
            }
        } else if(redeploy == REDEPLOY_ALL) {
            addRemoveRedeployLinksSteps(client, steps, name, null, null, false, redeploy);
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
                ctx.printLine("failed request: " + composite.toString());
                ctx.printLine("failed response: " + result.toString());
                throw new CommandFormatException(Util.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new CommandFormatException("Failed to remove overlay", e);
        }
    }

    protected void add(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        assertNotPresent(allRelevantServerGroups, args);

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
            String path = pair.substring(equalsIndex + 1);
            if(path.length() == 0) {
                throw new CommandFormatException("The filesystem paths is missing for the content '" + pair + "'");
            }
            path = pathCompleter.translatePath(path);
            final File f = new File(path);
            if(!f.exists()) {
                throw new CommandFormatException("Content file doesn't exist " + f.getAbsolutePath() + ", " + pathCompleter + ", windows=" + Util.isWindows());
            }
            contentPaths[i] = f;
        }

        final String[] deployments = getLinks(this.deployments, args);
        final String[] wildcards = getLinks(this.wildcards, args);

        final ModelControllerClient client = ctx.getModelControllerClient();

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

        if(deployments != null || wildcards != null) {
            if(ctx.isDomainMode()) {
                final List<String> sg = getServerGroupsToLink(ctx);
                for(String group : sg) {
                    // here we don't need a separate check whether the overlay is linked
                    // from the server group since it is created in the same op.
                    op = new ModelNode();
                    address = op.get(Util.ADDRESS);
                    address.add(Util.SERVER_GROUP, group);
                    address.add(Util.DEPLOYMENT_OVERLAY, name);
                    op.get(Util.OPERATION).set(Util.ADD);
                    steps.add(op);
                    if(deployments != null) {
                        addAddRedeployLinksSteps(ctx, steps, name, group, deployments, false);
                    }
                    if(wildcards != null) {
                        addAddRedeployLinksSteps(ctx, steps, name, group, wildcards, true);
                    }
                }
            } else {
                if(deployments != null) {
                    addAddRedeployLinksSteps(ctx, steps, name, null, deployments, false);
                }
                if(wildcards != null) {
                    addAddRedeployLinksSteps(ctx, steps, name, null, wildcards, true);
                }
            }
        } else if(ctx.isDomainMode() && (serverGroups.isPresent(args) || allServerGroups.isPresent(args))) {
            throw new CommandFormatException("server groups are specified but neither " + this.deployments.getFullName() +
                    " nor " + this.wildcards.getFullName() + " is.");
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

    protected void link(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        assertNotPresent(allRelevantServerGroups, args);

        final String name = this.name.getValue(args, true);
        final String[] deployments = getLinks(this.deployments, args);
        final String[] wildcards = getLinks(this.wildcards, args);
        if(deployments == null && wildcards == null) {
            throw new CommandFormatException("Either " + this.deployments.getFullName() + " or " + this.wildcards.getFullName() + " is required.");
        }

        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        final ModelControllerClient client = ctx.getModelControllerClient();
        if(ctx.isDomainMode()) {
            final List<String> sg = getServerGroupsToLink(ctx);
            for(String group : sg) {
                if(!Util.isValidPath(client, Util.SERVER_GROUP, group, Util.DEPLOYMENT_OVERLAY, name)) {
                    final ModelNode op = new ModelNode();
                    final ModelNode address = op.get(Util.ADDRESS);
                    address.add(Util.SERVER_GROUP, group);
                    address.add(Util.DEPLOYMENT_OVERLAY, name);
                    op.get(Util.OPERATION).set(Util.ADD);
                    steps.add(op);
                }
                if(deployments != null) {
                    addAddRedeployLinksSteps(ctx, steps, name, group, deployments, false);
                }
                if(wildcards != null) {
                    addAddRedeployLinksSteps(ctx, steps, name, group, wildcards, true);
                }
            }
        } else {
            if(deployments != null) {
                addAddRedeployLinksSteps(ctx, steps, name, null, deployments, false);
            }
            if(wildcards != null) {
                addAddRedeployLinksSteps(ctx, steps, name, null, wildcards, true);
            }
        }

        try {
            final ModelNode result = client.execute(composite);
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
            String path = pair.substring(equalsIndex + 1);
            if(path.length() == 0) {
                throw new CommandFormatException("The filesystem paths is missing for the content '" + pair + "'");
            }
            path = pathCompleter.translatePath(path);
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

        final ModelNode composite = new ModelNode();
        final OperationBuilder opBuilder = new OperationBuilder(composite, true);
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        // add the content
        for (int i = 0; i < contentNames.length; ++i) {
            final ModelNode op = new ModelNode();
            ModelNode address = op.get(Util.ADDRESS);
            address.add(Util.DEPLOYMENT_OVERLAY, name);
            address.add(Util.CONTENT, contentNames[i]);
            op.get(Util.OPERATION).set(Util.ADD);
            op.get(Util.CONTENT).get(Util.INPUT_STREAM_INDEX).set(i);
            opBuilder.addFileAsAttachment(contentPaths[i]);
            steps.add(op);
        }

        if(redeployAffected.isPresent(args)) {
            if(ctx.isDomainMode()) {
                for(String sgName : Util.getServerGroups(client)) {
                    addRemoveRedeployLinksSteps(client, steps, name, sgName, null, false, REDEPLOY_ALL);
                }
            } else {
                addRemoveRedeployLinksSteps(client, steps, name, null, null, false, REDEPLOY_ALL);
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

    protected List<String> loadLinks(final ModelControllerClient client, String overlay, String serverGroup) throws CommandLineException {
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

    protected ModelNode loadLinkResources(final ModelControllerClient client, String overlay, String serverGroup) throws CommandLineException {
        final ModelNode op = new ModelNode();
        final ModelNode addr = op.get(Util.ADDRESS);
        if(serverGroup != null) {
            addr.add(Util.SERVER_GROUP, serverGroup);
        }
        addr.add(Util.DEPLOYMENT_OVERLAY, overlay);
        op.get(Util.OPERATION).set(Util.READ_CHILDREN_RESOURCES);
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
            if(descr != null && (descr.contains("JBAS014807") || descr.contains("JBAS014793"))) {
                // resource doesn't exist
                return null;
            }
            throw new CommandLineException("Failed to load the list of deployments for overlay " + overlay + ": " + response);
        }
        return result;
    }

    protected void addRedeployStep(final ModelNode steps, final String deployment, String serverGroup) {
        final ModelNode step = new ModelNode();
        final ModelNode address = step.get(Util.ADDRESS);
        if(serverGroup != null) {
            address.add(Util.SERVER_GROUP, serverGroup);
        }
        address.add(Util.DEPLOYMENT, deployment);
        step.get(Util.OPERATION).set(Util.REDEPLOY);
        steps.add(step);
    }

    protected String[] getLinks(ArgumentWithValue linksArg, final ParsedCommandLine args) throws CommandFormatException {
        final String deploymentsStr = linksArg.getValue(args);
        final String[] deployments;
        if(deploymentsStr == null) {
            deployments = null;
        } else {
            deployments = deploymentsStr.split(",+");
            if(deployments.length == 0) {
                throw new CommandFormatException(linksArg.getFullName() + " is missing value.");
            }
        }
        return deployments;
    }

    protected List<String> getServerGroupsToLink(CommandContext ctx) throws CommandFormatException {
        final List<String> sg;
        final String sgStr = serverGroups.getValue(ctx.getParsedCommandLine());
        if(allServerGroups.isPresent(ctx.getParsedCommandLine())) {
            if(sgStr != null) {
                throw new CommandFormatException("Only one of " + allServerGroups.getFullName() + " or " + serverGroups.getFullName() + " can be specified at a time.");
            }
            sg = Util.getServerGroups(ctx.getModelControllerClient());
            if(sg.isEmpty()) {
                throw new CommandFormatException("No server group is available.");
            }
        } else {
            if(sgStr == null) {
                throw new CommandFormatException(serverGroups.getFullName() + " or " + allServerGroups.getFullName() + " must be specified.");
            }
            sg = Arrays.asList(sgStr.split(",+"));
            if(sg.isEmpty()) {
                throw new CommandFormatException(serverGroups.getFullName() + " is missing value.");
            }
        }
        return sg;
    }

    protected void addAddRedeployLinksSteps(CommandContext ctx, ModelNode steps,
            String overlay, String serverGroup, String[] links, boolean regexp)
                    throws CommandLineException {
        for(String link : links) {
            final ModelNode op = new ModelNode();
            final ModelNode address = op.get(Util.ADDRESS);
            if(serverGroup != null) {
                address.add(Util.SERVER_GROUP, serverGroup);
            }
            address.add(Util.DEPLOYMENT_OVERLAY, overlay);
            address.add(Util.DEPLOYMENT, link);
            op.get(Util.OPERATION).set(Util.ADD);
            if(regexp) {
                op.get(Util.REGULAR_EXPRESSION).set(true);
                steps.add(op);

                final List<String> matchingDeployments = Util.getMatchingDeployments(ctx.getModelControllerClient(), link, serverGroup);
                if(!matchingDeployments.isEmpty()) {
                    if(serverGroup == null) {
                        for(String deployment : matchingDeployments) {
                            final ModelNode step = new ModelNode();
                            final ModelNode addr = step.get(Util.ADDRESS);
                            addr.add(Util.DEPLOYMENT, deployment);
                            step.get(Util.OPERATION).set(Util.REDEPLOY);
                            steps.add(step);
                        }
                    } else {
                        for(String deployment : matchingDeployments) {
                            final ModelNode step = new ModelNode();
                            final ModelNode addr = step.get(Util.ADDRESS);
                            addr.add(Util.SERVER_GROUP, serverGroup);
                            addr.add(Util.DEPLOYMENT, deployment);
                            step.get(Util.OPERATION).set(Util.REDEPLOY);
                            steps.add(step);
                        }
                    }
                }
            } else if(redeployAffected.isPresent(ctx.getParsedCommandLine())) {
                steps.add(op);

                if(serverGroup == null) {
                    if(Util.isValidPath(ctx.getModelControllerClient(), Util.DEPLOYMENT, link)) {
                        final ModelNode step = new ModelNode();
                        final ModelNode addr = step.get(Util.ADDRESS);
                        addr.add(Util.DEPLOYMENT, link);
                        step.get(Util.OPERATION).set(Util.REDEPLOY);
                        steps.add(step);
                    }
                } else {
                    if(Util.isValidPath(ctx.getModelControllerClient(), Util.SERVER_GROUP, serverGroup, Util.DEPLOYMENT, link)) {
                        final ModelNode step = new ModelNode();
                        final ModelNode addr = step.get(Util.ADDRESS);
                        addr.add(Util.SERVER_GROUP, serverGroup);
                        addr.add(Util.DEPLOYMENT, link);
                        step.get(Util.OPERATION).set(Util.REDEPLOY);
                        steps.add(step);
                    }
                }
            } else {
                steps.add(op);
            }
        }
    }

    protected void addRemoveRedeployLinksSteps(ModelControllerClient client, ModelNode steps,
            String overlay, String sgName, List<String> specifiedLinks, boolean removeLinks, byte redeploy)
            throws CommandLineException {
        final ModelNode linkResources = loadLinkResources(client, overlay, sgName);
        if(linkResources == null) {
            return;
        }
        if(linkResources.keys().isEmpty()) {
            return;
        }

        if(removeLinks) {
            final Iterator<String> linkNames;
            if(specifiedLinks != null) {
                linkNames = specifiedLinks.iterator();
            } else {
                linkNames = linkResources.keys().iterator();
            }
            while(linkNames.hasNext()) {
                final String linkName = linkNames.next();
                final ModelNode op = new ModelNode();
                final ModelNode addr = op.get(Util.ADDRESS);
                if(sgName != null) {
                    addr.add(Util.SERVER_GROUP, sgName);
                }
                addr.add(Util.DEPLOYMENT_OVERLAY, overlay);
                addr.add(Util.DEPLOYMENT, linkName);
                op.get(Util.OPERATION).set(Util.REMOVE);
                steps.add(op);
            }
            if(specifiedLinks == null && sgName != null) {
                // this is only for the domain mode for the specific server group
                // TODO specified links may cover all, in which case it wouldn't clean this one
                final ModelNode op = new ModelNode();
                final ModelNode addr = op.get(Util.ADDRESS);
                addr.add(Util.SERVER_GROUP, sgName);
                addr.add(Util.DEPLOYMENT_OVERLAY, overlay);
                op.get(Util.OPERATION).set(Util.REMOVE);
                steps.add(op);
            }
        }

        // redeploy

        final Iterator<String> linkNames;
        if(redeploy == REDEPLOY_ALL) {
            linkNames = linkResources.keys().iterator();
        } else if(redeploy == REDEPLOY_ONLY_AFFECTED && specifiedLinks != null) {
            linkNames = specifiedLinks.iterator();
        } else {
            return;
        }

        final List<String> sgDeployments = Util.getDeployments(client, sgName);
        while(linkNames.hasNext() && !sgDeployments.isEmpty()) {
            final String linkName = linkNames.next();
            final ModelNode link = linkResources.get(linkName);
            if(!link.isDefined()) {
                final StringBuilder buf = new StringBuilder();
                buf.append(linkName);
                buf.append(" not found among the registered links ");
                if(sgName != null) {
                    buf.append("for server group ").append(sgName).append(' ');
                }
                buf.append(linkResources.keys());
                throw new CommandFormatException(buf.toString());
            }
            addRedeploySteps(steps, sgName, linkName, link, sgDeployments);
        }
    }

    protected void addRedeploySteps(ModelNode steps, String serverGroup, String linkName, ModelNode link, List<String> remainingDeployments) {
        final boolean regexp;
        if(link.has(Util.REGULAR_EXPRESSION)) {
            regexp = link.get(Util.REGULAR_EXPRESSION).asBoolean();
        } else {
            regexp = false;
        }
        if(regexp) {
            final Pattern pattern = Pattern.compile(Util.wildcardToJavaRegex(linkName));
            final Iterator<String> i = remainingDeployments.iterator();
            while(i.hasNext()) {
                final String deployment = i.next();
                if(pattern.matcher(deployment).matches()) {
                    i.remove();
                    addRedeployStep(steps, deployment, serverGroup);
                }
            }
        } else {
            if(remainingDeployments.remove(linkName)) {
                addRedeployStep(steps, linkName, serverGroup);
            }
        }
    }

    protected void assertNotPresent(ArgumentWithoutValue arg, ParsedCommandLine args) throws CommandFormatException {
        if(arg.isPresent(args)) {
            throw new CommandFormatException(arg.getFullName() + " is not allowed with action '" + action.getValue(args) + "'");
        }
    }
}
