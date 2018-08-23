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

import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * A basic testing of getting a connection from an agroal datasource.
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public abstract class AgroalDatasourceTestBase extends ContainerResourceMgmtTestBase {

    private static final Logger log = Logger.getLogger(AgroalDatasourceTestBase.class);

    protected static final String AGROAL_EXTENTION = "org.wildfly.extension.datasources-agroal";

    protected static final String DATASOURCES_SUBSYSTEM = "datasources-agroal";

    private static String wrapProp(String propertyName) {
        return String.format("${%s}", propertyName);
    }

    @Before
    public void enableExtension() throws Exception {
        ModelNode extensionOp = new ModelNode();
        extensionOp.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(EXTENSION, AGROAL_EXTENTION);
        extensionOp.get(OP).set(ADD);

        try {
            executeOperation(extensionOp);
        } catch (MgmtOperationException e) {
            log.infof( "Can't add extension '%s' by cli: %s", EXTENSION, e.getResult().get(FAILURE_DESCRIPTION));
        }

        ModelNode subsystemOp = new ModelNode();
        subsystemOp.get(OP_ADDR).set(SUBSYSTEM, DATASOURCES_SUBSYSTEM);
        subsystemOp.get(OP).set(ADD);

        try {
            executeOperation(subsystemOp);
        } catch (MgmtOperationException e) {
            log.infof( "Can't add subsystem '%s' by cli: %s", DATASOURCES_SUBSYSTEM, e.getResult().get(FAILURE_DESCRIPTION));
        }

        // Reload before continuing
        ServerReload.executeReloadAndWaitForCompletion(getModelControllerClient(), TimeoutUtil.adjust(50000));
    }

    @After
    public void disableExtension() throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(EXTENSION, AGROAL_EXTENTION);
        operation.get(OP).set(REMOVE);

        try {
            executeOperation(operation);
        } catch (MgmtOperationException e) {
            log.warnf("Can't remove extension '%s' by cli: ", AGROAL_EXTENTION, e.getResult().get(FAILURE_DESCRIPTION));
        }
    }

    // --- //

    @Test
    public void addDatasource() throws Exception {
        Datasource ds = Datasource.Builder("testDatasourceEnabled").build();

        try {
            createDriver(ds);
            createDataSource(ds);
            testConnection(ds);
        } finally {
            removeDataSourceSilently(ds);
            removeDriverSilently(ds);
        }
    }

    @Test
    public void allBySystemProperty() throws Exception {
        String url = "myds.url";
        String username = "myds.username";
        String password = "myds.password";
        String jndiName = "myds.jndi";

        Datasource ds = Datasource.Builder("testAllBySystem")
                                  .connectionUrl(wrapProp(url))
                                  .userName(wrapProp(username))
                                  .password(wrapProp(password))
                                  .jndiName(wrapProp(jndiName))
                                  .driverName("h2_ref")
                                  .build();

        try {
            Datasource defaultPropertyDs = Datasource.Builder("temporary").build();
            addSystemProperty(url, defaultPropertyDs.getConnectionUrl());
            addSystemProperty(username, defaultPropertyDs.getUserName());
            addSystemProperty(password, defaultPropertyDs.getPassword());
            addSystemProperty(jndiName, defaultPropertyDs.getJndiName());

            createDriver(ds);
            createDataSource(ds);
            testConnection(ds);
        } finally {
            removeDataSourceSilently(ds);
            removeDriverSilently(ds);

            removeSystemPropertySilently(url);
            removeSystemPropertySilently(username);
            removeSystemPropertySilently(password);
            removeSystemPropertySilently(jndiName);
        }
    }

    // --- //

    /*
     * Abstract methods overridden in test subclasses
     */
    protected abstract ModelNode createDataSource(Datasource datasource) throws Exception;

    protected abstract void removeDataSourceSilently(Datasource datasource) throws Exception;

    protected abstract ModelNode getDataSourceAddress(Datasource datasource);

    protected abstract void testConnection(Datasource datasource) throws Exception;

    /**
     * Common attribute for add datasource operation for non-XA and XA datasource creation.
     */
    protected ModelNode getDataSourceOperation(ModelNode address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    protected ModelNode getConnectionFactoryObject( Datasource datasource) {
        ModelNode factoryObject = new ModelNode();
        factoryObject.get("driver").set(datasource.getDriverName());
        factoryObject.get("url").set(datasource.getConnectionUrl());
        factoryObject.get("username").set(datasource.getUserName());
        factoryObject.get("password").set(datasource.getPassword());
        return factoryObject;
    }

    protected ModelNode getConnectionPoolObject(Datasource datasource) {
        ModelNode poolObject = new ModelNode();
        poolObject.get("min-size").set(datasource.getMinSize());
        poolObject.get("initial-size").set(datasource.getInitialSize());
        poolObject.get("max-size").set(datasource.getMaxSize());
        poolObject.get("blocking-timeout").set(datasource.getBlockingTimeout());
        return poolObject;
    }

    protected void createDriver(Datasource datasource) throws Exception {
        ModelNode address = new ModelNode().add(SUBSYSTEM, DATASOURCES_SUBSYSTEM).add("driver", datasource.getDriverName());
        address.protect();

        ModelNode driverOp = new ModelNode();
        driverOp.get(OP).set(ADD);
        driverOp.get(OP_ADDR).set(address);

        driverOp.get("module").set(datasource.getDriverModule());
        if (datasource.getDriverClass() != null) {
            driverOp.get("class").set(datasource.getDriverClass());
        }

        try {
            executeOperation(driverOp);
        } catch (MgmtOperationException e) {
            log.infof( "Can't add driver '%s' by cli: %s", DATASOURCES_SUBSYSTEM, e.getResult().get(FAILURE_DESCRIPTION));
        }
    }

    protected void removeDriverSilently(Datasource datasource) throws Exception {
        if (datasource == null || datasource.getDriverName() == null) {
            return;
        }

        ModelNode address = new ModelNode().add(SUBSYSTEM, DATASOURCES_SUBSYSTEM).add("driver", datasource.getDriverName());
        address.protect();
        try {
            ModelNode removeOperation = Operations.createRemoveOperation(address);
            removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
            executeOperation(removeOperation);
        } catch (MgmtOperationException e) {
            log.warnf(e, "Can't remove driver at address '%s': %s", datasource.getDriverName(), e.getResult().get(FAILURE_DESCRIPTION));
        }
    }

    // --- //

    protected void testConnectionBase(String dsName, String type) throws Exception {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, DATASOURCES_SUBSYSTEM);
        address.add(type, dsName);
        address.protect();

        ModelNode operation = new ModelNode();
        operation.get(OP).set("test-connection");
        operation.get(OP_ADDR).set(address);

        executeOperation(operation);
    }

    // --- //

    protected ModelNode readAttribute(ModelNode address, String attribute) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get(NAME).set(attribute);
        operation.get(OP_ADDR).set(address);
        return executeOperation(operation);
    }

    protected ModelNode writeAttribute(ModelNode address, String attribute, String value) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set("write-attribute");
        operation.get("name").set(attribute);
        operation.get("value").set(value);
        return executeOperation(operation);
    }

    // --- //

    private ModelNode addSystemProperty(String name, String value) throws IOException, MgmtOperationException {
        ModelNode address = new ModelNode().add(SYSTEM_PROPERTY, name);
        address.protect();

        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(ADD);
        operation.get(VALUE).set(value);

        return executeOperation(operation);
    }

    protected void removeSystemPropertySilently(String name) {
        if (name == null) {
            return;
        }
        try {
            ModelNode address = new ModelNode().add(SYSTEM_PROPERTY, name);
            address.protect();

            remove(address);
        } catch (Exception e) {
            log.warnf("Can't remove system property '%s' by cli: %s", name, e.getMessage());
        }
    }
}
