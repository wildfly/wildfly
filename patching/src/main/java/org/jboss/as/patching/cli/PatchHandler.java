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

package org.jboss.as.patching.cli;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.handlers.DefaultFilenameTabCompleter;
import org.jboss.as.cli.handlers.FilenameTabCompleter;
import org.jboss.as.cli.handlers.SimpleTabCompleter;
import org.jboss.as.cli.handlers.WindowsFilenameTabCompleter;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.FileSystemPathArgument;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.tool.PatchOperationBuilder;
import org.jboss.as.patching.tool.PatchOperationTarget;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class PatchHandler extends CommandHandlerWithHelp {

    static final String PATCH = "patch";
    static final String APPLY = "apply";
    static final String ROLLBACK = "rollback";
    static final String INFO = "info";

    private final ArgumentWithValue host;

    private final ArgumentWithValue action;

    private final ArgumentWithoutValue path;

    private final ArgumentWithValue patchId;
    private final ArgumentWithoutValue rollbackTo;
    private final ArgumentWithValue resetConfiguration;

    private final ArgumentWithoutValue overrideModules;
    private final ArgumentWithoutValue overrideAll;
    private final ArgumentWithValue override;
    private final ArgumentWithValue preserve;

    private final ArgumentWithoutValue distribution;

    public PatchHandler(final CommandContext context) {
        super(PATCH, false);

        action = new ArgumentWithValue(this, new SimpleTabCompleter(new String[]{APPLY, ROLLBACK, INFO}), 0, "--action");

        host = new ArgumentWithValue(this, new DefaultCompleter(CandidatesProviders.HOSTS), "--host") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                boolean connected = ctx.getControllerHost() != null;
                return connected && ctx.isDomainMode() && super.canAppearNext(ctx);
            }
        };

        // apply & rollback arguments

        overrideModules = new ArgumentWithoutValue(this, "--override-modules") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (canOnlyAppearAfterActions(ctx, APPLY, ROLLBACK)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        overrideModules.addRequiredPreceding(action);

        overrideAll = new ArgumentWithoutValue(this, "--override-all") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (canOnlyAppearAfterActions(ctx, APPLY, ROLLBACK)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        overrideAll.addRequiredPreceding(action);

        override = new ArgumentWithValue(this, "--override") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (canOnlyAppearAfterActions(ctx, APPLY, ROLLBACK)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        override.addRequiredPreceding(action);

        preserve = new ArgumentWithValue(this, "--preserve") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (canOnlyAppearAfterActions(ctx, APPLY, ROLLBACK)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        preserve.addRequiredPreceding(action);

        // apply arguments

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(context) : new DefaultFilenameTabCompleter(context);
        path = new FileSystemPathArgument(this, pathCompleter, 1, "--path") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (canOnlyAppearAfterActions(ctx, APPLY)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }

        };
        path.addRequiredPreceding(action);

        // rollback arguments

        patchId = new ArgumentWithValue(this, "--patch-id") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (canOnlyAppearAfterActions(ctx, ROLLBACK)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        patchId.addRequiredPreceding(action);
        rollbackTo = new ArgumentWithoutValue(this, "--rollback-to") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (canOnlyAppearAfterActions(ctx, ROLLBACK)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        rollbackTo.addRequiredPreceding(action);
        resetConfiguration = new ArgumentWithValue(this, SimpleTabCompleter.BOOLEAN, "--reset-configuration") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (canOnlyAppearAfterActions(ctx, ROLLBACK)) {
                    return super.canAppearNext(ctx);
                }
                return false;
            }
        };
        resetConfiguration.addRequiredPreceding(action);

        distribution = new FileSystemPathArgument(this, pathCompleter, "--distribution") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                // TODO this is hidden from the tab-completion for now (and also not documented),
                // although if the argument name is typed in and followed with the '=',
                // the tab-completion for its value will work
                return false;
            }
        };
    }

    private boolean canOnlyAppearAfterActions(CommandContext ctx, String... actions) {
        final String actionStr = this.action.getValue(ctx.getParsedCommandLine());
        if(actionStr == null || actions.length == 0) {
            return false;
        }
        return Arrays.asList(actions).contains(actionStr);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {
        final PatchOperationTarget target = createPatchOperationTarget(ctx);
        final PatchOperationBuilder builder = createPatchOperationBuilder(ctx.getParsedCommandLine());
        final ModelNode result;
        try {
            result = builder.execute(target);
        } catch (Exception e) {
            throw new CommandLineException("Unable to apply patch", e);
        }
        if (!Util.isSuccess(result)) {
            final ModelNode fd = result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION);
            if(!fd.isDefined()) {
                throw new CommandLineException("Failed to apply patch: " + result.asString());
            }
            if(fd.has(Constants.CONFLICTS)) {
                final StringBuilder buf = new StringBuilder();
                buf.append(fd.get(Constants.MESSAGE).asString()).append(": ");
                final ModelNode conflicts = fd.get(Constants.CONFLICTS);
                String title = "";
                if(conflicts.has(Constants.BUNDLES)) {
                    formatConflictsList(buf, conflicts, "", Constants.BUNDLES);
                    title = ", ";
                }
                if(conflicts.has(Constants.MODULES)) {
                    formatConflictsList(buf, conflicts, title, Constants.MODULES);
                    title = ", ";
                }
                if(conflicts.has(Constants.MISC)) {
                    formatConflictsList(buf, conflicts, title, Constants.MISC);
                }
                throw new CommandLineException(buf.toString());
            } else {
                throw new CommandLineException(Util.getFailureDescription(result));
            }
        }
        ctx.printLine(result.toJSONString(false));
    }

    protected void formatConflictsList(final StringBuilder buf, final ModelNode conflicts, String title, String contentType) {
        buf.append(title);
        final List<ModelNode> list = conflicts.get(contentType).asList();
        int i = 0;
        while(i < list.size()) {
            final ModelNode item = list.get(i++);
            buf.append(item.asString());
            if(i < list.size()) {
                buf.append(", ");
            }
        }
    }

    private PatchOperationBuilder createPatchOperationBuilder(ParsedCommandLine args) throws CommandFormatException {
        final String action = this.action.getValue(args, true);

        PatchOperationBuilder builder;
        if (APPLY.equals(action)) {
            final String path = this.path.getValue(args, true);

            final File f = new File(path);
            if(!f.exists()) {
                // i18n?
                throw new CommandFormatException("Path " + f.getAbsolutePath() + " doesn't exist.");
            }
            if(f.isDirectory()) {
                throw new CommandFormatException(f.getAbsolutePath() + " is a directory.");
            }
            builder = PatchOperationBuilder.Factory.patch(f);
        } else if (ROLLBACK.equals(action)) {
            String resetConfigValue = resetConfiguration.getValue(args, true);
            boolean resetConfig;
            if(Util.TRUE.equalsIgnoreCase(resetConfigValue)) {
                resetConfig = true;
            } else if(Util.FALSE.equalsIgnoreCase(resetConfigValue)) {
                resetConfig = false;
            } else {
                throw new CommandFormatException("Unexpected value for --reset-configuration (only true and false are allowed): " + resetConfigValue);
            }
            if(patchId.isPresent(args)) {
                final String id = patchId.getValue(args, true);
                final boolean rollbackTo = this.rollbackTo.isPresent(args);
                builder = PatchOperationBuilder.Factory.rollback(id, rollbackTo, resetConfig);
            } else {
                builder = PatchOperationBuilder.Factory.rollbackLast(resetConfig);
            }
        } else {
            builder = PatchOperationBuilder.Factory.info();
            return builder;
        }
        if (overrideModules.isPresent(args)) {
            builder.ignoreModuleChanges();
        }
        if (overrideAll.isPresent(args)) {
            builder.overrideAll();
        }
        if (override.isPresent(args)) {
            String overrideList = override.getValue(args);
            for (String path : overrideList.split(",+")) {
                builder.overrideItem(path);
            }
        }
        if (preserve.isPresent(args)) {
            String overrideList = preserve.getValue(args);
            for (String path : overrideList.split(",+")) {
                builder.preserveItem(path);
            }
        }
        return builder;
    }

    private PatchOperationTarget createPatchOperationTarget(CommandContext ctx) throws CommandLineException {
        final PatchOperationTarget target;
        boolean connected = ctx.getControllerHost() != null;
        if (connected) {
            if (ctx.isDomainMode()) {
                String hostName = host.getValue(ctx.getParsedCommandLine(), true);
                target = PatchOperationTarget.createHost(hostName, ctx.getModelControllerClient());
            } else {
                target = PatchOperationTarget.createStandalone(ctx.getModelControllerClient());
            }
        } else {
            final String jbossHome = getJBossHome(ctx.getParsedCommandLine());
            try {
                // TODO add separate params for modules and bundle directories
                final File root = new File(jbossHome);
                final File modules = new File(root, "modules");
                final File bundles = new File(root, "bundles");
                target = PatchOperationTarget.createLocal(root, Collections.singletonList(modules), Collections.singletonList(bundles));
            } catch (Exception e) {
                throw new CommandLineException("Unable to apply patch to local JBOSS_HOME=" + jbossHome, e);
            }
        }
        return target;
    }

    private String getJBossHome(ParsedCommandLine args) {
        final String targetDistro = distribution.getValue(args);
        if(targetDistro != null) {
            return targetDistro;
        }
        final String env = "JBOSS_HOME";
        String resolved;
        if (System.getSecurityManager() == null) {
            resolved = System.getenv(env);
        } else {
            resolved = (String) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    return System.getenv(env);
                }
            });
        }
        if (resolved == null) {
            if (System.getSecurityManager() == null) {
                resolved = System.getProperty("jboss.home.dir");
            } else {
                resolved = (String) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        return System.getProperty("jboss.home.dir");
                    }
                });
            }
        }
        if (resolved == null) {
            throw new RuntimeException("failed to resolve the home.dir use the --distribution attribute to point to the home.dir");
        }
        // TODO proper check
        final File home = new File(resolved);
        final File modules = new File(home, "modules");
        if (! modules.isDirectory()) {
            throw new RuntimeException("failed to resolve the home.dir use the --distribution attribute to point to the home.dir: " + resolved);
        }
        return resolved;
    }
}
