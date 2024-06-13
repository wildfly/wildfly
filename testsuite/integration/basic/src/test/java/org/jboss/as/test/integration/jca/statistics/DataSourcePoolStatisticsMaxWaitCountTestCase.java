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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
 * Test for <a href="https://issues.redhat.com/browse/ WFLY-14691">WFLY-14691</a>
 * <p>
 * Tests that MaxWaitCount pool statistics attribute is correct.
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(DataSourcePoolStatisticsMaxWaitCountTestCase.MinimalPoolServerSetupTask.class)
public class DataSourcePoolStatisticsMaxWaitCountTestCase extends AbstractDataSourcePoolStatisticsTestCase {

    @Deployment(name = DEPLOYMENT)
    public static WebArchive appDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DataSourcePoolStatisticsMaxWaitCountTestCase.class.getName() + ".war");
        war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller," +
                // Needed for RemotingPermission class if security manager is enabled
                (System.getProperty("security.manager") == null ? "" : "org.jboss.remoting,") +
                "org.jboss.dmr\n"), "MANIFEST.MF");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read"),
                new RuntimePermission("modifyThread")
        ), "permissions.xml");
        war.addClass(AbstractDataSourcePoolStatisticsTestCase.class);
        return war;
    }

    private Runnable allocateConnectionsCall = () -> {
        try {
            allocateConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    /**
     * Tests the data source statistics attribute MaxWaitCount during waiting requests.
     * <p>
     * Test for <a href="https://issues.redhat.com/browse/ WFLY-14691">WFLY-14691</a>
     *
     * @throws Exception
     */
    @Test
    public void testDataSourceStatistics() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        int maxWaitCount = readStatisticsAttribute("MaxWaitCount");
        assertEquals(0, maxWaitCount);

        try {
            executor.execute(allocateConnectionsCall);
            Thread.sleep(1000L);
            executor.execute(allocateConnectionsCall);
            Thread.sleep(1000L);
            maxWaitCount = readStatisticsAttribute("MaxWaitCount");
            assertEquals(1, maxWaitCount);
            executor.execute(allocateConnectionsCall);
            Thread.sleep(1000L);
            maxWaitCount = readStatisticsAttribute("MaxWaitCount");
            assertEquals(2, maxWaitCount);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            clearConnections();
        }
    }


    /**
     * Server setup task for the test case DataSourcePoolStatisticsMaxWaitCountTestCase.
     * Enables statistics and sets min-pool-size=max-pool-size=1 on the ExampleDS datasource.
     */
    public static class MinimalPoolServerSetupTask extends SnapshotRestoreSetupTask {

        @Override
        public void doSetup(ManagementClient managementClient, String s) throws Exception {
            // /subsystem=datasources/data-source=ExampleDS:write-attribute(name=statistics-enabled, value=true)
            // /subsystem=datasources/data-source=ExampleDS:write-attribute(name=max-pool-size, value=1)
            // /subsystem=datasources/data-source=ExampleDS:write-attribute(name=min-pool-size, value=1)

            ModelNode enableStatsOp = createOpNode("subsystem=datasources/data-source=ExampleDS", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            enableStatsOp.get(ModelDescriptionConstants.NAME).set("statistics-enabled");
            enableStatsOp.get(ModelDescriptionConstants.VALUE).set(true);

            ModelNode maxPoolSizeOp = createOpNode("subsystem=datasources/data-source=ExampleDS", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            maxPoolSizeOp.get(ModelDescriptionConstants.NAME).set("max-pool-size");
            maxPoolSizeOp.get(ModelDescriptionConstants.VALUE).set(1);

            ModelNode minPoolSizeOp = createOpNode("subsystem=datasources/data-source=ExampleDS", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            minPoolSizeOp.get(ModelDescriptionConstants.NAME).set("min-pool-size");
            minPoolSizeOp.get(ModelDescriptionConstants.VALUE).set(1);

            ModelNode updateOp = Util.createCompositeOperation(List.of(enableStatsOp, maxPoolSizeOp, minPoolSizeOp));
            updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());

            ServerReload.reloadIfRequired(managementClient);
        }
    }
}
