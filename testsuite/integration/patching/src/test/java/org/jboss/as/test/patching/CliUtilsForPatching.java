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

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jboss.as.patching.Constants.BASE;

/**
 * @author Jan Martiska
 */
public class CliUtilsForPatching {

    private static final Logger logger = Logger.getLogger(CliUtilsForPatching.class);

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
     * @param args          conflict resolution arguments or null
     * @throws Exception
     */
    public static boolean applyPatch(String patchFilePath, String... args) throws Exception {
        CLIWrapper cli = null;
        try {
            cli = new CLIWrapper(true);

            StringBuilder builder = new StringBuilder("patch apply");
            if (args != null) {
                for (String arg : args) {
                    builder.append(" ").append(arg);
                }
            }
            builder.append(" ").append(patchFilePath);
            String command = builder.toString();
            logger.info("----- sending command to CLI: " + command + " -----");
            return cli.sendLine(command, true);
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }
    }

    /**
     * Use the CLI to rollback a patch
     *
     * @param oneoffPatchID the ID of the patch that should be rolled back
     * @param args          conflict resolution arguments, rollback arguments
     * @throws Exception
     */
    public static boolean rollbackPatch(String oneoffPatchID, String... args) throws Exception {
        CLIWrapper cli = null;
        try {
            cli = new CLIWrapper(true);
            StringBuilder builder = new StringBuilder("patch rollback --reset-configuration=false");
            if (args != null) {
                for (String arg : args) {
                    builder.append(" ").append(arg);
                }
            }
            builder.append(" --patch-id=").append(oneoffPatchID);
            String command = builder.toString();
            logger.info("----- sending command to CLI: " + command + " -----");
            return cli.sendLine(command, true);
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }

    }

    /**
     * Use the CLI to read information about the installed patches
     *
     * @return output of "patch info" command or null if output is empty
     * @throws Exception
     */
    public static ModelNode info() throws Exception {
        return info(true);
    }

    /**
     * Use the CLI to read information about the installed patches
     *
     * @param connect to the server
     * @return output of "patch info" command or null if output is empty
     * @throws Exception
     */
    public static ModelNode info(boolean connect) throws Exception {
        CLIWrapper cli = null;
        try {
            cli = new CLIWrapper(connect);
            String command = "patch info";
            logger.info("----- sending command to CLI: " + command + " -----");
            cli.sendLine(command);
            String output = cli.readOutput();
            return ModelNode.fromJSONString(output);
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }

    }


    /**
     * Use CLI to get the list of currently installed patches
     *
     * @return the currently installed patches as a collection of patch IDs (strings)
     * @throws Exception
     */
    public static Collection<String> getInstalledPatches() throws Exception {
        CLIWrapper cli = null;
        try {
            cli = new CLIWrapper(true);
            String command = "patch info";
            logger.info("----- sending command to CLI: " + command + " -----");
            cli.sendLine(command);
            String response = cli.readOutput();
            ModelNode responseNode = ModelNode.fromJSONString(response);
            List<ModelNode> patchesList = responseNode.get("result").get("patches").asList();
            List<String> patchesListString = new ArrayList<String>();
            for (ModelNode n : patchesList) {
                patchesListString.add(n.asString());
            }
            return patchesListString;
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }
    }

    /**
     * Use CLI to get the cumulative patch id
     *
     * @return the current cumulative patch id (string)
     * @throws Exception
     */
    public static String getCumulativePatchId() throws Exception {
        CLIWrapper cli = null;
        try {
            cli = new CLIWrapper(true);
            cli.sendLine("patch info");
            String response = cli.readOutput();
            ModelNode responseNode = ModelNode.fromJSONString(response);
            return responseNode.get("result").get("cumulative-patch-id").asString();
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }
    }

    /**
     * Check if the server is in restart-required state, that means
     * management operation return "response-headers" : {"process-state" : "restart-required"}
     *
     * @return true if the server is in "restart-required" state
     * @throws Exception
     */
    public static boolean doesServerRequireRestart() throws Exception {
        CLIWrapper cli = null;
        try {
            cli = new CLIWrapper(true);
            cli.sendLine("patch info", true);
            String response = cli.readOutput();
            ModelNode responseNode = ModelNode.fromJSONString(response);
            ModelNode respHeaders = responseNode.get("response-headers");
            if (respHeaders != null && respHeaders.isDefined()) {
                ModelNode processState = respHeaders.get("process-state");
                return processState != null && processState.isDefined() && processState.asString()
                        .equals(ClientConstants.CONTROLLER_PROCESS_STATE_RESTART_REQUIRED);
            } else {
                return false;
            }
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }
    }

