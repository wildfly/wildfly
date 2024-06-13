/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.statistics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.io.FilePermission;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * Test for <a href="https://issues.redhat.com/browse/WFLY-14789">WFLY-14789</a>
 * <p>
 * Tests that the clearStatistics operation on a datasource pool doesn't clear some attributes that are not supposed to be cleared.
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(DataSourcePoolClearStatisticsTestCase.FixedSizePrefillPoolServerSetupTask.class)
public class DataSourcePoolClearStatisticsTestCase extends AbstractDataSourcePoolStatisticsTestCase {

    @Deployment(name = DEPLOYMENT)
    public static WebArchive appDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DataSourcePoolClearStatisticsTestCase.class.getName() + ".war");
        war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller," +
                // Needed for RemotingPermission class if security manager is enabled
                (System.getProperty("security.manager") == null ? "" : "org.jboss.remoting,") +
                "org.jboss.dmr\n"), "MANIFEST.MF");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");
        war.addClass(AbstractDataSourcePoolStatisticsTestCase.class);
        return war;
    }

    /**
     * Tests data source statistics after clearStatistics operation was executed.
     * <p>
     * Test for <a href="https://issues.redhat.com/browse/WFLY-14789">WFLY-14789</a>
     *
     * @throws Exception
     */
    @Test
    public void testClearedDataSourceStatistics() throws Exception {
        try {
            allocateConnection();

            clearStatistics();

            int activeCount = readStatisticsAttribute("ActiveCount");
            int availableCount = readStatisticsAttribute("AvailableCount");
            int createdCount = readStatisticsAttribute("CreatedCount");
            int idleCount = readStatisticsAttribute("IdleCount");
            int inUseCount = readStatisticsAttribute("InUseCount");
            assertEquals(5, activeCount);
            assertEquals(4, availableCount);
            assertEquals(5, createdCount);
            assertEquals(4, idleCount);
            assertEquals(1, inUseCount);
        } finally {
            clearConnections();
        }
    }

    /**
     * Server setup task for the test case DataSourcePoolClearStatisticsTestCase.
     * Enables statistics and pool-prefill, and sets min-pool-size=max-pool-size=5 on the ExampleDS datasource.
     */
    public static class FixedSizePrefillPoolServerSetupTask extends SnapshotRestoreSetupTask {

        @Override
        public void doSetup(ManagementClient managementClient, String s) throws Exception {
            // /subsystem=datasources/data-source=ExampleDS:write-attribute(name=statistics-enabled, value=true)
            // /subsystem=datasources/data-source=ExampleDS:write-attribute(name=max-pool-size, value=5)
            // /subsystem=datasources/data-source=ExampleDS:write-attribute(name=min-pool-size, value=5)
            // /subsystem=datasources/data-source=ExampleDS:write-attribute(name=pool-prefill, value=true)

            ModelNode enableStatsOp = createOpNode("subsystem=datasources/data-source=ExampleDS", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            enableStatsOp.get(ModelDescriptionConstants.NAME).set("statistics-enabled");
            enableStatsOp.get(ModelDescriptionConstants.VALUE).set(true);

            ModelNode maxPoolSizeOp = createOpNode("subsystem=datasources/data-source=ExampleDS", WRITE_ATTRIBUTE_OPERATION);
            maxPoolSizeOp.get(NAME).set("max-pool-size");
            maxPoolSizeOp.get(VALUE).set(5);

            ModelNode minPoolSizeOp = createOpNode("subsystem=datasources/data-source=ExampleDS", WRITE_ATTRIBUTE_OPERATION);
            minPoolSizeOp.get(NAME).set("min-pool-size");
            minPoolSizeOp.get(VALUE).set(5);

            ModelNode prefillOp = createOpNode("subsystem=datasources/data-source=ExampleDS", WRITE_ATTRIBUTE_OPERATION);
            prefillOp.get(NAME).set("pool-prefill");
            prefillOp.get(VALUE).set(true);

            ModelNode updateOp = Util.createCompositeOperation(List.of(enableStatsOp, maxPoolSizeOp, minPoolSizeOp, prefillOp));
            updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());

            ServerReload.reloadIfRequired(managementClient);
        }
    }
}
