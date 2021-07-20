/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.statistics;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
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
        List<ModelNode> operations = new ArrayList<>();

        ModelNode enableStatsOp = createOpNode("subsystem=datasources/data-source=ExampleDS", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        enableStatsOp.get(ModelDescriptionConstants.NAME).set("statistics-enabled");
        enableStatsOp.get(ModelDescriptionConstants.VALUE).set(true);
        operations.add(enableStatsOp);

        ModelNode maxPoolSizeOp = createOpNode("subsystem=datasources/data-source=ExampleDS", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        maxPoolSizeOp.get(ModelDescriptionConstants.NAME).set("max-pool-size");
        maxPoolSizeOp.get(ModelDescriptionConstants.VALUE).set(1);
        operations.add(maxPoolSizeOp);

        ModelNode minPoolSizeOp = createOpNode("subsystem=datasources/data-source=ExampleDS", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        minPoolSizeOp.get(ModelDescriptionConstants.NAME).set("min-pool-size");
        minPoolSizeOp.get(ModelDescriptionConstants.VALUE).set(1);
        operations.add(minPoolSizeOp);

        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());

        ServerReload.reloadIfRequired(managementClient);
    }

}
