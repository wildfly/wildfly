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


import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.protocol.old.StreamUtils;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeployHandler extends BatchModeCommandHandler {

    private final ArgumentWithoutValue force;
    private final ArgumentWithoutValue l;
    private final ArgumentWithoutValue path;
    private final ArgumentWithoutValue name;
    private final ArgumentWithoutValue rtName;
    private final ArgumentWithValue serverGroups;
    private final ArgumentWithoutValue allServerGroups;
    private final ArgumentWithoutValue disabled;

    public DeployHandler() {
        super("deploy", true);

        l = new ArgumentWithoutValue(this, "-l");
        l.setExclusive(true);

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? WindowsFilenameTabCompleter.INSTANCE : DefaultFilenameTabCompleter.INSTANCE;
        path = new ArgumentWithValue(this, pathCompleter, 0, "--path") {
            @Override
            public String getValue(ParsedArguments args) {
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

        force = new ArgumentWithoutValue(this, "--force", "-f");
        force.addRequiredPreceding(path);

        name = new ArgumentWithValue(this, new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

                ParsedArguments args = ctx.getParsedArguments();
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

            }}, "--name");
        name.addCantAppearAfter(l);
        path.addCantAppearAfter(name);
        //name.addRequiredPreceding(path);

        rtName = new ArgumentWithValue(this, "--runtime-name");
        rtName.addRequiredPreceding(path);

        allServerGroups = new ArgumentWithoutValue(this, "--all-server-groups")  {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };

        allServerGroups.addRequiredPreceding(path);
        allServerGroups.addRequiredPreceding(name);

        serverGroups = new ArgumentWithValue(this, new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                List<String> allGroups = Util.getServerGroups(ctx.getModelControllerClient());
                if(buffer.isEmpty()) {
                    candidates.addAll(allGroups);
                    Collections.sort(candidates);
                    return 0;
                }

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
        serverGroups.addRequiredPreceding(path);
        serverGroups.addRequiredPreceding(name);

        serverGroups.addCantAppearAfter(allServerGroups);
        allServerGroups.addCantAppearAfter(serverGroups);

        disabled = new ArgumentWithoutValue(this, "--disabled");
        disabled.addRequiredPreceding(path);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        ModelControllerClient client = ctx.getModelControllerClient();

        ParsedArguments args = ctx.getParsedArguments();
        boolean l = this.l.isPresent(args);
        if (!args.hasArguments() || l) {
            printList(ctx, Util.getDeployments(client), l);
            return;
        }

        final String path = this.path.getValue(args);
        final File f;
        if(path != null) {
            f = new File(path);
            if(!f.exists()) {
                ctx.printLine("Path " + f.getAbsolutePath() + " doesn't exist.");
                return;
            }
            if(f.isDirectory()) {
                ctx.printLine(f.getAbsolutePath() + " is a directory.");
                return;
            }
        } else {
            f = null;
        }

        String name = this.name.getValue(args);
        if(name == null) {
            if(f == null) {
                ctx.printLine("Either path or --name is requied.");
                return;
            }
            name = f.getName();
        }

        final String runtimeName = rtName.getValue(args);

        if(Util.isDeploymentInRepository(name, client) && f != null) {
            if(force.isPresent(args)) {
                DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();

                ModelNode result;

                // replace
                builder = new DefaultOperationRequestBuilder();
                builder.setOperationName("full-replace-deployment");
                builder.addProperty("name", name);
                if(runtimeName != null) {
                    builder.addProperty("runtime-name", runtimeName);
                }

                FileInputStream is = null;
                try {
                    is = new FileInputStream(f);
                    ModelNode request = builder.buildRequest();
                    OperationBuilder op = new OperationBuilder(request);
                    op.addInputStream(is);
                    request.get("content").get(0).get("input-stream-index").set(0);
                    result = client.execute(op.build());
                } catch(Exception e) {
                    ctx.printLine("Failed to replace the deployment: " + e.getLocalizedMessage());
                    return;
                } finally {
                    StreamUtils.safeClose(is);
                }
                if(!Util.isSuccess(result)) {
                    ctx.printLine(Util.getFailureDescription(result));
                    return;
                }

                ctx.printLine("'" + name + "' re-deployed successfully.");
            } else {
                ctx.printLine("'" + name + "' is already deployed (use " + force.getFullName() + " to force re-deploy).");
            }

            return;
        } else {

            DefaultOperationRequestBuilder builder;
            ModelNode result;

            // add
            if (f != null) {
                builder = new DefaultOperationRequestBuilder();
                builder.setOperationName("add");
                builder.addNode("deployment", name);
                if (runtimeName != null) {
                    builder.addProperty("runtime-name", runtimeName);
                }

                FileInputStream is = null;
                try {
                    is = new FileInputStream(f);
                    ModelNode request = builder.buildRequest();
                    OperationBuilder op = new OperationBuilder(request);
                    op.addInputStream(is);
                    request.get("content").get(0).get("input-stream-index").set(0);
                    result = client.execute(op.build());
                } catch (Exception e) {
                    ctx.printLine("Failed to add the deployment content to the repository: " + e.getLocalizedMessage());
                    return;
                } finally {
                    StreamUtils.safeClose(is);
                }
                if (!Util.isSuccess(result)) {
                    ctx.printLine(Util.getFailureDescription(result));
                    return;
                }
            }

            //deploy
            if (!disabled.isPresent(args)) {
                final ModelNode request;

                if (ctx.isDomainMode()) {

                    final List<String> serverGroups;
                    if (ctx.isDomainMode()) {
                        if(allServerGroups.isPresent(args)) {
                            serverGroups = Util.getServerGroups(client);
                        } else {
                            String serverGroupsStr = this.serverGroups.getValue(args);
                            if(serverGroupsStr == null) {
                                ctx.printLine("Either --all-server-groups or --server-groups must be specified.");
                                return;
                            }
                            serverGroups = Arrays.asList(serverGroupsStr.split(","));
                        }

                        if(serverGroups.isEmpty()) {
                            ctx.printLine("No server group is available.");
                            return;
                        }
                    } else {
                        serverGroups = null;
                    }

                    request = new ModelNode();
                    request.get("operation").set("composite");
                    request.get("address").setEmptyList();
                    ModelNode steps = request.get("steps");

                    for (String serverGroup : serverGroups) {
                        steps.add(Util.configureDeploymentOperation("add", name, serverGroup));
                    }

                    for (String serverGroup : serverGroups) {
                        steps.add(Util.configureDeploymentOperation("deploy", name, serverGroup));
                    }
                } else {
                    builder = new DefaultOperationRequestBuilder();
                    builder.setOperationName("deploy");
                    builder.addNode("deployment", name);
                    try {
                        request = builder.buildRequest();
                    } catch (Exception e) {
                        ctx.printLine("Failed to deploy: " + e.getLocalizedMessage());
                        return;
                    }
                }

                try {
                    result = client.execute(request);
                } catch (Exception e) {
                    ctx.printLine("Failed to deploy: " + e.getLocalizedMessage());
                    return;
                }

                if (!Util.isSuccess(result)) {
                    ctx.printLine(Util.getFailureDescription(result));
                    return;
                }
            }
            ctx.printLine("'" + name + "' deployed successfully.");
        }
    }

    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {

        ParsedArguments args = ctx.getParsedArguments();
        if (!args.hasArguments()) {
            throw new OperationFormatException("Required arguments are missing.");
        }

        final String filePath = path.getValue(args);
        String name = this.name.getValue(args);
        String runtimeName = rtName.getValue(args);

        final File f;
        if(filePath != null ) {
            f = new File(filePath);
            if(!f.exists()) {
                throw new OperationFormatException(f.getAbsolutePath() + " doesn't exist.");
            }

            if(name == null) {
                name = f.getName();
            }
        } else {
            f = null;
            if(name == null) {
                throw new CommandFormatException("Either file path or --name has to be specified.");
            }
        }

        if(Util.isDeploymentInRepository(name, ctx.getModelControllerClient()) && f != null) {
            if(force.isPresent(args)) {
                DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();

                // replace
                builder = new DefaultOperationRequestBuilder();
                builder.setOperationName("full-replace-deployment");
                builder.addProperty("name", name);
                if(runtimeName != null) {
                    builder.addProperty("runtime-name", runtimeName);
                }

                byte[] bytes = readBytes(f);
                builder.getModelNode().get("content").get(0).get("bytes").set(bytes);
                return builder.buildRequest();
            } else {
                throw new OperationFormatException("'" + name + "' is already deployed (use -f to force re-deploy).");
            }
        }

        final List<String> serverGroups;
        if (ctx.isDomainMode()) {
            if(allServerGroups.isPresent(args)) {
                serverGroups = Util.getServerGroups(ctx.getModelControllerClient());
            } else {
                String serverGroupsStr = this.serverGroups.getValue(args);
                if(serverGroupsStr == null) {
                    throw new OperationFormatException("Either --all-server-groups or --server-groups must be specified.");
                }
                serverGroups = Arrays.asList(serverGroupsStr.split(","));
            }

            if(serverGroups.isEmpty() && !disabled.isPresent(args)) {
                new OperationFormatException("No server group is available.");
            }
        } else {
            serverGroups = null;
        }

        ModelNode composite = new ModelNode();
        composite.get("operation").set("composite");
        composite.get("address").setEmptyList();
        ModelNode steps = composite.get("steps");

        DefaultOperationRequestBuilder builder;

        // add
        if (f != null) {
            builder = new DefaultOperationRequestBuilder();
            builder.setOperationName("add");
            builder.addNode("deployment", name);
            if (runtimeName != null) {
                builder.addProperty("runtime-name", runtimeName);
            }

            byte[] bytes = readBytes(f);
            builder.getModelNode().get("content").get(0).get("bytes").set(bytes);
            steps.add(builder.buildRequest());
        }

        if(!disabled.isPresent(args)) {
            // deploy
            if (ctx.isDomainMode()) {
                for (String serverGroup : serverGroups) {
                    steps.add(Util.configureDeploymentOperation("add", name, serverGroup));
                }
                for (String serverGroup : serverGroups) {
                    steps.add(Util.configureDeploymentOperation("deploy", name, serverGroup));
                }
            } else {
                builder = new DefaultOperationRequestBuilder();
                builder.setOperationName("deploy");
                builder.addNode("deployment", name);
                steps.add(builder.buildRequest());
            }
        }
        return composite;
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
}
