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
package org.jboss.as.cli.handlers;


import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_REMOVE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_UNDEPLOY_OPERATION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class UndeployHandler extends BatchModeCommandHandler {

    private final ArgumentWithoutValue l;
    private final ArgumentWithValue name;
    private final ArgumentWithValue serverGroups;
    private final ArgumentWithoutValue allRelevantServerGroups;
    private final ArgumentWithoutValue keepContent;

    public UndeployHandler(CommandContext ctx) {
        super(ctx, "undeploy", true);

        final DefaultOperationRequestAddress requiredAddress = new DefaultOperationRequestAddress();
        requiredAddress.toNodeType(Util.DEPLOYMENT);
        addRequiredPath(requiredAddress);

        l = new ArgumentWithoutValue(this, "-l");
        l.setExclusive(true);

        name = new ArgumentWithValue(this, new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

                int nextCharIndex = 0;
                while (nextCharIndex < buffer.length()) {
                    if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                        break;
                    }
                    ++nextCharIndex;
                }

                if(ctx.getModelControllerClient() != null) {
                    List<String> deployments = Util.getDeployments(ctx.getModelControllerClient());
                    if(deployments.isEmpty()) {
                        return -1;
                    }

                    String opBuffer = buffer.substring(nextCharIndex).trim();
                    if (opBuffer.isEmpty()) {
                        candidates.addAll(deployments);
                    } else {
                        for(String name : deployments) {
                            if(name.startsWith(opBuffer)) {
                                candidates.add(name);
                            }
                        }
                        Collections.sort(candidates);
                    }
                    return nextCharIndex;
                } else {
                    return -1;
                }

            }}, 0, "--name");
        name.addCantAppearAfter(l);

        allRelevantServerGroups = new ArgumentWithoutValue(this, "--all-relevant-server-groups") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
        allRelevantServerGroups.addRequiredPreceding(name);

        serverGroups = new ArgumentWithValue(this, new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

                if(buffer.isEmpty()) {
                    candidates.addAll(Util.getServerGroups(ctx.getModelControllerClient()));
                    Collections.sort(candidates);
                    return 0;
                }

//                final String deploymentName = name.getValue(ctx.getParsedArguments());
                final List<String> allGroups;
//                if(deploymentName == null) {
                    allGroups = Util.getServerGroups(ctx.getModelControllerClient());
//                } else {
//                    allGroups = Util.getAllReferencingServerGroups(deploymentName, ctx.getModelControllerClient());
//                }

                final String[] groups = buffer.split(",+");

                final String chunk;
                final int lastGroupIndex;
                if(buffer.charAt(buffer.length() - 1) == ',') {
                    lastGroupIndex = groups.length;
                    chunk = null;
                } else {
                    lastGroupIndex = groups.length - 1;
                    chunk = groups[groups.length - 1];
                }

                for(int i = 0; i < lastGroupIndex; ++i) {
                    allGroups.remove(groups[i]);
                }

                final int result;
                if(chunk == null) {
                    candidates.addAll(allGroups);
                    result = buffer.length();
                } else {
                    for(String group : allGroups) {
                        if(group.startsWith(chunk)) {
                            candidates.add(group);
                        }
                    }
                    result = buffer.lastIndexOf(',') + 1;
                }
                Collections.sort(candidates);
                return result;
            }}, "--server-groups") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
        serverGroups.addRequiredPreceding(name);

        serverGroups.addCantAppearAfter(allRelevantServerGroups);
        allRelevantServerGroups.addCantAppearAfter(serverGroups);

        keepContent = new ArgumentWithoutValue(this, "--keep-content");
        keepContent.addRequiredPreceding(name);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        final ModelControllerClient client = ctx.getModelControllerClient();
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final boolean l = this.l.isPresent(args);
        if(!args.hasProperties() || l) {
            printList(ctx, Util.getDeployments(client), l);
            return;
        }

        final String name = this.name.getValue(ctx.getParsedCommandLine());
        if (name == null) {
            printList(ctx, Util.getDeployments(client), l);
            return;
        }

        final ModelNode request;
        try {
            request = buildRequest(ctx);
            addHeaders(ctx, request);
        } catch (OperationFormatException e) {
            ctx.error(e.getLocalizedMessage());
            return;
        }

        final ModelNode result;
        try {
            result = client.execute(request);
        } catch (Exception e) {
            ctx.error("Undeploy failed: " + e.getLocalizedMessage());
            return;
        }
        if (!Util.isSuccess(result)) {
            ctx.error("Undeploy failed: " + Util.getFailureDescription(result));
            return;
        }
    }

    @Override
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {

        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String name = this.name.getValue(args);
        if(name == null) {
            throw new OperationFormatException("Required argument name are missing.");
        }

        final ModelControllerClient client = ctx.getModelControllerClient();
        DefaultOperationRequestBuilder builder;

        final boolean keepContent;
        try {
            keepContent = this.keepContent.isPresent(args);
        } catch (CommandFormatException e) {
            throw new OperationFormatException(e.getLocalizedMessage());
        }
        if(ctx.isDomainMode()) {
            final List<String> serverGroups;
            if(allRelevantServerGroups.isPresent(args)) {
                if(keepContent) {
                    serverGroups = Util.getAllEnabledServerGroups(name, client);
                } else {
                    serverGroups = Util.getAllReferencingServerGroups(name, client);
                }
            } else {
                final String serverGroupsStr = this.serverGroups.getValue(args);
                if(serverGroupsStr == null) {
                    //throw new OperationFormatException("Either --all-relevant-server-groups or --server-groups must be specified.");
                    serverGroups = Collections.emptyList();
                } else {
                    serverGroups = Arrays.asList(serverGroupsStr.split(","));
                }
            }

            if(serverGroups.isEmpty()) {
                if(keepContent) {
                    throw new OperationFormatException("None server group is specified or available.");
                }
            } else {
                for (String group : serverGroups){
                    ModelNode groupStep = Util.configureDeploymentOperation(DEPLOYMENT_UNDEPLOY_OPERATION, name, group);
                    steps.add(groupStep);
                }

//                if(!keepContent) {
                    for (String group : serverGroups) {
                        ModelNode groupStep = Util.configureDeploymentOperation(DEPLOYMENT_REMOVE_OPERATION, name, group);
                        steps.add(groupStep);
                    }
//                }
            }
        } else if(Util.isDeployedAndEnabledInStandalone(name, client)) {
            builder = new DefaultOperationRequestBuilder();
            builder.setOperationName("undeploy");
            builder.addNode("deployment", name);
            steps.add(builder.buildRequest());
        }

        if (!keepContent) {
            builder = new DefaultOperationRequestBuilder();
            builder.setOperationName("remove");
            builder.addNode("deployment", name);
            steps.add(builder.buildRequest());
        }
        return composite;
    }
}
