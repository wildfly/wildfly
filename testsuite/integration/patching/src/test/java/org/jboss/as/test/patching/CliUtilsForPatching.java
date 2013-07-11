/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.patching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.dmr.ModelNode;

/**
 * @author Jan Martiska
 */
public class CliUtilsForPatching {
    public static final String OVERRIDE_ALL = "--override-all";
    public static final String OVERRIDE_MODULES = "--override-modules";
    public static final String OVERRIDE = "--override=%s";
    public static final String PRESERVE = "--preserve=%s";
    public static final String KEEP_CONFIGURATION = "--keep-configuration";
    public static final String ROLLBACK_TO = "--rollback-to";

    /**
     * Use the CLI to apply a patch
     *
     * @param patchFilePath absolute path to the ZIP file containing the patch
     * @throws Exception
     */
    public static void applyPatch(String patchFilePath) throws Exception {
        applyPatch(patchFilePath, null);
    }


    /**
     * Use the CLI to apply a patch
     *
     * @param patchFilePath absolute path to the ZIP file containing the patch
     * @param args          conflict resolution arguments or null
     * @throws Exception
     */
    public static void applyPatch(String patchFilePath, String... args) throws Exception {
        CLIWrapper cli = new CLIWrapper(true);
        StringBuilder builder = new StringBuilder("patch apply");
        if (args != null) {
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
        }
        builder.append(" ").append(patchFilePath);
        String command = builder.toString();
        cli.sendLine(command);
        cli.quit();
    }

    /**
     * Use the CLI to rollback a patch
     *
     * @param oneOffPatchID the ID of the patch that should be rolled back
     * @throws Exception
     */
    public static void rollbackPatch(String oneOffPatchID) throws Exception {
        rollbackPatch(oneOffPatchID, null);
    }

    /**
     * Use the CLI to rollback a patch
     *
     * @param oneoffPatchID the ID of the patch that should be rolled back
     * @param args          conflict resolution arguments, rollback arguments
     * @throws Exception
     */
    public static void rollbackPatch(String oneoffPatchID, String... args ) throws Exception {
        CLIWrapper cli = new CLIWrapper(true);
        StringBuilder builder = new StringBuilder("patch rollback");
        if (args != null) {
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
        }
        builder.append(" --patch-id=").append(oneoffPatchID);
        String command = builder.toString();
        cli.sendLine(command);
        cli.quit();
    }

    /**
     * Use the CLI to read information about the installed patches
     *
     * @return output of "patch info" command or null if output is empty
     * @throws Exception
     */
    public static ModelNode info() throws Exception {
        CLIWrapper cli = new CLIWrapper(true);
        String command = "patch info";
        cli.sendLine(command);
        String output = cli.readOutput();
        cli.quit();
        return ModelNode.fromJSONString(output);
    }


    /**
     * Use CLI to get the list of currently installed patches
     * @return the currently installed patches as a collection of patch IDs (strings)
     * @throws Exception
     */
    public static Collection<String> getInstalledPatches() throws Exception {
        CLIWrapper cli = new CLIWrapper(true);
        cli.sendLine("patch info");
        String response = cli.readOutput();
        ModelNode responseNode = ModelNode.fromJSONString(response);
        List<ModelNode> patchesList = responseNode.get("result").get("patches").asList();
        List<String> patchesListString = new ArrayList<String>();
        for(ModelNode n : patchesList) {
            patchesListString.add(n.asString());
        }
        cli.quit();
        return patchesListString;
    }

}