/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.externalid;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.EJB;

/**
 * Integration test to verify the creation and persistence of EJB timers
 * utilizing the custom external_id database column and API.
 */
@RunWith(Arquillian.class)
@ServerSetup(ExternalIdTimerTestCase.DatabaseTimerStoreSetupTask.class)
public class ExternalIdTimerTestCase {

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, "external-id-timer-test.jar")
                .addClasses(ExternalIdTimerTestCase.class, ExternalIdTimerBean.class, DatabaseTimerStoreSetupTask.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                // Require access to internal WildFly EJB classes for WildFlyTimerConfig and WildFlyTimerService
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.ejb3\n"), "MANIFEST.MF");
    }

    @EJB
    private ExternalIdTimerBean timerBean;

    @Test
    public void testTimerCreationWithExternalId() throws Exception {
        final String expectedExternalId = "foo-external-id";

        // 1. Create the timer using the new Config API
        timerBean.createTimerWithExternalId(expectedExternalId);

        // 2. Verify via raw SQL that the persistence layer inserted it correctly
        int sqlTimerCount = timerBean.getActiveTimersCountByExternalIdViaSql(expectedExternalId);
        assertEquals("SQL Check: There should be exactly 1 timer mapped to the external ID in the database",
                1, sqlTimerCount);

        // 3. Verify via the new WildFlyTimerService API casting mechanism
        int apiTimerCount = timerBean.getActiveTimersCountByExternalIdViaApi(expectedExternalId);
        assertEquals("API Check: The WildFlyTimerService should return exactly 1 timer",
                1, apiTimerCount);

        // 4. Verify a non-existent ID returns 0 via the API
        assertEquals("API Check: A non-existent external ID should return 0 timers",
                0, timerBean.getActiveTimersCountByExternalIdViaApi("unknown-id"));
    }

    /**
     * ServerSetupTask to switch the default timer data store to the database store.
     */
    static class DatabaseTimerStoreSetupTask implements ServerSetupTask {

        private static final ModelNode TIMER_SERVICE_ADDRESS = Operations.createAddress("subsystem", "ejb3", "service", "timer-service");
        private static final ModelNode DB_STORE_ADDRESS = Operations.createAddress("subsystem", "ejb3", "service", "timer-service", "database-data-store", "default-database-store");

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient client = managementClient.getControllerClient();

            // 1. Explicitly add the database-data-store resource
            ModelNode addStoreOp = Operations.createAddOperation(DB_STORE_ADDRESS);
            addStoreOp.get("datasource-jndi-name").set("java:jboss/datasources/ExampleDS");
            addStoreOp.get("database").set("h2");
            addStoreOp.get("partition").set("default");
            ModelNode addResult = client.execute(addStoreOp);
            assertTrue(Operations.isSuccessfulOutcome(addResult));

            // 2. Switch default-data-store to the newly created default-database-store
            ModelNode op = Operations.createWriteAttributeOperation(TIMER_SERVICE_ADDRESS, "default-data-store", "default-database-store");
            ModelNode result = client.execute(op);
            assertTrue(Operations.isSuccessfulOutcome(result));

            ServerReload.executeReloadAndWaitForCompletion(client);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient client = managementClient.getControllerClient();

            // 1. Revert back to default-file-store
            ModelNode op = Operations.createWriteAttributeOperation(TIMER_SERVICE_ADDRESS, "default-data-store", "default-file-store");
            client.execute(op);

            // 2. Remove the database-data-store resource
            ModelNode removeStoreOp = Operations.createRemoveOperation(DB_STORE_ADDRESS);
            client.execute(removeStoreOp);

            ServerReload.executeReloadAndWaitForCompletion(client);
        }
    }
}