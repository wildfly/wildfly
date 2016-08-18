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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;
import javax.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaTestsUtil;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnection;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnectionFactory;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnectionFactoryImpl;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnectionImpl;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyLocalTransaction;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyManagedConnection;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyManagedConnectionFactory;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyManagedConnectionMetaData;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyResourceAdapter;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyXAResource;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPool;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for JCA capacity policies JBJCA-986 using resource adapter
 * *
 *
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ResourceAdapterCapacityPoliciesTestCase.ResourceAdapterCapacityPoliciesServerSetupTask.class)
public class ResourceAdapterCapacityPoliciesTestCase extends JcaMgmtBase {
    protected static final String RA_NAME = "capacity-policies-test.rar";
    protected static final ModelNode RA_ADDRESS = new ModelNode().add(SUBSYSTEM, "resource-adapters")
            .add("resource-adapter", RA_NAME);

    static {
        RA_ADDRESS.protect();
    }

    @Deployment
    public static Archive<?> createResourceAdapter() {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "capacity-policies-test.rar");
        rar.addAsManifestResource(LazyResourceAdapter.class.getPackage(), "ra-notx.xml", "ra.xml");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "capacity-policies-test.jar");
        jar.addClass(LazyResourceAdapter.class)
                .addClass(LazyManagedConnectionFactory.class)
                .addClass(LazyManagedConnection.class)
                .addClass(LazyConnection.class)
                .addClass(LazyConnectionImpl.class)
                .addClass(LazyXAResource.class)
                .addClass(LazyLocalTransaction.class)
                .addClass(LazyManagedConnectionMetaData.class)
                .addClass(LazyConnectionFactory.class)
                .addClass(LazyConnectionFactoryImpl.class);

        jar.addClasses(
                ResourceAdapterCapacityPoliciesTestCase.class,
                AbstractMgmtServerSetupTask.class,
                AbstractMgmtTestBase.class,
                JcaMgmtBase.class,
                ContainerResourceMgmtTestBase.class,
                MgmtOperationException.class,
                ManagementOperations.class,
                JcaTestsUtil.class);

        rar.addAsManifestResource(new StringAsset("Dependencies: javax.inject.api,org.jboss.as.connector," +
                "org.jboss.as.controller,org.jboss.dmr,org.jboss.as.cli,org.jboss.staxmapper," +
                "org.jboss.ironjacamar.impl, org.jboss.ironjacamar.jdbcadapters\n"), "MANIFEST.MF");

        rar.addAsLibrary(jar);
        return rar;
    }

    @Resource(mappedName = "java:/eis/Lazy")
    private LazyConnectionFactory lcf;

    @ArquillianResource
    private ManagementClient managementClient;

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
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

        LazyConnection[] connections = new LazyConnection[4];
        connections[0] = lcf.getConnection();

        // sometimes InUseCount is 2 and AvailableCount is 3 when statistics are checked right after
        // ds.getConnection, hence this sleep. I guess it's caused by CapacityFiller
        Thread.sleep(50);

        checkStatistics(4, 1, 5, 0);

        connections[1] = lcf.getConnection();
        checkStatistics(3, 2, 5, 0);

        connections[2] = lcf.getConnection();
        checkStatistics(2, 3, 5, 0);

        connections[3] = lcf.getConnection();
        checkStatistics(1, 4, 5, 0);

        for (int i = 0; i < 4; i++) {
            LazyConnection c = connections[i];
            c.close();
        }

        ManagedConnectionPool mcp = JcaTestsUtil.extractManagedConnectionPool(lcf);
        JcaTestsUtil.callRemoveIdleConnections(mcp);

        checkStatistics(5, 0, 2, 3);
    }

    private void checkStatistics(int expectedAvailableCount, int expectedInUseCount,
                                 int expectedActiveCount, int expectedDestroyedCount) throws Exception {
        // /subsystem=resource-adapters/resource-adapter=capacity-policies-test.rar ...
        // .../connection-definitions=Lazy/statistics=pool:read-resource(include-runtime=true
        ModelNode statsAddress = RA_ADDRESS.clone();
        statsAddress.add("connection-definitions", "Lazy")
                .add("statistics", "pool");
        statsAddress.protect();

        int availableCount = readAttribute(statsAddress, "AvailableCount").asInt();
        int inUseCount = readAttribute(statsAddress, "InUseCount").asInt();
        int activeCount = readAttribute(statsAddress, "ActiveCount").asInt();
        int destroyedCount = readAttribute(statsAddress, "DestroyedCount").asInt();

        Assert.assertEquals("Unexpected AvailableCount", expectedAvailableCount, availableCount);
        Assert.assertEquals("Unexpected InUseCount", expectedInUseCount, inUseCount);
        Assert.assertEquals("Unexpected ActiveCount", expectedActiveCount, activeCount);
        Assert.assertEquals("Unexpected DestroyedCount", expectedDestroyedCount, destroyedCount);
    }

    static class ResourceAdapterCapacityPoliciesServerSetupTask extends AbstractMgmtServerSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            String xml = FileUtils.readFile(ResourceAdapterCapacityPoliciesTestCase.class, "ra-def.xml");
            List<ModelNode> operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_1.getUriString(), new ResourceAdapterSubsystemParser());
            executeOperation(operationListToCompositeOperation(operations));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            remove(RA_ADDRESS);
        }
    }


}
