/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.agroal;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Running tests from {@link AgroalDatasourceTestBase} with standard non-XA datasource.
 *
 * @author <a href="mailto:ochaloup@redhat.com>Ondra Chaloupka</a>
 * @author <a href="mailto:lbarreiro@redhat.com>Luis Barreiro</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XADatasourceTestCase extends AgroalDatasourceTestBase {
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
    protected void removeDataSourceSilently(Datasource datasource) throws Exception {
        if (datasource == null || datasource.getName() == null) {
            return;
        }

        ModelNode address = getDataSourceAddress(datasource);
        try {
            ModelNode removeOperation = Operations.createRemoveOperation(address);
            removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
            executeOperation(removeOperation);
        } catch (MgmtOperationException e) {
            log.debugf(e, "Can't remove datasource at address '%s': %s", address, e.getResult().get(FAILURE_DESCRIPTION));
        }
    }

    @Override
    protected ModelNode getDataSourceAddress(Datasource datasource) {
        ModelNode address = new ModelNode().add(SUBSYSTEM, DATASOURCES_SUBSYSTEM).add("xa-datasource", datasource.getName());
        address.protect();
        return address;
    }

    @Override
    protected void testConnection(Datasource datasource) throws Exception {
        testConnectionBase(datasource.getName(), "xa-datasource");
    }

}
