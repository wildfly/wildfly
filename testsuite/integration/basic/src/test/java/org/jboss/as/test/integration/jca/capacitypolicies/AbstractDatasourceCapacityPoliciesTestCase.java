/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2015, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.test.integration.jca.capacitypolicies;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.datasources.WildFlyDataSource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.JcaTestsUtil;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.adapters.jdbc.WrapperDataSource;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPool;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test for JCA capacity policies JBJCA-986 using datasource/xa-datasource
 *
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public abstract class AbstractDatasourceCapacityPoliciesTestCase extends JcaMgmtBase {
    protected static final String DS_JNDI_NAME = "java:jboss/datasources/TestDatasource";
    protected static final String DS_NAME = "TestDatasource";
    protected static final ModelNode DS_ADDRESS = new ModelNode().add(SUBSYSTEM, "datasources")
            .add("data-source", DS_NAME);
    protected static final ModelNode XA_DS_ADDRESS = new ModelNode().add(SUBSYSTEM, "datasources")
            .add("xa-data-source", DS_NAME);
    private boolean xaDatasource;

    static {
        DS_ADDRESS.protect();
        XA_DS_ADDRESS.protect();
    }

    public AbstractDatasourceCapacityPoliciesTestCase(boolean xaDatasource) {
        this.xaDatasource = xaDatasource;
    }

    @Resource(mappedName = "java:jboss/datasources/TestDatasource")
    private DataSource ds;

    @ArquillianResource
    private ManagementClient managementClient;

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
    }

    @Deployment
    public static Archive<?> createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jca-capacity-test.jar");
        jar.addClasses(JcaMgmtBase.class,
                ManagementOperations.class,
                ContainerResourceMgmtTestBase.class,
                AbstractMgmtTestBase.class,
                JcaMgmtServerSetupTask.class,
                MgmtOperationException.class,
                AbstractDatasourceCapacityPoliciesTestCase.class,
                DatasourceCapacityPoliciesTestCase.class,
                JcaTestsUtil.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: javax.inject.api,org.jboss.as.connector," +
                "org.jboss.as.controller,org.jboss.dmr,org.jboss.as.cli,org.jboss.staxmapper," +
                "org.jboss.ironjacamar.impl, org.jboss.ironjacamar.jdbcadapters\n"), "MANIFEST.MF");

        return jar;
    }

    /**
     * Test pool with
     * org.jboss.jca.core.connectionmanager.pool.capacity.MinPoolSizeDecrementer
     * org.jboss.jca.core.connectionmanager.pool.capacity.MaxPoolSizeIncrementer"
     *
     * @throws Exception
     */
    @Test
    public void testNonDefaultDecrementerAndIncrementer() throws Exception {
        checkStatistics(5, 0, 0, 0);

        Connection[] connections = new Connection[4];
        connections[0] = ds.getConnection();

        // sometimes InUseCount is 2 and AvailableCount is 3 when statistics are checked right after
        // ds.getConnection, hence this sleep. I guess it's caused by CapacityFiller
        Thread.sleep(500);

        checkStatistics(4, 1, 5, 0);

        connections[1] = ds.getConnection();
        checkStatistics(3, 2, 5, 0);

        connections[2] = ds.getConnection();
        checkStatistics(2, 3, 5, 0);

        connections[3] = ds.getConnection();
        checkStatistics(1, 4, 5, 0);

        for (int i = 0; i < 4; i++) {
            Connection c = connections[i];
            c.close();
        }

        WrapperDataSource wsds = JcaTestsUtil.extractWrapperDatasource((WildFlyDataSource) ds);
        ManagedConnectionPool mcp = JcaTestsUtil.extractManagedConnectionPool(wsds);
        JcaTestsUtil.callRemoveIdleConnections(mcp);

        checkStatistics(5, 0, 2, 3);
    }

    private void checkStatistics(int expectedAvailableCount, int expectedInUseCount,
                                 int expectedActiveCount, int expectedDestroyedCount) throws Exception {
        ModelNode statsAddress = xaDatasource ? XA_DS_ADDRESS.clone() : DS_ADDRESS.clone();
        statsAddress.add("statistics", "pool");

        int availableCount = readAttribute(statsAddress, "AvailableCount").asInt();
        int inUseCount = readAttribute(statsAddress, "InUseCount").asInt();
        int activeCount = readAttribute(statsAddress, "ActiveCount").asInt();
        int destroyedCount = readAttribute(statsAddress, "DestroyedCount").asInt();

        Assert.assertEquals("Unexpected AvailableCount", expectedAvailableCount, availableCount);
        Assert.assertEquals("Unexpected InUseCount", expectedInUseCount, inUseCount);
        Assert.assertEquals("Unexpected ActiveCount", expectedActiveCount, activeCount);
        Assert.assertEquals("Unexpected DestroyedCount", expectedDestroyedCount, destroyedCount);
    }

    abstract static class AbstractDatasourceCapacityPoliciesServerSetup extends JcaMgmtServerSetupTask {
        private boolean xa;

        AbstractDatasourceCapacityPoliciesServerSetup(boolean xa) {
            this.xa = xa;
        }

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            CapacityConfiguration configuration = new CapacityConfiguration(
                    "org.jboss.jca.core.connectionmanager.pool.capacity.MinPoolSizeDecrementer",
                    "org.jboss.jca.core.connectionmanager.pool.capacity.MaxPoolSizeIncrementer");
            createDatasource(configuration);

        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            removeDatasource();
        }

        private void createDatasource(CapacityConfiguration capacityConfiguration) throws Exception {
            ModelNode addOperation = new ModelNode();
            addOperation.get(OP).set(ADD);
            addOperation.get(OP_ADDR).set(xa ? XA_DS_ADDRESS : DS_ADDRESS);
            addOperation.get("jndi-name").set(DS_JNDI_NAME);
            addOperation.get("driver-name").set("h2");
            addOperation.get("statistics-enabled").set("true");
            addOperation.get("enabled").set("false");
            addOperation.get("min-pool-size").set(2);
            addOperation.get("max-pool-size").set(5);
            addOperation.get("user-name").set("sa");
            addOperation.get("password").set("sa");
            if (!xa) { addOperation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"); }

            if (capacityConfiguration != null) {
                // add capacity-decrementer-class
                if (capacityConfiguration.getCapacityDecrementerClass() != null) {
                    addOperation.get("capacity-decrementer-class").set(capacityConfiguration.getCapacityDecrementerClass());

                    if (!capacityConfiguration.getCapacityDecrementerProperties().isEmpty()) {
                        Map<String, String> properties = capacityConfiguration.getCapacityDecrementerProperties();
                        for (String key : properties.keySet()) {
                            ModelNode props = new ModelNode();
                            props.add(key, properties.get(key));
                            addOperation.get("capacity-incrementer-properties").set(props);
                        }
                    }
                }

                // add capacity-incrementer-class
                if (capacityConfiguration.getCapacityIncrementerClass() != null) {
                    addOperation.get("capacity-incrementer-class").set(capacityConfiguration.getCapacityIncrementerClass());

                    if (!capacityConfiguration.getCapacityIncrementerProperties().isEmpty()) {
                        Map<String, String> properties = capacityConfiguration.getCapacityIncrementerProperties();
                        for (String key : properties.keySet()) {
                            ModelNode props = new ModelNode();
                            props.add(key, properties.get(key));
                            addOperation.get("capacity-incrementer-properties").set(props);
                        }
                    }
                }
            }

            executeOperation(addOperation);

            if (xa) {
                ModelNode xaDatasourcePropertiesAddress = XA_DS_ADDRESS.clone();
                xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
                xaDatasourcePropertiesAddress.protect();
                ModelNode xaDatasourcePropertyOperation = new ModelNode();
                xaDatasourcePropertyOperation.get(OP).set("add");
                xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
                xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

                executeOperation(xaDatasourcePropertyOperation);
            }

            writeAttribute(xa ? XA_DS_ADDRESS : DS_ADDRESS, ENABLED, "true");
            reload();
        }

        private void removeDatasource() throws Exception {
            if (xa) {
                remove(XA_DS_ADDRESS);
            } else {
                remove(DS_ADDRESS);
            }
            reload();
        }
    }

    static class CapacityConfiguration {
        private String capacityDecrementerClass;
        private Map<String, String> capacityDecrementerProperties;
        private String capacityIncrementerClass;
        private Map<String, String> capacityIncrementerProperties;

        CapacityConfiguration(String capacityDecrementerClass, String capacityIncrementerClass) {
            this.capacityDecrementerProperties = new HashMap<>();
            this.capacityIncrementerProperties = new HashMap<>();
            this.capacityDecrementerClass = capacityDecrementerClass;
            this.capacityIncrementerClass = capacityIncrementerClass;
        }

        void addCapacityDecrementerProperty(String name, String value) {
            if (capacityDecrementerClass == null) { throw new IllegalStateException("capacityDecrementerClass isn't set"); }
            if (name == null) { throw new NullPointerException("name"); }
            if (value == null) { throw new NullPointerException("value"); }
            capacityDecrementerProperties.put(name, value);
        }

        void addCapacityIncrementerProperty(String name, String value) {
            if (capacityIncrementerClass == null) { throw new IllegalStateException("capacityIncrementerClass isn't set"); }
            if (name == null) { throw new NullPointerException("name"); }
            if (value == null) { throw new NullPointerException("value"); }
            capacityIncrementerProperties.put(name, value);
        }

        String getCapacityDecrementerClass() {
            return capacityDecrementerClass;
        }

        Map<String, String> getCapacityDecrementerProperties() {
            return capacityDecrementerProperties;
        }

        String getCapacityIncrementerClass() {
            return capacityIncrementerClass;
        }

        Map<String, String> getCapacityIncrementerProperties() {
            return capacityIncrementerProperties;
        }
    }
}
