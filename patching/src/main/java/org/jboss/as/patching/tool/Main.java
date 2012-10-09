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

package org.jboss.as.patching.tool;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.runner.ContentVerificationPolicy;
import org.jboss.as.patching.runner.PatchingResult;
import org.jboss.as.version.ProductConfig;
import org.jboss.modules.ModuleLoader;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * java -Dboot.module.loader=org.jboss.as.boot.BootModuleLoader -cp jboss-modules.jar:loader.jar org.jboss.modules.Main -mp modules org.jboss.as.patching.tool
 *
 * @author Emanuel Muckenhuber
 */
public class Main {

    private static final PatchLogger log = PatchLogger.ROOT_LOGGER;

    static enum Argument {

        GC("gc"),
        PATCH("patch"),
        ROLLBACK("rollback"),
        OVERRIDE_ALL("--override-all"),

        UNKNOWN(null),
        ;

        private final String operation;
        Argument(String operation) {
            this.operation = operation;
        }

        static final Map<String, Argument> operations = new HashMap<String, Argument>();
        static {
            for(final Argument operation : Argument.values()) {
                if(operation.operation != null) {
                    operations.put(operation.operation, operation);
                }
            }
        }

        static Argument forName(String arg) {
            final Argument argument = operations.get(arg);
            return argument == null ? UNKNOWN : argument;
        }

    }


    public static void main(final String[] args) throws Exception {

        Argument operation = null;
        String param = null;
        boolean overrideAll = false;

        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            final Argument argument = Argument.forName(arg);
            switch (argument) {
                // process operation
                case GC:
                case PATCH:
                case ROLLBACK:
                    param = args[++i];
                    operation = argument;
                    break;
                case OVERRIDE_ALL:
                    overrideAll = true;
                    break;
                default:
                    System.out.println("Illegal argument: " + arg);
                    printUsage(System.out);
                    return;
            }
        }
        if(operation == null || param == null) {
            printUsage(System.out);
            return;
        }


        final String home = System.getProperty("jboss.home.dir", System.getenv("JBOSS_HOME"));
        final File jbossHome = home == null ? new File(".") : new File(home);
        final DirectoryStructure structure = DirectoryStructure.createDefault(jbossHome.getAbsoluteFile());
        final ModuleLoader loader = ModuleLoader.forClass(Main.class);
        final ProductConfig config = new ProductConfig(loader, jbossHome.getAbsolutePath());
        final PatchInfo info = LocalPatchInfo.load(config, structure);

        // Debug information
        debug(info, structure);

        final PatchTool tool = new PatchTool(info, structure);
        final PatchingResult result;
        // Rollback
        if(operation == Argument.ROLLBACK) {
            result = tool.rollback(param, overrideAll);
        // Apply patch
        } else if (operation == Argument.PATCH) {
            final File file = new File(param);
            assert file.exists();
            final ContentVerificationPolicy policy = overrideAll ? ContentVerificationPolicy.OVERRIDE_ALL : ContentVerificationPolicy.STRICT;
            result = tool.applyPatch(file, policy);
        } else {
            // TODO
            return;
        }
        if(result.hasFailures()) {
            log.errorf("Failed to complete operation for patch: \"%s\"", result.getPatchId());
            final Collection<ContentItem> problems = result.getProblems();
            if(problems != null && ! problems.isEmpty()) {
                log.errorf("Conflicting items are:");
                for(final ContentItem item : result.getProblems()) {
                    log.error(item);
                }
                log.infof("Run with '--override-all' to force overriding all content conflicts.");
            }
        } else {
            log.infof("Operation completed successfully: %s", result.getPatchId());
        }
    }

    static void printUsage(final PrintStream stream) {
        stream.println("Usage: org.jboss.as.patching.tool <command> [<args>]");
        stream.println();
        stream.println("The available commands are:");
        stream.println("   patch     <file> [<--override-all>]");
        stream.println("   rollback  <patch-id> [<--override-all>]");
        stream.println("   gc        [<patch-id>]");
        stream.println();
    }

    static void debug(final PatchInfo info, final DirectoryStructure structure) {
        log.debugf("Patching environment, Version ", info.getVersion());
        log.debugf("CP:   (%s), one-off patches %s", info.getCumulativeID(), info.getPatchIDs());
        log.debugf("Root (%s)", structure.getInstalledImage().getJbossHome().getAbsolutePath());
    }

}
