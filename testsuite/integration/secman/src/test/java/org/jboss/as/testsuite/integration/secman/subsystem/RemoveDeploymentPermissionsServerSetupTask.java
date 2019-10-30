/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
     *
     * @throws Exception
     */
    private static void reload(final ManagementClient managementClient) throws Exception {
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

}
