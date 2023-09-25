/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.runner.RunWith;

/**
 * Running tests from {@link DatasourceEnableAttributeTestBase} with XA datsource.
 *
 * @author <a href="mailto:ochaloup@redhat.com>Ondra Chaloupka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DatasourceXaEnableAttributeTestCase extends DatasourceEnableAttributeTestBase {
    private static final Logger log = Logger.getLogger(DatasourceXaEnableAttributeTestCase.class);

    @Override
    protected ModelNode createDataSource(Datasource datasource) throws Exception {
        ModelNode address = getDataSourceAddress(datasource);

        ModelNode batch = new ModelNode();
        batch.get(OP).set(COMPOSITE);
        batch.get(OP_ADDR).setEmptyList();

        ModelNode operation = getDataSourceOperation(address, datasource);
        batch.get(STEPS).add(operation);

        ModelNode operationXAProperty = getAddXADataSourcePropertyOperation(address, "URL", datasource.getConnectionUrl());
        batch.get(STEPS).add(operationXAProperty);

        executeOperation(batch);
        return address;
    }

    @Override
    protected void removeDataSourceSilently(Datasource datasource) {
        if (datasource == null || datasource.getName() == null) {
            return;
        }

        ModelNode address = getDataSourceAddress(datasource);
        try {
            ModelNode removeOperation = Operations.createRemoveOperation(address);
            removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
            executeOperation(removeOperation);
        } catch (Exception e) {
            log.debugf(e, "Can't remove xa datasource at address '%s'", address);
        }
    }

    @Override
    protected ModelNode getDataSourceAddress(Datasource datasource) {
        ModelNode address = new ModelNode()
                .add(SUBSYSTEM, "datasources")
                .add("xa-data-source", datasource.getName());
        address.protect();
        return address;
    }

    @Override
    protected void testConnection(Datasource datasource) throws Exception {
        testConnectionXA(datasource.getName());
    }

    private ModelNode getAddXADataSourcePropertyOperation(final ModelNode address, final String name, final String value) throws IOException, MgmtOperationException {
        final ModelNode propertyAddress = address.clone();
        propertyAddress.add("xa-datasource-properties", name);
        propertyAddress.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(propertyAddress);
        operation.get("value").set(value);

        return operation;
    }
}
