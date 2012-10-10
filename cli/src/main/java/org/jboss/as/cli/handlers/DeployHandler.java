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
import java.io.FileInputStream;
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
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.spi.MountHandle;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeployHandler extends DeploymentHandler {

    private final ArgumentWithoutValue force;
    private final ArgumentWithoutValue l;
    private final ArgumentWithoutValue path;
    private final ArgumentWithoutValue name;
    private final ArgumentWithoutValue rtName;
    private final ArgumentWithValue serverGroups;
    private final ArgumentWithoutValue allServerGroups;
    private final ArgumentWithoutValue disabled;
    private final ArgumentWithoutValue unmanaged;
    private final ArgumentWithValue script;

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

        serverGroups = new ArgumentWithValue(this, new CommaSeparatedCompleter() {
            @Override
            protected Collection<String> getAllCandidates(CommandContext ctx) {
                return Util.getServerGroups(ctx.getModelControllerClient());
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

        unmanaged = new ArgumentWithoutValue(this, "--unmanaged");
        unmanaged.addRequiredPreceding(path);

        script = new ArgumentWithValue(this, "--script");
        script.addRequiredPreceding(path);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final ModelControllerClient client = ctx.getModelControllerClient();

        ParsedCommandLine args = ctx.getParsedCommandLine();
        boolean l = this.l.isPresent(args);
        if (!args.hasProperties() || l) {
            listDeployments(ctx, l);
            return;
        }

        final boolean unmanaged = this.unmanaged.isPresent(args);

        final String path = this.path.getValue(args);
        final File f;
        if(path != null) {
            f = new File(path);
            if(!f.exists()) {
                throw new CommandFormatException("Path " + f.getAbsolutePath() + " doesn't exist.");
            }
            if(!unmanaged && f.isDirectory()) {
                throw new CommandFormatException(f.getAbsolutePath() + " is a directory.");
            }
        } else {
            f = null;
        }

        if (isCliArchive(f)) {
            final ModelNode request = buildRequestWOValidation(ctx);
            if(request == null) {
                throw new CommandFormatException("Operation request wasn't built.");
            }

            // if script had no server-side commands, just return
            if (!request.get("steps").isDefined()) return;

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

        String name = this.name.getValue(args);
        if(name == null) {
            if(f == null) {
                throw new CommandFormatException("Either path or --name is required.");
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
                throw new CommandFormatException(this.force.getFullName() + " requires a filesystem path of the deployment to be added to the deployment repository.");
            }
            if(disabled || serverGroups != null || allServerGroups) {
                throw new CommandFormatException(this.force.getFullName() +
                        " only replaces the content in the deployment repository and can't be used in combination with any of " +
                        this.disabled.getFullName() + ", " + this.serverGroups.getFullName() + " or " + this.allServerGroups.getFullName() + '.');
            }

            if(Util.isDeploymentInRepository(name, client)) {
                replaceDeployment(ctx, f, name, runtimeName);
                return;
            } else if(ctx.isDomainMode()) {
                // add deployment to the repository (enabled in standalone, disabled in domain (i.e. not associated with any sg))
                final ModelNode request = buildAddRequest(ctx, f, name, runtimeName, unmanaged);
                execute(ctx, request, f, unmanaged);
                return;
            }
            // standalone mode will add and deploy
        }

        if(disabled) {
            if(f == null) {
                throw new CommandFormatException(this.disabled.getFullName() + " requires a filesystem path of the deployment to be added to the deployment repository.");
            }

            if(serverGroups != null || allServerGroups) {
                throw new CommandFormatException(this.serverGroups.getFullName() + " and " + this.allServerGroups.getFullName() +
                        " can't be used in combination with " + this.disabled.getFullName() + '.');
            }

            if(Util.isDeploymentInRepository(name, client)) {
                throw new CommandFormatException("'" + name + "' already exists in the deployment repository (use " +
                this.force.getFullName() + " to replace the existing content in the repository).");
            }

            // add deployment to the repository disabled
            final ModelNode request = buildAddRequest(ctx, f, name, runtimeName, unmanaged);
            execute(ctx, request, f, unmanaged);
            return;
        }

        // actually, the deployment is added before it is deployed
        // but this code here is to validate arguments and not to add deployment if something is wrong
        final ModelNode deployRequest;
        if(ctx.isDomainMode()) {
            final List<String> sgList;
            if(allServerGroups) {
                if(serverGroups != null) {
                    throw new CommandFormatException(this.serverGroups.getFullName() + " can't appear in the same command with " + this.allServerGroups.getFullName());
                }
                sgList = Util.getServerGroups(client);
                if(sgList.isEmpty()) {
                    throw new CommandFormatException("No server group is available.");
                }
            } else if(serverGroups == null) {
                final StringBuilder buf = new StringBuilder();
                buf.append("One of ");
                if(f != null) {
                    buf.append(this.disabled.getFullName()).append(", ");
                }
                buf.append(this.allServerGroups.getFullName() + " or " + this.serverGroups.getFullName() + " is missing.");
                throw new CommandFormatException(buf.toString());
            } else {
                sgList = Arrays.asList(serverGroups.split(","));
                if(sgList.isEmpty()) {
                    throw new CommandFormatException("Couldn't locate server group name in '" + this.serverGroups.getFullName() + "=" + serverGroups + "'.");
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
                throw new CommandFormatException(this.serverGroups.getFullName() + " and " + this.allServerGroups.getFullName() +
                        " can't appear in standalone mode.");
            }
            deployRequest = new ModelNode();
            deployRequest.get(Util.OPERATION).set(Util.DEPLOY);
            deployRequest.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        }

        if(f != null) {
            if(Util.isDeploymentInRepository(name, client)) {
                throw new CommandFormatException("'" + name + "' already exists in the deployment repository (use " +
                this.force.getFullName() + " to replace the existing content in the repository).");
            }
            final ModelNode request = new ModelNode();
            request.get(Util.OPERATION).set(Util.COMPOSITE);
            request.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = request.get(Util.STEPS);
            steps.add(buildAddRequest(ctx, f, name, runtimeName, unmanaged));
            steps.add(deployRequest);
            execute(ctx, request, f, unmanaged);
            return;
        } else if(!Util.isDeploymentInRepository(name, client)) {
            throw new CommandFormatException("'" + name + "' is not found among the registered deployments.");
        }

        try {
            final ModelNode result = client.execute(deployRequest);
            if (!Util.isSuccess(result)) {
                throw new CommandFormatException(Util.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new CommandFormatException("Failed to deploy", e);
        }
    }

    @Override
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {

        final ModelControllerClient client = ctx.getModelControllerClient();

        ParsedCommandLine args = ctx.getParsedCommandLine();
        boolean l = this.l.isPresent(args);
        if (!args.hasProperties() || l) {
            throw new OperationFormatException("Command is missing arguments for non-interactive mode: '" + args.getOriginalLine() + "'.");
        }

        final boolean unmanaged = this.unmanaged.isPresent(args);

        final String path = this.path.getValue(args);
        final File f;
        if(path != null) {
            f = new File(path);
            if(!f.exists()) {
                throw new OperationFormatException("Path " + f.getAbsolutePath() + " doesn't exist.");
            }
            if(!unmanaged && f.isDirectory()) {
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
        final boolean archive = isCliArchive(f);

        if(force) {
            if(f == null) {
                throw new OperationFormatException(this.force.getFullName() + " requires a filesystem path of the deployment to be added to the deployment repository.");
            }
            if(disabled || serverGroups != null || allServerGroups) {
                throw new OperationFormatException(this.force.getFullName() +
                        " only replaces the content in the deployment repository and can't be used in combination with any of " +
                        this.disabled.getFullName() + ", " + this.serverGroups.getFullName() + " or " + this.allServerGroups.getFullName() + '.');
            }
            if (archive) {
                throw new OperationFormatException(this.force.getFullName() + " can't be used in combination with a CLI archive.");
            }

            if(Util.isDeploymentInRepository(name, client)) {
                // in batch mode these kind of checks might be inaccurate
                // because the previous commands and operations are not taken into account
                return buildDeploymentReplace(f, name, runtimeName);
            } else {
                // add deployment to the repository (enabled in standalone, disabled in domain (i.e. not associated with any sg))
                return buildDeploymentAdd(f, name, runtimeName, unmanaged);
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

            if (archive) {
                throw new OperationFormatException(this.disabled.getFullName() + " can't be used in combination with a CLI archive.");
            }

            if(!ctx.isBatchMode() && Util.isDeploymentInRepository(name, client)) {
                throw new OperationFormatException("'" + name + "' already exists in the deployment repository (use " +
                    this.force.getFullName() + " to replace the existing content in the repository).");
            }

            // add deployment to the repository disabled
            return buildDeploymentAdd(f, name, runtimeName, unmanaged);
        }

        if (archive) {
            if(serverGroups != null || allServerGroups) {
                throw new OperationFormatException(this.serverGroups.getFullName() + " and " + this.allServerGroups.getFullName() +
                        " can't be used in combination with a CLI archive.");
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
                    script = "deploy.scr";
                }

                File scriptFile = new File(ctx.getCurrentDir(),script);
                if (!scriptFile.exists()) {
                    throw new CommandFormatException("ERROR: script '" + script + "' not found.");
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
                    throw new CommandFormatException("ERROR: script '" + script + "' not found.");
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
            if(!ctx.isBatchMode() && Util.isDeploymentInRepository(name, client)) {
                throw new OperationFormatException("'" + name + "' already exists in the deployment repository (use " +
                    this.force.getFullName() + " to replace the existing content in the repository).");
            }
            addRequest = this.buildDeploymentAdd(f, name, runtimeName, unmanaged);
        } else if(!ctx.isBatchMode() && !Util.isDeploymentInRepository(name, client)) {
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

    protected ModelNode buildDeploymentAdd(final File f, String name, String runtimeName, boolean unmanaged) throws OperationFormatException {
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(Util.ADD);
        request.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        if (runtimeName != null) {
            request.get(Util.RUNTIME_NAME).set(runtimeName);
        }
        if(unmanaged) {
            final ModelNode content = request.get(Util.CONTENT).get(0);
            content.get(Util.PATH).set(f.getAbsolutePath());
            content.get(Util.ARCHIVE).set(f.isFile());
        } else {
            byte[] bytes = readBytes(f);
            request.get(Util.CONTENT).get(0).get(Util.BYTES).set(bytes);
        }
        return request;
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
    }

    protected ModelNode buildAddRequest(CommandContext ctx, final File f, String name, final String runtimeName, boolean unmanaged) {
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(Util.ADD);
        request.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        if (runtimeName != null) {
            request.get(Util.RUNTIME_NAME).set(runtimeName);
        }
        if(unmanaged) {
            final ModelNode content = request.get(Util.CONTENT).get(0);
            content.get(Util.PATH).set(f.getAbsolutePath());
            content.get(Util.ARCHIVE).set(f.isFile());
        } else {
            request.get(Util.CONTENT).get(0).get(Util.INPUT_STREAM_INDEX).set(0);
        }
        return request;
    }

    protected void replaceDeployment(CommandContext ctx, final File f, String name, final String runtimeName) throws CommandFormatException {
        // replace
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(Util.FULL_REPLACE_DEPLOYMENT);
        request.get(Util.NAME).set(name);
        if(runtimeName != null) {
            request.get(Util.RUNTIME_NAME).set(runtimeName);
        }
        request.get(Util.CONTENT).get(0).get(Util.INPUT_STREAM_INDEX).set(0);
        execute(ctx, request, f, false);
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
