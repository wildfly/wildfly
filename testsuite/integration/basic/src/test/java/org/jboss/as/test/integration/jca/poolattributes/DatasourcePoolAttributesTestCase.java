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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.common.pool.Constants;
import org.jboss.as.connector.subsystems.datasources.WildFlyDataSource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.JcaTestsUtil;
import org.jboss.as.test.integration.jca.datasource.Datasource;
import org.jboss.as.test.integration.jca.datasource.DatasourceNonCcmTestCase;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.adapters.jdbc.WrapperDataSource;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Checks that pool attributes can be set and do (not) require a reload.
 *
 * @author <a href="mailto:thofman@redhat.com">Tomas Hofman</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(DatasourcePoolAttributesTestCase.DatasourceServerSetupTask.class)
public class DatasourcePoolAttributesTestCase extends JcaMgmtBase {

    private static final String DS_NAME = "DS";
    private static final ModelNode DS_ADDRESS = new ModelNode().add(SUBSYSTEM, "datasources")
            .add("data-source", DS_NAME);

    static {
        DS_ADDRESS.protect();
    }

    @Deployment
    public static Archive<?> createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "deployment.jar");

        jar.addClasses(
                DatasourceNonCcmTestCase.class,
                Datasource.class,
                WildFlyDataSource.class,
                WrapperDataSource.class,
                JcaMgmtServerSetupTask.class,
                DatasourcePoolAttributesTestCase.class,
                AbstractMgmtServerSetupTask.class,
                AbstractMgmtTestBase.class,
                JcaMgmtBase.class,
                ContainerResourceMgmtTestBase.class,
                MgmtOperationException.class,
                ManagementOperations.class,
                JcaTestsUtil.class,
                ServerReload.class);

        jar.addAsManifestResource(new StringAsset("Dependencies: javax.inject.api," +
                "org.jboss.as.connector," +
                "org.jboss.as.controller," +
                "org.jboss.dmr," +
                "org.jboss.as.cli," +
                "org.jboss.remoting3," +
                "org.jboss.staxmapper," +
                "org.jboss.ironjacamar.api," +
                "org.jboss.ironjacamar.impl," +
                "org.jboss.ironjacamar.jdbcadapters\n"), "MANIFEST.MF");

        jar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("connect"),
                new RemotingPermission("createEndpoint"),
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")),
                "permissions.xml");

        return jar;
    }

    @Resource(mappedName = "java:jboss/datasources/" + DS_NAME)
    private DataSource datasource;

    @ArquillianResource
    private ManagementClient managementClient;

    @ArquillianResource
    private static ContainerController container;

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
    }

    /**
     * Checks that attributes not requiring reload can be set.
     */
    @Test
    public void testModifyNonReloadAttributes() throws Exception {
        WrapperDataSource wrapperDataSource = JcaTestsUtil.extractWrapperDatasource((WildFlyDataSource) datasource);
        PoolConfiguration poolConfiguration = JcaTestsUtil.exctractPoolConfiguration(wrapperDataSource);

        // check initial values
        Assert.assertNotNull(poolConfiguration);
        Assert.assertEquals(0, poolConfiguration.getInitialSize());
        Assert.assertEquals(30000, poolConfiguration.getBlockingTimeout());
        Assert.assertEquals(true, poolConfiguration.isFair());
        Assert.assertEquals(false, poolConfiguration.isStrictMin());

        // modify values
        writeAttribute(DS_ADDRESS, Constants.INITIAL_POOL_SIZE.getName(), "4");
        writeAttribute(DS_ADDRESS, Constants.BLOCKING_TIMEOUT_WAIT_MILLIS.getName(), "10000");
        writeAttribute(DS_ADDRESS, Constants.POOL_FAIR.getName(), "false");
        writeAttribute(DS_ADDRESS, Constants.POOL_USE_STRICT_MIN.getName(), "true");

        // check that server is not in reload-required state
        ModelNode serverState = readAttribute(new ModelNode(), "server-state");
        Assert.assertEquals("running", serverState.asString());
        // check that runtime was updated
        Assert.assertEquals(4, poolConfiguration.getInitialSize());
        Assert.assertEquals(10000, poolConfiguration.getBlockingTimeout());
        Assert.assertEquals(false, poolConfiguration.isFair());
        Assert.assertEquals(true, poolConfiguration.isStrictMin());
    }

    static class DatasourceServerSetupTask extends JcaMgmtServerSetupTask {

        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            setupDs(managementClient, DS_NAME, true);
            reload();
        }

        private void setupDs(ManagementClient managementClient, String dsName, boolean jta) throws Exception {
            Datasource ds = Datasource.Builder(dsName).build();
            ModelNode address = new ModelNode();
            address.add("subsystem", "datasources");
            address.add("data-source", dsName);

            ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("jndi-name").set(ds.getJndiName());
            operation.get("use-java-context").set("true");
            operation.get("driver-name").set(ds.getDriverName());
            operation.get("enabled").set("true");
            operation.get("user-name").set(ds.getUserName());
            operation.get("password").set(ds.getPassword());
            operation.get("jta").set(jta);
            operation.get("use-ccm").set("true");
            operation.get("connection-url").set(ds.getConnectionUrl());
            managementClient.getControllerClient().execute(operation);
        }
    }


}
