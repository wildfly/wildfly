/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * {@link ServerSetupTask} instance for security realms setup.
 * 
 * @see SecurityRealm
 * @author Josef Cacek
 */
public abstract class AbstractSecurityRealmsServerSetupTask implements ServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(AbstractSecurityRealmsServerSetupTask.class);

    protected ManagementClient managementClient;

    private SecurityRealm[] securityRealms;

    // Public methods --------------------------------------------------------

    /**
     * Adds security realms retrieved from {@link #getSecurityRealms()}.
     * 
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public final void setup(final ManagementClient managementClient, String containerId) throws Exception {
        this.managementClient = managementClient;
        securityRealms = getSecurityRealms();

        if (securityRealms == null || securityRealms.length == 0) {
            LOGGER.warn("Empty security realm configuration.");
            return;
        }

        final List<ModelNode> updates = new LinkedList<ModelNode>();
        for (final SecurityRealm securityRealm : securityRealms) {
            final String securityRealmName = securityRealm.getName();
            LOGGER.info("Adding security realm " + securityRealmName);
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // /core-service=management/security-realm=foo:add
            final PathAddress realmAddr = PathAddress.pathAddress().append(CORE_SERVICE, MANAGEMENT)
                    .append(SECURITY_REALM, securityRealmName);
            ModelNode op = Util.createAddOperation(realmAddr);
            steps.add(op);

            // /core-service=management/security-realm=foo/server-identity=secret:add(value="Q29ubmVjdGlvblBhc3N3b3JkMSE=")
            final ServerIdentity serverIdentity = securityRealm.getServerIdentity();
            if (serverIdentity != null && StringUtils.isNotEmpty(serverIdentity.getSecret())) {
                final ModelNode secretModuleNode = Util.createAddOperation(realmAddr.append(SERVER_IDENTITY, SECRET));
                secretModuleNode.get(Constants.VALUE).set(serverIdentity.getSecret());
                secretModuleNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                steps.add(secretModuleNode);
            }
            updates.add(compositeOp);
        }
        Utils.applyUpdates(updates, managementClient.getControllerClient());
    }

    /**
     * Removes the security realms from the AS configuration.
     * 
     * @param managementClient
     * @param containerId
     */
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (securityRealms == null || securityRealms.length == 0) {
            LOGGER.warn("Empty security realms configuration.");
            return;
        }

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        for (final SecurityRealm securityRealm : securityRealms) {
            final String realmName = securityRealm.getName();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Removing security realm " + realmName);
            }
            final ModelNode op = Util.createRemoveOperation(PathAddress.pathAddress().append(CORE_SERVICE, MANAGEMENT)
                    .append(SECURITY_REALM, realmName));
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);
        }
        Utils.applyUpdates(updates, managementClient.getControllerClient());
        this.managementClient = null;
        this.securityRealms = null;
    }

    // Protected methods -----------------------------------------------------

    /**
     * Returns configuration for creating security realms.
     * 
     * @return array of SecurityRealm instances
     */
    protected abstract SecurityRealm[] getSecurityRealms() throws Exception;
}
