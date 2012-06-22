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
package org.jboss.as.test.integration.security.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.config.DataSource;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * {@link ServerSetupTask} instance for datasources setup.
 * 
 * @author Josef Cacek
 */
public abstract class AbstractDataSourceServerSetupTask implements ServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(AbstractDataSourceServerSetupTask.class);
    private static final String SUBSYSTEM_DATASOURCES = "datasources";
    private static final String DATASOURCE = "data-source";

    // Public methods --------------------------------------------------------

    /**
     * Adds a security domain represented by this class to the AS configuration.
     * 
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public final void setup(final ManagementClient managementClient, String containerId) throws Exception {
        final DataSource[] dataSourceConfigurations = getDataSourceConfigurations(managementClient, containerId);

        if (dataSourceConfigurations == null) {
            LOGGER.warn("Null DataSourceConfiguration array provided");
            return;
        }

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        for (final DataSource config : dataSourceConfigurations) {
            final String name = config.getName();
            LOGGER.info("Adding datasource " + name);
            final ModelNode dsNode = new ModelNode();
            dsNode.get(OP).set(ADD);
            dsNode.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
            dsNode.get(OP_ADDR).add(DATASOURCE, name);

            dsNode.get("connection-url").set(config.getConnectionUrl());
            dsNode.get("jndi-name").set(config.getJndiName());
            dsNode.get("driver-name").set(config.getDriver());
            if (StringUtils.isNotEmpty(config.getUsername())) {
                dsNode.get("user-name").set(config.getUsername());
            }
            if (StringUtils.isNotEmpty(config.getPassword())) {
                dsNode.get("password").set(config.getPassword());
            }
            updates.add(dsNode);
            final ModelNode enableNode = new ModelNode();
            enableNode.get(OP).set(ENABLE);
            enableNode.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
            enableNode.get(OP_ADDR).add(DATASOURCE, name);
            updates.add(enableNode);
        }
        Utils.applyUpdates(updates, managementClient.getControllerClient());
    }

    /**
     * Removes the security domain from the AS configuration.
     * 
     * @param managementClient
     * @param containerId
     * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        final DataSource[] dataSourceConfigurations = getDataSourceConfigurations(managementClient, containerId);
        if (dataSourceConfigurations == null) {
            LOGGER.warn("Null DataSourceConfiguration array provided");
            return;
        }
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        for (final DataSource config : dataSourceConfigurations) {
            final String name = config.getName();
            LOGGER.info("Removing datasource " + name);
            final ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
            op.get(OP_ADDR).add(DATASOURCE, name);

            updates.add(op);
        }

        Utils.applyUpdates(updates, managementClient.getControllerClient());
    }

    // Protected methods -----------------------------------------------------

    protected DataSource[] getDataSourceConfigurations(final ManagementClient managementClient, String containerId) {
        return null;
    }
}
