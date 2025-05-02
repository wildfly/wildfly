/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

import org.jboss.as.arquillian.api.ReloadIfRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ReloadIfRequired(20L)
public class AddDataSourceSetupTask implements ServerSetupTask {
    static final String JNDI_NAME = "java:jboss/datasource/TestDataSource";
    private static final ModelNode ADDRESS = Operations.createAddress("subsystem", "datasources", "data-source", "test-ds");

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        final ModelNode op = Operations.createAddOperation(ADDRESS);
        op.get("jndi-name").set(JNDI_NAME);
        op.get("driver-name").set("h2");
        op.get("user-name").set("sa");
        op.get("password").set("sa");
        op.get("jta").set(true);
        op.get("connection-url").set("jdbc:h2:mem:secondary;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=${wildfly.h2.compatibility.mode:REGULAR}");
        executeOperation(managementClient, op);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        executeOperation(managementClient, Operations.createRemoveOperation(ADDRESS));
    }
}
