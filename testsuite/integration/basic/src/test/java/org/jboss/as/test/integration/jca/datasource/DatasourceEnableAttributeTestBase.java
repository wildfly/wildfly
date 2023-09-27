/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;

import org.jboss.as.test.integration.management.jca.DsMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * A basic testing of setting/unsetting "enable" attribute of datasource.
 *
 * @author <a href="mailto:ochaloup@redhat.com>Ondra Chaloupka</a>
 */
public abstract class DatasourceEnableAttributeTestBase extends DsMgmtTestBase {
    private static final Logger log = Logger.getLogger(DatasourceEnableAttributeTestBase.class);

    private static final String DS_ENABLED_SYSTEM_PROPERTY_NAME = "ds.enabled";

    @Test
    public void addDatasourceEnabled() throws Exception {
        Datasource ds = Datasource.Builder("testDatasourceEnabled")
                .enabled(true)
                .build();

        try {
            createDataSource(ds);
            testConnection(ds);
        } finally {
            removeDataSourceSilently(ds);
        }
    }

    @Test(expected = MgmtOperationException.class)
    public void addDatasourceDisabled() throws Exception {
        Datasource ds = Datasource.Builder("testDatasourceDisabled")
                .enabled(false)
                .build();

        try {
            createDataSource(ds);
            testConnection(ds);
        } finally {
            removeDataSourceSilently(ds);
        }
    }

    @Test
    public void enableLater() throws Exception {
        Datasource ds = Datasource.Builder("testDatasourceLater")
                .enabled(false)
                .build();

        try {
            createDataSource(ds);

            try {
                testConnection(ds);
                Assert.fail("Datasource " + ds + " is disabled. Test connection can't succeed.");
            } catch (MgmtOperationException moe) {
                // expecting that datasource won't be available 'online'
            }

            enableDatasource(ds);
            testConnection(ds);

        } finally {
            removeDataSourceSilently(ds);
        }
    }

    @Test
    public void enableBySystemProperty() throws Exception {
        Datasource ds = Datasource.Builder("testDatasourceEnableBySystem")
                .enabled(wrapProp(DS_ENABLED_SYSTEM_PROPERTY_NAME))
                .build();

        try {
            addSystemProperty(DS_ENABLED_SYSTEM_PROPERTY_NAME, "true");
            createDataSource(ds);
            testConnection(ds);
        } finally {
            removeSystemPropertySilently(DS_ENABLED_SYSTEM_PROPERTY_NAME);
            removeDataSourceSilently(ds);
        }
    }

    @Test(expected = MgmtOperationException.class)
    public void disableBySystemProperty() throws Exception {
        Datasource ds = Datasource.Builder("testDatasourceDisableBySystem")
                .enabled(wrapProp(DS_ENABLED_SYSTEM_PROPERTY_NAME))
                .build();

        try {
            addSystemProperty(DS_ENABLED_SYSTEM_PROPERTY_NAME, "false");
            createDataSource(ds);
            testConnection(ds);
        } finally {
            removeSystemPropertySilently(DS_ENABLED_SYSTEM_PROPERTY_NAME);
            removeDataSourceSilently(ds);
        }
    }

    @Test
    public void allBySystemProperty() throws Exception {
        String url = "ds.url";
        String username = "ds.username";
        String password = "ds.password";
        String jndiName = "ds.jndi";
        String driverName = "ds.drivername";
        Datasource ds = Datasource.Builder("testAllBySystem")
                .connectionUrl(wrapProp(url))
                .userName(wrapProp(username))
                .password(wrapProp(password))
                .jndiName(wrapProp(jndiName))
                .driverName(wrapProp(driverName))
                .enabled(wrapProp(DS_ENABLED_SYSTEM_PROPERTY_NAME))
                .build();

        try {
            Datasource defaultPropertyDs = Datasource.Builder("temporary").build();
            addSystemProperty(url, defaultPropertyDs.getConnectionUrl());
            addSystemProperty(username, defaultPropertyDs.getUserName());
            addSystemProperty(password, defaultPropertyDs.getPassword());
            addSystemProperty(jndiName, defaultPropertyDs.getJndiName());
            addSystemProperty(driverName, defaultPropertyDs.getDriverName());
            addSystemProperty(DS_ENABLED_SYSTEM_PROPERTY_NAME, "true");
            createDataSource(ds);
            testConnection(ds);
        } finally {
            removeDataSourceSilently(ds);
            removeSystemPropertySilently(url);
            removeSystemPropertySilently(username);
            removeSystemPropertySilently(password);
            removeSystemPropertySilently(jndiName);
            removeSystemPropertySilently(driverName);
            removeSystemPropertySilently(DS_ENABLED_SYSTEM_PROPERTY_NAME);
        }
    }

    /*
     * Abstract method overriden in test subclasses
     */
    protected abstract ModelNode createDataSource(Datasource datasource) throws Exception;

    protected abstract void removeDataSourceSilently(Datasource datasource);

    protected abstract ModelNode getDataSourceAddress(Datasource datasource);

    protected abstract void testConnection(Datasource datasource) throws Exception;

    /**
     * Common attribute for add datsource operation for non-XA and XA datasource creation.
     */
    protected ModelNode getDataSourceOperation(ModelNode address, Datasource datasource) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get("jndi-name").set(datasource.getJndiName());
        operation.get("driver-name").set(datasource.getDriverName());
        operation.get("enabled").set(datasource.getEnabled());
        operation.get("user-name").set(datasource.getUserName());
        operation.get("password").set(datasource.getPassword());
        return operation;
    }

    protected void enableDatasource(Datasource ds) throws Exception {
        ModelNode address = getDataSourceAddress(ds);
        writeAttribute(address, "enabled", "true");
        // enabling datasource requires reload
        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    protected ModelNode writeAttribute(ModelNode address, String attribute, String value) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set("write-attribute");
        operation.get("name").set(attribute);
        operation.get("value").set(value);
        return executeOperation(operation);
    }

    private ModelNode addSystemProperty(String name, String value) throws IOException, MgmtOperationException {
        ModelNode address = new ModelNode()
                .add(SYSTEM_PROPERTY, name);
        address.protect();
        final ModelNode operation = new ModelNode();
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
            ModelNode address = new ModelNode()
                    .add(SYSTEM_PROPERTY, name);
            address.protect();
            remove(address);
        } catch (Exception e) {
            log.debugf(e, "Can't remove system property '%s' by cli", name);
        }
    }

    private String wrapProp(String propertyName) {
        return String.format("${%s}", propertyName);
    }
}
