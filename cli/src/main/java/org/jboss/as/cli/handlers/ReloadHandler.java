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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.CLIModelControllerClient;
import org.jboss.as.cli.impl.CommaSeparatedCompleter;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * @author Alexey Loubyansky
 *
 */
public class ReloadHandler extends BaseOperationCommand {

    private final ArgumentWithValue adminOnly;
    // standalone only arguments
    private final ArgumentWithValue useCurrentServerConfig;
    // domain only arguments
    private final ArgumentWithValue host;
    private final ArgumentWithValue restartServers;
    private final ArgumentWithValue useCurrentDomainConfig;
    private final ArgumentWithValue useCurrentHostConfig;

    public ReloadHandler(CommandContext ctx) {
        super(ctx, "reload", true);

        adminOnly = new ArgumentWithValue(this, SimpleTabCompleter.BOOLEAN, "--admin-only");

        useCurrentServerConfig = new ArgumentWithValue(this, SimpleTabCompleter.BOOLEAN, "--use-current-server-config"){
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
            if(ctx.isDomainMode()) {
                return false;
            }
            return super.canAppearNext(ctx);
        }};

        restartServers = new ArgumentWithValue(this, SimpleTabCompleter.BOOLEAN, "--restart-servers"){
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
            if(!ctx.isDomainMode()) {
                return false;
            }
            return super.canAppearNext(ctx);
        }};

        useCurrentDomainConfig = new ArgumentWithValue(this, SimpleTabCompleter.BOOLEAN, "--use-current-domain-config"){
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
            if(!ctx.isDomainMode()) {
                return false;
            }
            return super.canAppearNext(ctx);
        }};

        useCurrentHostConfig = new ArgumentWithValue(this, SimpleTabCompleter.BOOLEAN, "--use-current-host-config"){
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
            if(!ctx.isDomainMode()) {
                return false;
            }
            return super.canAppearNext(ctx);
        }};

        host = new ArgumentWithValue(this, new CommaSeparatedCompleter() {
            @Override
            protected Collection<String> getAllCandidates(CommandContext ctx) {
                if(!ctx.isDomainMode()) {
                    return Collections.emptyList();
                }
                final ModelControllerClient client = ctx.getModelControllerClient();
                if(client == null) {
                    return Collections.emptyList();
                }
                final ModelNode op = new ModelNode();
                op.get(Util.ADDRESS).setEmptyList();
                op.get(Util.OPERATION).set(Util.READ_CHILDREN_NAMES);
                op.get(Util.CHILD_TYPE).set(Util.HOST);
                try {
                    ModelNode outcome = client.execute(op);
                    if (Util.isSuccess(outcome)) {
                        return Util.getList(outcome);
                    }
                } catch (Exception e) {
                }
                return Collections.emptyList();
            }} , "--host") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {
        final ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            throw new CommandLineException("Connection is now available.");
        }
        if(!(client instanceof CLIModelControllerClient)) {
            throw new CommandLineException("Unsupported ModelControllerClient implementation " + client.getClass().getName());
        }
        final CLIModelControllerClient cliClient = (CLIModelControllerClient) client;


        final ModelNode op = this.buildRequestWithoutHeaders(ctx);
        final ModelNode response;
        try {
            response = cliClient.execute(op, true);
        } catch(IOException e) {
            ctx.disconnectController();
            throw new CommandLineException("Failed to execute :reload", e);
        }

        if(Util.isSuccess(response)) {
            //ctx.disconnectController(); it'll automatically try to re-connect for the next command/operation
        } else {
            throw new CommandLineException(Util.getFailureDescription(response));
        }
    }

    @Override
    protected ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {
        final ParsedCommandLine args = ctx.getParsedCommandLine();

        final ModelNode op = new ModelNode();
        if(ctx.isDomainMode()) {
            if(useCurrentServerConfig.isPresent(args)) {
                throw new CommandFormatException(useCurrentServerConfig.getFullName() + " is not allowed in the domain mode.");
            }

            final String hostName = host.getValue(args);
            if(hostName == null) {
                throw new CommandFormatException("Missing required argument " + host.getFullName());
            }
            op.get(Util.ADDRESS).add(Util.HOST, hostName);

            setBooleanArgument(args, op, restartServers, "restart-servers");
            setBooleanArgument(args, op, this.useCurrentDomainConfig, "use-current-domain-config");
            setBooleanArgument(args, op, this.useCurrentHostConfig, "use-current-host-config");
        } else {
            if(host.isPresent(args)) {
                throw new CommandFormatException(host.getFullName() + " is not allowed in the standalone mode.");
            }
            if(useCurrentDomainConfig.isPresent(args)) {
                throw new CommandFormatException(useCurrentDomainConfig.getFullName() + " is not allowed in the standalone mode.");
            }
            if(useCurrentHostConfig.isPresent(args)) {
                throw new CommandFormatException(useCurrentHostConfig.getFullName() + " is not allowed in the standalone mode.");
            }
            if(restartServers.isPresent(args)) {
                throw new CommandFormatException(restartServers.getFullName() + " is not allowed in the standalone mode.");
            }

            op.get(Util.ADDRESS).setEmptyList();
            setBooleanArgument(args, op, this.useCurrentServerConfig, "use-current-server-config");
        }
        op.get(Util.OPERATION).set("reload");

        setBooleanArgument(args, op, adminOnly, "admin-only");
        return op;
    }

    protected void setBooleanArgument(final ParsedCommandLine args, final ModelNode op, ArgumentWithValue arg, String paramName)
            throws CommandFormatException {
        if(!arg.isPresent(args)) {
            return;
        }
        final String value = arg.getValue(args);
        if(value == null) {
            throw new CommandFormatException(arg.getFullName() + " is missing value.");
        }
        if(value.equalsIgnoreCase(Util.TRUE)) {
            op.get(paramName).set(true);
        } else if(value.equalsIgnoreCase(Util.FALSE)) {
            op.get(paramName).set(false);
        } else {
            throw new CommandFormatException("Invalid value for " + arg.getFullName() + ": '" + value + "'");
        }
    }
}