    /**
     * Rollback all installed oneoffs, offline!!
     *
     * @return true if operation was successful or false if at least one rollback failed
     * @throws Exception
     */
    public static boolean rollbackAllOneOffs() throws Exception {
        CLIWrapper cli = null;
        boolean success = true;
        final String infoCommand = "patch info --distribution=%s";
        final String rollbackCommand = "patch rollback --patch-id=%s --distribution=%s --reset-configuration=true --override-all";
        try {
            cli = new CLIWrapper(false);
            String command = String.format(infoCommand, PatchingTestUtil.AS_DISTRIBUTION);
            logger.info("----- sending command to CLI: " + command + " -----");
            cli.sendLine(command);
            String response = cli.readOutput();
            ModelNode responseNode = ModelNode.fromJSONString(response);
            List<ModelNode> patchesList = responseNode.get("result").get("patches").asList();
            for (ModelNode n : patchesList) {
                success = success && cli.sendLine(String.format(rollbackCommand, n.asString(), PatchingTestUtil.AS_DISTRIBUTION), true);
            }
            return success;
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }
    }

    /**
     * Rollback cumulative patch, online!!
     *
     * @return true if operation was successful or false if at least one rollback failed
     * @param resetConfiguration
     * @throws Exception
     */
    public static boolean rollbackCumulativePatch(boolean resetConfiguration) throws Exception {
        CLIWrapper cli = null;
        final String infoCommand = "patch info --distribution=%s";
        final String rollbackCommand = "patch rollback --patch-id=%s --distribution=%s --reset-configuration=%s";
        try {
            cli = new CLIWrapper(true);
            String command = String.format(infoCommand, PatchingTestUtil.AS_DISTRIBUTION);
            logger.info("----- sending command to CLI: " + command + " -----");
            cli.sendLine(command);
            String response = cli.readOutput();
            ModelNode responseNode = ModelNode.fromJSONString(response);
            String cumulativePatchId = responseNode.get("result").get("cumulative-patch-id").asString();
            return cli.sendLine(String.format(rollbackCommand, cumulativePatchId, PatchingTestUtil.AS_DISTRIBUTION, resetConfiguration), true);
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }
    }

    /**
     * Rollback cumulative patch and all picked up one-offs, offline
     *
     * @return true if operation was successful or false if at least one rollback failed
     * @throws Exception
     */
    public static boolean rollbackAll() throws Exception {
        CLIWrapper cli = null;
        boolean success = true;
        final String infoCommand = "patch info --distribution=%s";
        final String rollbackCommand = "patch rollback --patch-id=%s --distribution=%s --reset-configuration=true";
        try {
            cli = new CLIWrapper(false);
            String command = String.format(infoCommand, PatchingTestUtil.AS_DISTRIBUTION);
            logger.info("----- sending command to CLI: " + command + " -----");
            cli.sendLine(command);
            String response = cli.readOutput();
            ModelNode responseNode = ModelNode.fromJSONString(response);
            String cumulativePatchId = responseNode.get("result").get("cumulative-patch-id").asString();
            if(!cumulativePatchId.equalsIgnoreCase(BASE)) {
                success = success && cli.sendLine(String.format(rollbackCommand, cumulativePatchId, PatchingTestUtil.AS_DISTRIBUTION), true);
            }
            cli.sendLine(String.format(infoCommand, PatchingTestUtil.AS_DISTRIBUTION));
            response = cli.readOutput();
            responseNode = ModelNode.fromJSONString(response);
            List<ModelNode> patchesList = responseNode.get("result").get("patches").asList();
            for (ModelNode n : patchesList) {
                success = success && cli.sendLine(String.format(rollbackCommand, n.asString(), PatchingTestUtil.AS_DISTRIBUTION), true);
            }
            return success;
        } finally {
            if (cli != null) {
                cli.quit();
            }
        }
    }

}