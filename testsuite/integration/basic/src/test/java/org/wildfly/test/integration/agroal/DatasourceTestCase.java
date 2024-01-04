/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.agroal;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Running tests from {@link AgroalDatasourceTestBase} with standard non-XA datsource.
 *
 * @author <a href="mailto:ochaloup@redhat.com>Ondra Chaloupka</a>
 * @author <a href="mailto:lbarreiro@redhat.com>Luis Barreiro</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DatasourceTestCase extends AgroalDatasourceTestBase {
    private static final Logger log = Logger.getLogger(AgroalDatasourceTestBase.class);

    @Override
    protected ModelNode createDataSource(Datasource datasource) throws Exception {
        ModelNode address = getDataSourceAddress(datasource);

        ModelNode operation = getDataSourceOperation(address);

        operation.get("jndi-name").set(datasource.getJndiName());

        operation.get("connection-factory").set(getConnectionFactoryObject(datasource));
        operation.get("connection-pool").set(getConnectionPoolObject(datasource));

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
        ModelNode address = new ModelNode().add(SUBSYSTEM, DATASOURCES_SUBSYSTEM).add("datasource", datasource.getName());
        address.protect();
        return address;
    }

    @Override
    protected void testConnection(Datasource datasource) throws Exception {
        testConnectionBase(datasource.getName(), "datasource");
    }

}
