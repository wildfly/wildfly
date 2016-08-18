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

package org.jboss.as.test.integration.jca.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.runner.RunWith;

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
        operation.get("connection-url").set(datasource.getConnectionUrl());
        executeOperation(operation);

        reload();
        return address;
    }

    @Override
    protected void removeDataSourceSilently(Datasource datasource) {
        if (datasource == null || datasource.getName() == null) {
            return;
        }

        ModelNode address = getDataSourceAddress(datasource);
        try {
            remove(address);
            reload();
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
}
