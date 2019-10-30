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
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.fail;

import java.lang.reflect.ReflectPermission;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.io.FilePermission;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;
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
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.adapters.jdbc.WrapperDataSource;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPool;
import org.jboss.remoting3.security.RemotingPermission;
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
    private final ModelNode statisticsAddress;
    private final boolean xaDatasource;

    static {
        DS_ADDRESS.protect();
        XA_DS_ADDRESS.protect();
    }

    public AbstractDatasourceCapacityPoliciesTestCase(boolean xaDatasource) {
        this.xaDatasource = xaDatasource;
        statisticsAddress = xaDatasource ? XA_DS_ADDRESS.clone() : DS_ADDRESS.clone();
        statisticsAddress.add("statistics", "pool");
        statisticsAddress.protect();
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
                JcaTestsUtil.class,
                TimeoutUtil.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: javax.inject.api,org.jboss.as.connector," +
                "org.jboss.as.controller,org.jboss.dmr,org.jboss.as.cli,org.jboss.staxmapper," +
                "org.jboss.ironjacamar.impl, org.jboss.ironjacamar.jdbcadapters,org.jboss.remoting\n"), "MANIFEST.MF");
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read"),
                new PropertyPermission("ts.timeout.factor", "read"),
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks")
        ), "permissions.xml");

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

        // wait until IJ PoolFiller and CapacityFiller fill pool with expected number of connections,
        // also remember initial destroyedCount, IJ fills pool with two threads (CapacityFiller and PoolFiller)
        // and it can result in one connection created and immediately destroyed because pool has been already filled
        // by other thread, for details see https://issues.jboss.org/browse/JBJCA-1344
        int initialDestroyedCount = waitForPool(2000);

        checkStatistics(4, 1, 5, initialDestroyedCount);

        connections[1] = ds.getConnection();
        checkStatistics(3, 2, 5, initialDestroyedCount);

        connections[2] = ds.getConnection();
        checkStatistics(2, 3, 5, initialDestroyedCount);

        connections[3] = ds.getConnection();
        checkStatistics(1, 4, 5, initialDestroyedCount);

        for (int i = 0; i < 4; i++) {
            Connection c = connections[i];
            c.close();
        }

        WrapperDataSource wsds = JcaTestsUtil.extractWrapperDatasource((WildFlyDataSource) ds);
        ManagedConnectionPool mcp = JcaTestsUtil.extractManagedConnectionPool(wsds);
        JcaTestsUtil.callRemoveIdleConnections(mcp);

        checkStatistics(5, 0, 2, initialDestroyedCount + 3);
    }

    private void checkStatistics(int expectedAvailableCount, int expectedInUseCount,
                                 int expectedActiveCount, int expectedDestroyedCount) throws Exception {

        int availableCount = readStatisticsAttribute("AvailableCount");
        int inUseCount = readStatisticsAttribute("InUseCount");
        int activeCount = readStatisticsAttribute("ActiveCount");
        int destroyedCount = readStatisticsAttribute("DestroyedCount");

        Assert.assertEquals("Unexpected AvailableCount", expectedAvailableCount, availableCount);
        Assert.assertEquals("Unexpected InUseCount", expectedInUseCount, inUseCount);
        Assert.assertEquals("Unexpected ActiveCount", expectedActiveCount, activeCount);
        Assert.assertEquals("Unexpected DestroyedCount", expectedDestroyedCount, destroyedCount);
    }

    private int readStatisticsAttribute(final String attributeName) throws Exception {
        return readAttribute(statisticsAddress, attributeName).asInt();
    }

    private int waitForPool(final int timeout) throws Exception {
        long waitTimeout = TimeoutUtil.adjust(timeout);
        long sleep = 50L;
        while (true) {
            int availableCount = readStatisticsAttribute("AvailableCount");
            int inUseCount = readStatisticsAttribute("InUseCount");
            int activeCount = readStatisticsAttribute("ActiveCount");

            if (availableCount == 4 && inUseCount == 1 && activeCount == 5)
                return readStatisticsAttribute("DestroyedCount");
            TimeUnit.MILLISECONDS.sleep(sleep);

            waitTimeout -= sleep;
            if (waitTimeout <= 0) {
                fail("Pool hasn't been filled with expected connections within specified timeout");
            }
        }
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
            if (!xa) { addOperation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"); }

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
