/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.statistics;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 *
 * Server setup task for the test case DataSourceMultipleConnStatsTestCase.
 * Enables statistics and sets min/max pool size on the ExampleDS datasource.
 *
 * @author Daniel Cihak
 */
public class DataSourceMultipleConnStatsServerSetupTask extends SnapshotRestoreSetupTask {

    @Override
    public void doSetup(ManagementClient managementClient, String s) throws Exception {

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
