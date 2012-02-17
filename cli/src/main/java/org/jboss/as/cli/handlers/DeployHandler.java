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
import java.io.IOException;
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
import org.jboss.as.cli.util.StrictSizeTable;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

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

    public DeployHandler(CommandContext ctx) {
        super(ctx, "deploy", true);

        final DefaultOperationRequestAddress requiredAddress = new DefaultOperationRequestAddress();
        requiredAddress.toNodeType(Util.DEPLOYMENT);
        addRequiredPath(requiredAddress);

        l = new ArgumentWithoutValue(this, "-l");
        l.setExclusive(true);

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
        path = new ArgumentWithValue(this, pathCompleter, 0, "--path") {
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

        force = new ArgumentWithoutValue(this, "--force", "-f");
        force.addRequiredPreceding(path);

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

            }}, "--name");
        name.addCantAppearAfter(l);
        path.addCantAppearAfter(name);

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
        allServerGroups.addCantAppearAfter(force);
        force.addCantAppearAfter(allServerGroups);

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
        serverGroups.addCantAppearAfter(force);
        force.addCantAppearAfter(serverGroups);

        serverGroups.addCantAppearAfter(allServerGroups);
        allServerGroups.addCantAppearAfter(serverGroups);

        disabled = new ArgumentWithoutValue(this, "--disabled");
        disabled.addRequiredPreceding(path);
        disabled.addCantAppearAfter(serverGroups);
        disabled.addCantAppearAfter(allServerGroups);
        disabled.addCantAppearAfter(force);
        force.addCantAppearAfter(disabled);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        final ModelControllerClient client = ctx.getModelControllerClient();

        ParsedCommandLine args = ctx.getParsedCommandLine();
        boolean l = this.l.isPresent(args);
        if (!args.hasProperties() || l) {
            listDeployments(ctx, l);
            return;
        }

        final String path = this.path.getValue(args);
        final File f;
        if(path != null) {
            f = new File(path);
            if(!f.exists()) {
                ctx.error("Path " + f.getAbsolutePath() + " doesn't exist.");
                return;
            }
            if(f.isDirectory()) {
                ctx.error(f.getAbsolutePath() + " is a directory.");
                return;
            }
        } else {
            f = null;
        }

        String name = this.name.getValue(args);
        if(name == null) {
            if(f == null) {
                ctx.error("Either path or --name is required.");
                return;
            }
            name = f.getName();
        }

        final String runtimeName = rtName.getValue(args);

        final boolean force = this.force.isPresent(args);
        final boolean disabled = this.disabled.isPresent(args);
        final String serverGroups = this.serverGroups.getValue(args);
        final boolean allServerGroups = this.allServerGroups.isPresent(args);

        if(force) {
            if(f == null) {
                ctx.error(this.force.getFullName() + " requires a filesystem path of the deployment to be added to the deployment repository.");
                return;
            }
            if(disabled || serverGroups != null || allServerGroups) {
                ctx.error(this.force.getFullName() +
                        " only replaces the content in the deployment repository and can't be used in combination with any of " +
                        this.disabled.getFullName() + ", " + this.serverGroups.getFullName() + " or " + this.allServerGroups.getFullName() + '.');
                return;
            }

            if(Util.isDeploymentInRepository(name, client)) {
                replaceDeployment(ctx, f, name, runtimeName);
                return;
            } else if(ctx.isDomainMode()) {
                // add deployment to the repository (enabled in standalone, disabled in domain (i.e. not associated with any sg))
                final ModelNode request = buildAddRequest(ctx, f, name, runtimeName);
                execute(ctx, request, f);
                return;
            }
            // standalone mode will add and deploy
        }

        if(disabled) {
            if(f == null) {
                ctx.error(this.disabled.getFullName() + " requires a filesystem path of the deployment to be added to the deployment repository.");
                return;
            }

            if(serverGroups != null || allServerGroups) {
                ctx.error(this.serverGroups.getFullName() + " and " + this.allServerGroups.getFullName() +
                        " can't be used in combination with " + this.disabled.getFullName() + '.');
                return;
            }

            if(Util.isDeploymentInRepository(name, client)) {
                ctx.error("'" + name + "' already exists in the deployment repository (use " +
                this.force.getFullName() + " to replace the existing content in the repository).");
                return;
            }

            // add deployment to the repository disabled
            final ModelNode request = buildAddRequest(ctx, f, name, runtimeName);
            execute(ctx, request, f);
            return;
        }

        // actually, the deployment is added before it is deployed
        // but this code here is to validate arguments and not to add deployment if something is wrong
        final ModelNode deployRequest;
        if(ctx.isDomainMode()) {
            final List<String> sgList;
            if(allServerGroups) {
                if(serverGroups != null) {
                    ctx.error(this.serverGroups.getFullName() + " can't appear in the same command with " + this.allServerGroups.getFullName());
                    return;
                }
                sgList = Util.getServerGroups(client);
                if(sgList.isEmpty()) {
                    ctx.error("No server group is available.");
                    return;
                }
            } else if(serverGroups == null) {
                final StringBuilder buf = new StringBuilder();
                buf.append("One of ");
                if(f != null) {
                    buf.append(this.disabled.getFullName()).append(", ");
                }
                buf.append(this.allServerGroups.getFullName() + " or " + this.serverGroups.getFullName() + " is missing.");
                ctx.error(buf.toString());
                return;
            } else {
                sgList = Arrays.asList(serverGroups.split(","));
                if(sgList.isEmpty()) {
                    ctx.error("Couldn't locate server group name in '" + this.serverGroups.getFullName() + "=" + serverGroups + "'.");
                    return;
                }
            }

            deployRequest = new ModelNode();
            deployRequest.get(Util.OPERATION).set(Util.COMPOSITE);
            deployRequest.get(Util.ADDRESS).setEmptyList();
            ModelNode steps = deployRequest.get(Util.STEPS);
            for (String serverGroup : sgList) {
                steps.add(Util.configureDeploymentOperation(Util.ADD, name, serverGroup));
            }
            for (String serverGroup : sgList) {
                steps.add(Util.configureDeploymentOperation(Util.DEPLOY, name, serverGroup));
            }
        } else {
            if(serverGroups != null || allServerGroups) {
                ctx.error(this.serverGroups.getFullName() + " and " + this.allServerGroups.getFullName() +
                        " can't appear in standalone mode.");
                return;
            }
            deployRequest = new ModelNode();
            deployRequest.get(Util.OPERATION).set(Util.DEPLOY);
            deployRequest.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        }

        if(f != null) {
            if(Util.isDeploymentInRepository(name, client)) {
                ctx.error("'" + name + "' already exists in the deployment repository (use " +
                this.force.getFullName() + " to replace the existing content in the repository).");
                return;
            }
            final ModelNode request = new ModelNode();
            request.get(Util.OPERATION).set(Util.COMPOSITE);
            request.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = request.get(Util.STEPS);
            steps.add(buildAddRequest(ctx, f, name, runtimeName));
            steps.add(deployRequest);
            execute(ctx, request, f);
            return;
        } else if(!Util.isDeploymentInRepository(name, client)) {
            ctx.error("'" + name + "' is not found among the registered deployments.");
            return;
        }

        try {
            final ModelNode result = client.execute(deployRequest);
            if (!Util.isSuccess(result)) {
                ctx.error(Util.getFailureDescription(result));
                return;
            }
        } catch (Exception e) {
            ctx.error("Failed to deploy: " + e.getLocalizedMessage());
            return;
        }
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {

        final ModelControllerClient client = ctx.getModelControllerClient();

        ParsedCommandLine args = ctx.getParsedCommandLine();
        boolean l = this.l.isPresent(args);
        if (!args.hasProperties() || l) {
            throw new OperationFormatException("Command is missing arguments for non-interactive mode: '" + args.getOriginalLine() + "'.");
        }

        final String path = this.path.getValue(args);
        final File f;
        if(path != null) {
            f = new File(path);
            if(!f.exists()) {
                throw new OperationFormatException("Path " + f.getAbsolutePath() + " doesn't exist.");
            }
            if(f.isDirectory()) {
                throw new OperationFormatException(f.getAbsolutePath() + " is a directory.");
            }
        } else {
            f = null;
        }

        String name = this.name.getValue(args);
        if(name == null) {
            if(f == null) {
                throw new OperationFormatException("Either path or --name is required.");
            }
            name = f.getName();
        }

        final String runtimeName = rtName.getValue(args);

        final boolean force = this.force.isPresent(args);
        final boolean disabled = this.disabled.isPresent(args);
        final String serverGroups = this.serverGroups.getValue(args);
        final boolean allServerGroups = this.allServerGroups.isPresent(args);

        if(force) {
            if(f == null) {
                throw new OperationFormatException(this.force.getFullName() + " requires a filesystem path of the deployment to be added to the deployment repository.");
            }
            if(disabled || serverGroups != null || allServerGroups) {
                throw new OperationFormatException(this.force.getFullName() +
                        " only replaces the content in the deployment repository and can't be used in combination with any of " +
                        this.disabled.getFullName() + ", " + this.serverGroups.getFullName() + " or " + this.allServerGroups.getFullName() + '.');
            }

            if(Util.isDeploymentInRepository(name, client)) {
                return buildDeploymentReplace(f, name, runtimeName);
            } else {
                // add deployment to the repository (enabled in standalone, disabled in domain (i.e. not associated with any sg))
                return buildDeploymentAdd(f, name, runtimeName);
            }
        }

        if(disabled) {
            if(f == null) {
                throw new OperationFormatException(this.disabled.getFullName() +
                        " requires a filesystem path of the deployment to be added to the deployment repository.");
            }

            if(serverGroups != null || allServerGroups) {
                throw new OperationFormatException(this.serverGroups.getFullName() + " and " + this.allServerGroups.getFullName() +
                        " can't be used in combination with " + this.disabled.getFullName() + '.');
            }

            if(Util.isDeploymentInRepository(name, client)) {
                throw new OperationFormatException("'" + name + "' already exists in the deployment repository (use " +
                    this.force.getFullName() + " to replace the existing content in the repository).");
            }

            // add deployment to the repository disabled
            return buildDeploymentAdd(f, name, runtimeName);
        }

        // actually, the deployment is added before it is deployed
        // but this code here is to validate arguments and not to add deployment if something is wrong
        final ModelNode deployRequest;
        if(ctx.isDomainMode()) {
            final List<String> sgList;
            if(allServerGroups) {
                if(serverGroups != null) {
                    throw new OperationFormatException(this.serverGroups.getFullName() + " can't appear in the same command with " + this.allServerGroups.getFullName());
                }
                sgList = Util.getServerGroups(client);
                if(sgList.isEmpty()) {
                    throw new OperationFormatException("No server group is available.");
                }
            } else if(serverGroups == null) {
                final StringBuilder buf = new StringBuilder();
                buf.append("One of ");
                if(f != null) {
                    buf.append(this.disabled.getFullName()).append(", ");
                }
                buf.append(this.allServerGroups.getFullName() + " or " + this.serverGroups.getFullName() + " is missing.");
                throw new OperationFormatException(buf.toString());
            } else {
                sgList = Arrays.asList(serverGroups.split(","));
                if(sgList.isEmpty()) {
                    throw new OperationFormatException("Couldn't locate server group name in '" + this.serverGroups.getFullName() + "=" + serverGroups + "'.");
                }
            }

            deployRequest = new ModelNode();
            deployRequest.get(Util.OPERATION).set(Util.COMPOSITE);
            deployRequest.get(Util.ADDRESS).setEmptyList();
            ModelNode steps = deployRequest.get(Util.STEPS);
            for (String serverGroup : sgList) {
                steps.add(Util.configureDeploymentOperation(Util.ADD, name, serverGroup));
            }
            for (String serverGroup : sgList) {
                steps.add(Util.configureDeploymentOperation(Util.DEPLOY, name, serverGroup));
            }
        } else {
            if(serverGroups != null || allServerGroups) {
                throw new OperationFormatException(this.serverGroups.getFullName() + " and " + this.allServerGroups.getFullName() +
                        " can't appear in standalone mode.");
            }
            deployRequest = new ModelNode();
            deployRequest.get(Util.OPERATION).set(Util.DEPLOY);
            deployRequest.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        }

        final ModelNode addRequest;
        if(f != null) {
            if(Util.isDeploymentInRepository(name, client)) {
                throw new OperationFormatException("'" + name + "' already exists in the deployment repository (use " +
                    this.force.getFullName() + " to replace the existing content in the repository).");
            }
            addRequest = this.buildDeploymentAdd(f, name, runtimeName);
        } else if(!Util.isDeploymentInRepository(name, client)) {
            throw new OperationFormatException("'" + name + "' is not found among the registered deployments.");
        } else {
            addRequest = null;
        }

        if(addRequest != null) {
            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);
            steps.add(addRequest);
            steps.add(deployRequest);
            return composite;
        }
        return deployRequest;
    }

    protected ModelNode buildDeploymentReplace(final File f, String name, String runtimeName) throws OperationFormatException {
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(Util.FULL_REPLACE_DEPLOYMENT);
        request.get(Util.NAME).set(name);
        if(runtimeName != null) {
            request.get(Util.RUNTIME_NAME).set(runtimeName);
        }

        byte[] bytes = readBytes(f);
        request.get(Util.CONTENT).get(0).get(Util.BYTES).set(bytes);
        return request;
    }

    protected ModelNode buildDeploymentAdd(final File f, String name, String runtimeName) throws OperationFormatException {
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(Util.ADD);
        request.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        if (runtimeName != null) {
            request.get(Util.RUNTIME_NAME).set(runtimeName);
        }
        byte[] bytes = readBytes(f);
        request.get(Util.CONTENT).get(0).get(Util.BYTES).set(bytes);
        return request;
    }

    protected void execute(CommandContext ctx, ModelNode request, File f) {
        ModelNode result;
        FileInputStream is = null;
        try {
            is = new FileInputStream(f);
            OperationBuilder op = new OperationBuilder(request);
            op.addInputStream(is);
            request.get(Util.CONTENT).get(0).get(Util.INPUT_STREAM_INDEX).set(0);
            result = ctx.getModelControllerClient().execute(op.build());
        } catch (Exception e) {
            ctx.error("Failed to add the deployment content to the repository: " + e.getLocalizedMessage());
            return;
        } finally {
            StreamUtils.safeClose(is);
        }
        if (!Util.isSuccess(result)) {
            ctx.error(Util.getFailureDescription(result));
            return;
        }
    }

    protected ModelNode buildAddRequest(CommandContext ctx, final File f, String name, final String runtimeName) {
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(Util.ADD);
        request.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        if (runtimeName != null) {
            request.get(Util.RUNTIME_NAME).set(runtimeName);
        }
        request.get(Util.CONTENT).get(0).get(Util.INPUT_STREAM_INDEX).set(0);
        return request;
    }

    protected void replaceDeployment(CommandContext ctx, final File f, String name, final String runtimeName) {
        // replace
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(Util.FULL_REPLACE_DEPLOYMENT);
        request.get(Util.NAME).set(name);
        if(runtimeName != null) {
            request.get(Util.RUNTIME_NAME).set(runtimeName);
        }
        request.get(Util.CONTENT).get(0).get(Util.INPUT_STREAM_INDEX).set(0);
        execute(ctx, request, f);
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
        ctx.error(table.toString());
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
}
