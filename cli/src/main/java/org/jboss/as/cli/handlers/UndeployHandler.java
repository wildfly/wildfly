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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.spi.MountHandle;

/**
 *
 * @author Alexey Loubyansky
 */
public class UndeployHandler extends DeploymentHandler {

    private final ArgumentWithoutValue l;
    private final ArgumentWithoutValue path;
    private final ArgumentWithValue name;
    private final ArgumentWithValue serverGroups;
    private final ArgumentWithoutValue allRelevantServerGroups;
    private final ArgumentWithoutValue keepContent;
    private final ArgumentWithValue script;

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

                ParsedCommandLine args = ctx.getParsedCommandLine();
                try {
                    if(path.isPresent(args)) {
                        return -1;
                    }
                } catch (CommandFormatException e) {
                    return -1;
                }

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

        serverGroups = new ArgumentWithValue(this, new CommaSeparatedCompleter() {
            @Override
            protected Collection<String> getAllCandidates(CommandContext ctx) {
              final String deploymentName = name.getValue(ctx.getParsedCommandLine());
              final List<String> allGroups;
//              if(deploymentName == null) {
//                  allGroups = Util.getServerGroups(ctx.getModelControllerClient());
//              } else {
                  try {
                    allGroups = Util.getServerGroupsReferencingDeployment(deploymentName, ctx.getModelControllerClient());
                } catch (CommandLineException e) {
                    e.printStackTrace();
                    return Collections.emptyList();
                }
//              }
                  return allGroups;
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

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
        path = new ArgumentWithValue(this, pathCompleter, "--path") {
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
        path.addCantAppearAfter(l);

        script = new ArgumentWithValue(this, "--script");
        script.addRequiredPreceding(path);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final ModelControllerClient client = ctx.getModelControllerClient();
        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final boolean l = this.l.isPresent(args);
        if(!args.hasProperties() || l) {
            this.listDeployments(ctx, l);
            return;
        }

        final String path = this.path.getValue(args);
        final File f;
        if(path != null) {
            f = new File(path);
            if(!f.exists()) {
                throw new CommandFormatException("Path '" + f.getAbsolutePath() + "' doesn't exist.");
            } else if (!isCliArchive(f)) {
                throw new CommandFormatException("File '" + f.getAbsolutePath() + "' is not a valid CLI archive. CLI archives should have a '.cli' extension.");
            }
        } else {
            f = null;
        }

        if (isCliArchive(f)) {
            final ModelNode request = buildRequest(ctx);
            if(request == null) {
                throw new CommandFormatException("Operation request wasn't built.");
            }
            try {
                final ModelNode result = client.execute(request);
                if(Util.isSuccess(result)) {
                    return;
                } else {
                    throw new CommandFormatException("Failed to execute archive script: " + Util.getFailureDescription(result));
                }
            } catch (IOException e) {
                throw new CommandFormatException("Failed to execute archive script: " + e.getLocalizedMessage(), e);
            }
        }

        final String name = this.name.getValue(ctx.getParsedCommandLine());
        if (name == null) {
            printList(ctx, Util.getDeployments(client), l);
            return;
        }

        final ModelNode request = buildRequestWOValidation(ctx);
        addHeaders(ctx, request);

        final ModelNode result;
        try {
            result = client.execute(request);
        } catch (Exception e) {
            throw new CommandFormatException("Undeploy failed: " + e.getLocalizedMessage());
        }
        if (!Util.isSuccess(result)) {
            throw new CommandFormatException("Undeploy failed: " + Util.getFailureDescription(result));
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
        final boolean keepContent = this.keepContent.isPresent(args);
        final boolean allRelevantServerGroups = this.allRelevantServerGroups.isPresent(args);
        final String serverGroupsStr = this.serverGroups.getValue(args);

        final String path = this.path.getValue(args);
        final File f;
        if(path != null) {
            f = new File(path);
            if(!f.exists()) {
                throw new OperationFormatException("Path '" + f.getAbsolutePath() + "' doesn't exist.");
            }
            if(!isCliArchive(f)) {
                throw new OperationFormatException("File '" + f.getAbsolutePath() + "' is not a valid CLI archive. CLI archives should have a '.cli' extension.");
            }
        } else {
            f = null;
        }
        if (isCliArchive(f)) {
            if (name != null) {
                throw new OperationFormatException(this.name.getFullName() + " can't be used in combination with a CLI archive.");
            }

            if(serverGroupsStr != null || allRelevantServerGroups) {
                throw new OperationFormatException(this.serverGroups.getFullName() + " and " + this.allRelevantServerGroups.getFullName() +
                        " can't be used in combination with a CLI archive.");
            }

            if (keepContent) {
                throw new OperationFormatException(this.keepContent.getFullName() + " can't be used in combination with a CLI archive.");
            }

            MountHandle root;
            try {
                root = extractArchive(f);
            } catch (IOException e) {
                throw new OperationFormatException("Unable to extract archive '" + f.getAbsolutePath() + "' to temporary location");
            }

            final File currentDir = ctx.getCurrentDir();
            ctx.setCurrentDir(root.getMountSource());
            String holdbackBatch = activateNewBatch(ctx);

            try {
                String script = this.script.getValue(args);
                if (script == null) {
                    script = "undeploy.scr";
                }

                File scriptFile = new File(ctx.getCurrentDir(),script);
                if (!scriptFile.exists()) {
                    throw new CommandFormatException("ERROR: script '" + script + "' not found in archive '" + f.getAbsolutePath() + "'.");
                }

                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(scriptFile));
                    String line = reader.readLine();
                    while (!ctx.isTerminated() && line != null) {
                        ctx.handle(line);
                        line = reader.readLine();
                    }
                } catch (FileNotFoundException e) {
                    throw new CommandFormatException("ERROR: script '" + script + "' not found in archive '" + f.getAbsolutePath() + "'.");
                } catch (IOException e) {
                    throw new CommandFormatException("Failed to read the next command from " + scriptFile.getName() + ": " + e.getMessage(), e);
                } catch (CommandLineException e) {
                    throw new CommandFormatException(e.getMessage(), e);
                } finally {
                    if(reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                        }
                    }
                }

                return ctx.getBatchManager().getActiveBatch().toRequest();
            } finally {
                // reset current dir in context
                ctx.setCurrentDir(currentDir);
                discardBatch(ctx, holdbackBatch);
                try {
                    root.close();
                } catch (IOException ignore) {}
            }
        }

        if(name == null) {
            throw new OperationFormatException("Deployment name is missing.");
        }

        final ModelControllerClient client = ctx.getModelControllerClient();
        DefaultOperationRequestBuilder builder;

        final List<String> deploymentNames;
        if(name.indexOf('*') < 0) {
            deploymentNames = Collections.singletonList(name);
        } else {
            deploymentNames = Util.getDeployments(client, name);
            if(deploymentNames.isEmpty()) {
                throw new CommandFormatException("No deployment matched wildcard expression " + name);
            }
        }

        for(String deploymentName : deploymentNames) {

            final List<String> serverGroups;
            if(ctx.isDomainMode()) {
                if(allRelevantServerGroups) {
                    if(keepContent) {
                        serverGroups = Util.getAllEnabledServerGroups(deploymentName, client);
                    } else {
                        try {
                            serverGroups = Util.getServerGroupsReferencingDeployment(deploymentName, client);
                        } catch (CommandLineException e) {
                            throw new CommandFormatException("Failed to retrieve all referencing server groups", e);
                        }
                    }
                } else {
                    if(serverGroupsStr == null) {
                        //throw new OperationFormatException("Either --all-relevant-server-groups or --server-groups must be specified.");
                        serverGroups = Collections.emptyList();
                    } else {
                        serverGroups = Arrays.asList(serverGroupsStr.split(","));
                    }
                }

                if(serverGroups.isEmpty()) {
                    if(keepContent) {
                        throw new OperationFormatException("None of the server groups is specified or references specified deployment.");
                    }
                } else {
                    for (String group : serverGroups){
                        ModelNode groupStep = Util.configureDeploymentOperation(Util.UNDEPLOY, deploymentName, group);
                        steps.add(groupStep);
//                      if(!keepContent) {
                            groupStep = Util.configureDeploymentOperation(Util.REMOVE, deploymentName, group);
                            steps.add(groupStep);
//                      }
                    }
                }
            } else {
                if(Util.isDeployedAndEnabledInStandalone(deploymentName, client)) {
                    builder = new DefaultOperationRequestBuilder();
                    builder.setOperationName(Util.UNDEPLOY);
                    builder.addNode(Util.DEPLOYMENT, deploymentName);
                    steps.add(builder.buildRequest());
                }
            }
        }

        if (!keepContent) {
            for(String deploymentName : deploymentNames) {
                builder = new DefaultOperationRequestBuilder();
                builder.setOperationName(Util.REMOVE);
                builder.addNode(Util.DEPLOYMENT, deploymentName);
                steps.add(builder.buildRequest());
            }
        }
        return composite;
    }
}
