/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.subsystem;

import static org.junit.Assert.assertTrue;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.logging.Logger;

/**
 * Setup task which removes <code>/subsystem=security-manager/deployment-permissions=default</code> node from the domain model.
 * The {@link #tearDown(ManagementClient, String)} method restores the node with single attribute configured:<br/>
 * <code>maximum-permissions=[{class=java.security.AllPermission}])</code>.
 *
 * @author Josef Cacek
 */
public class RemoveDeploymentPermissionsServerSetupTask implements ServerSetupTask {

    private static CLIWrapper cli;

    private static Logger LOGGER = Logger.getLogger(RemoveDeploymentPermissionsServerSetupTask.class);

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     * java.lang.String)
     */
    @Override
    public final void setup(final ManagementClient managementClient, String containerId) throws Exception {
        if (cli == null) {
            cli = new CLIWrapper(true);
        }
        CLIOpResult result = null;

        // remove the deployment permissions
        cli.sendLine("/subsystem=security-manager/deployment-permissions=default:remove()");
        result = cli.readAllAsOpResult();
        assertTrue("Removing deployment-permissions by using management API failed", result.isIsOutcomeSuccess());

        // reload the server
        LOGGER.debug("Reloading the server");
        reload(managementClient);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     * java.lang.String)
     */
    @Override
    public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
        CLIOpResult result = null;

        // remove the deployment permissions configuration if exists
        cli.sendLine("/subsystem=security-manager/deployment-permissions=default:remove()", true);
        result = cli.readAllAsOpResult();
        LOGGER.debug("Just in case. We tried to remove deployment-permissions before adding it. Result of the delete: "
                + result.getFromResponse(ModelDescriptionConstants.OUTCOME));

        // revert original deployment permissions
        cli.sendLine(
                "/subsystem=security-manager/deployment-permissions=default:add(maximum-permissions=[{class=java.security.AllPermission}])");
        result = cli.readAllAsOpResult();
        assertTrue("Reverting maximum-permissions by using management API failed", result.isIsOutcomeSuccess());

        // reload the server
        LOGGER.debug("Reloading the server");
        reload(managementClient);

    }

    /**
     * Provide reload operation on the server
     */
    private static void reload(final ManagementClient managementClient) {
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

}
