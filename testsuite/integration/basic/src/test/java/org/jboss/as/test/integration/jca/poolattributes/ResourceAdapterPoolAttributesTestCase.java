/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
 *
 */

package org.jboss.as.test.integration.jca.poolattributes;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.util.List;

import javax.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.common.pool.Constants;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
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
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Checks that pool attributes can be set and do not require a reload.
 *
 * @author <a href="mailto:thofman@redhat.com">Tomas Hofman</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ResourceAdapterPoolAttributesTestCase.ResourceAdapterCapacityPoliciesServerSetupTask.class)
public class ResourceAdapterPoolAttributesTestCase extends JcaMgmtBase {
    private static final String RA_NAME = "pool-attributes-test.rar";
    private static final ModelNode RA_ADDRESS = new ModelNode().add(SUBSYSTEM, "resource-adapters")
            .add("resource-adapter", RA_NAME);
    private static final ModelNode CONNECTION_ADDRESS = RA_ADDRESS.clone().add("connection-definitions", "Lazy");

    static {
        RA_ADDRESS.protect();
        CONNECTION_ADDRESS.protect();
    }

    @Deployment
    public static Archive<?> createResourceAdapter() {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, RA_NAME);
        rar.addAsManifestResource(LazyResourceAdapter.class.getPackage(), "ra-notx.xml", "ra.xml");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "pool-attributes-test.jar");
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
                ResourceAdapterPoolAttributesTestCase.class,
                AbstractMgmtServerSetupTask.class, JcaMgmtServerSetupTask.class,
                AbstractMgmtTestBase.class,
                JcaMgmtBase.class,
                ContainerResourceMgmtTestBase.class,
                MgmtOperationException.class,
                ManagementOperations.class,
                JcaTestsUtil.class);

        rar.addAsManifestResource(new StringAsset("Dependencies: javax.inject.api,org.jboss.as.connector," +
                "org.jboss.as.controller,org.jboss.dmr,org.jboss.as.cli,org.jboss.staxmapper," +
                "org.jboss.ironjacamar.impl, org.jboss.ironjacamar.jdbcadapters,org.jboss.remoting\n"), "MANIFEST.MF");
        rar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");

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

    @Test
    public void testModifyPoolAttributes() throws Exception {
        PoolConfiguration poolConfiguration = JcaTestsUtil.exctractPoolConfiguration(lcf);

        // check initial values
        Assert.assertNotNull(poolConfiguration);
        Assert.assertEquals(2, poolConfiguration.getMinSize());
        Assert.assertEquals(5, poolConfiguration.getMaxSize());
        Assert.assertEquals(2, poolConfiguration.getInitialSize());
        Assert.assertEquals(30000, poolConfiguration.getBlockingTimeout());
        Assert.assertEquals(true, poolConfiguration.isFair());
        Assert.assertEquals(false, poolConfiguration.isStrictMin());

        // modify values
        writeAttribute(CONNECTION_ADDRESS, Constants.INITIAL_POOL_SIZE.getName(), "4");
        writeAttribute(CONNECTION_ADDRESS, Constants.BLOCKING_TIMEOUT_WAIT_MILLIS.getName(), "10000");
        writeAttribute(CONNECTION_ADDRESS, Constants.POOL_FAIR.getName(), "false");
        writeAttribute(CONNECTION_ADDRESS, Constants.POOL_USE_STRICT_MIN.getName(), "true");

        // check that server is not in reload-required state
        ModelNode serverState = readAttribute(new ModelNode(), "server-state");
        Assert.assertEquals("running", serverState.asString());
        // check that runtime was updated
        Assert.assertEquals(4, poolConfiguration.getInitialSize());
        Assert.assertEquals(10000, poolConfiguration.getBlockingTimeout());
        Assert.assertEquals(false, poolConfiguration.isFair());
        Assert.assertEquals(true, poolConfiguration.isStrictMin());

        writeAttribute(CONNECTION_ADDRESS, Constants.MIN_POOL_SIZE.getName(), "4");
        writeAttribute(CONNECTION_ADDRESS, Constants.MAX_POOL_SIZE.getName(), "10");

        // check that server is in reload-required state
        serverState = readAttribute(new ModelNode(), "server-state");
        Assert.assertEquals("reload-required", serverState.asString());

        // check that runtime was updated
        Assert.assertEquals(4, poolConfiguration.getMinSize());
        Assert.assertEquals(10, poolConfiguration.getMaxSize());
    }

    static class ResourceAdapterCapacityPoliciesServerSetupTask extends JcaMgmtServerSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            String xml = FileUtils.readFile(ResourceAdapterPoolAttributesTestCase.class, "ra-def.xml");
            List<ModelNode> operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_1.getUriString(), new ResourceAdapterSubsystemParser());
            executeOperation(operationListToCompositeOperation(operations));
            reload();
        }
    }


}
