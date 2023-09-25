/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Running tests from {@link DatasourceEnableAttributeTestBase} with standard non-XA datsource.
 *
 * @author <a href="mailto:ochaloup@redhat.com>Ondra Chaloupka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DatasourceEnableAttributeTestCase extends DatasourceEnableAttributeTestBase {
    private static final Logger log = Logger.getLogger(DatasourceEnableAttributeTestBase.class);

    @Override
    protected ModelNode createDataSource(Datasource datasource) throws Exception {
        ModelNode address = getDataSourceAddress(datasource);

        ModelNode operation = getDataSourceOperation(address, datasource);
        if (datasource.getConnectionUrl() != null) {
            operation.get("connection-url").set(datasource.getConnectionUrl());
        }
        if (datasource.getDataSourceClass() != null) {
            operation.get("datasource-class").set(datasource.getDataSourceClass());
        }
        executeOperation(operation);

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
            log.debugf(e, "Can't remove datasource at address '%s'", address);
        }
    }

    @Override
    protected ModelNode getDataSourceAddress(Datasource datasource) {
        ModelNode address = new ModelNode()
                .add(SUBSYSTEM, "datasources")
                .add("data-source", datasource.getName());
        address.protect();
        return address;
    }

    @Override
    protected void testConnection(Datasource datasource) throws Exception {
        testConnection(datasource.getName());
    }

    @Test
    public void testNoConnectionURLWithDatasourceClass() throws Exception {
        final String dsName = "testDatasourceNoConnectionURLWithDataSourceClass";
        Datasource ds = Datasource.Builder(dsName)
                .enabled(false)
                .connectionUrl(null)
                .dataSourceClass("org.h2.jdbcx.JdbcDataSource")
                .build();

        try {
            createDataSource(ds);
            addDataSourceConnectionProps(ds);
            enableDatasource(ds);
            testConnection(ds);
        } finally {
            removeDataSourceSilently(ds);
        }
    }

    private void addDataSourceConnectionProps(Datasource ds) throws Exception {
        String ConnPropURL = "URL";
        String URLValue = "jdbc:h2:mem:testDS";
        ModelNode address = new ModelNode()
            .add(SUBSYSTEM, "datasources")
            .add("data-source", ds.getName())
            .add("connection-properties", ConnPropURL);
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get(VALUE).set(URLValue);
        executeOperation(operation);
    }
}
