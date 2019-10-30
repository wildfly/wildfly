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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.sql.Connection;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.JcaTestsUtil;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.jca.DsMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Running tests on data-source in non-jta mode.
 *
 * @author <a href="mailto:lgao@redhat.com>Lin Gao</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(DatasourceNonCcmTestCase.DatasourceServerSetupTask.class)
public class DatasourceNonCcmTestCase extends JcaMgmtBase {

    private static final String NON_TX_DS_NAME = "NonJTADS";

    private static final String TX_DS_NAME = "JTADS";

    @ArquillianResource
    private ManagementClient managementClient;

    static class DatasourceServerSetupTask extends JcaMgmtServerSetupTask {
        boolean debug = false;

        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            ModelNode address = new ModelNode();
            address.add("subsystem", "jca");
            address.add("cached-connection-manager", "cached-connection-manager");

            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(address);
            operation.get(OP).set("read-attribute");
            operation.get("name").set("debug");
            ModelNode result = managementClient.getControllerClient().execute(operation);
            if (result.hasDefined("debug")) {
                debug = result.require("debug").asBoolean();
            }

            operation = new ModelNode();
            operation.get(OP_ADDR).set(address);
            operation.get(OP).set("write-attribute");
            operation.get("name").set("debug");
            operation.get("value").set("true");
            managementClient.getControllerClient().execute(operation);

            // set up a DS
            setupDs(managementClient, NON_TX_DS_NAME, false);
            setupDs(managementClient, TX_DS_NAME, true);

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

    @Resource(mappedName = "java:jboss/datasources/" + NON_TX_DS_NAME)
    private DataSource nonTXDS;

    @Resource(mappedName = "java:jboss/datasources/" + TX_DS_NAME)
    private DataSource txDS;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        jar.addClasses(
                DatasourceNonCcmTestCase.class,
                Datasource.class,
                JcaMgmtBase.class,
                ManagementOperations.class,
                ContainerResourceMgmtTestBase.class,
                AbstractMgmtTestBase.class,
                JcaMgmtServerSetupTask.class,
                MgmtOperationException.class,
                DsMgmtTestBase.class,
                JcaTestsUtil.class);
        jar.addAsManifestResource(new StringAsset(
                "Dependencies: javax.inject.api,org.jboss.as.connector," +
                    "org.jboss.as.controller, " +
                    "org.jboss.dmr,org.jboss.as.cli, " +
                    "org.jboss.staxmapper,  " +
                    "org.jboss.ironjacamar.impl, " +
                    "org.jboss.ironjacamar.jdbcadapters, " +
                    "org.jboss.remoting3\n"
        ), "MANIFEST.MF");

        jar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");

        return jar;
    }

    @Test
    public void testNonJTADS() throws Exception {
        Assert.assertNotNull(nonTXDS);

        Connection c1 = nonTXDS.getConnection();
        Assert.assertNotNull(c1);
        Assert.assertEquals(1, getNumberOfConnections(false));

        Connection c2 = nonTXDS.getConnection();
        Assert.assertNotNull(c2);
        Assert.assertEquals(2, getNumberOfConnections(false));

        c1.close();
        Assert.assertEquals(1, getNumberOfConnections(false));
        c2.close();
        Assert.assertEquals(0, getNumberOfConnections(false));
    }

    @Test
    public void testJTADS() throws Exception {
        Assert.assertNotNull(txDS);

        Connection c1 = txDS.getConnection();
        Assert.assertNotNull(c1);
        Assert.assertEquals(1, getNumberOfConnections(true));

        Connection c2 = txDS.getConnection();
        Assert.assertNotNull(c2);
        Assert.assertEquals(2, getNumberOfConnections(true));

        c1.close();
        Assert.assertEquals(1, getNumberOfConnections(true));
        c2.close();
        Assert.assertEquals(0, getNumberOfConnections(true));
    }

    private int getNumberOfConnections(boolean tx) throws Exception {
        ModelNode address = new ModelNode();
        address.add("subsystem", "jca");
        address.add("cached-connection-manager", "cached-connection-manager");
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set("get-number-of-connections");
        ModelNode result = managementClient.getControllerClient().execute(operation).get("result");
        ModelNode txNode = result.get("TX");
        ModelNode nonTxNode = result.get("NonTX");
        if (tx) {
            if (txNode.isDefined()) {
                return txNode.asInt();
            }
        } else {
            if (nonTxNode.isDefined()) {
                return nonTxNode.asInt();
            }
        }
        return 0;
    }

}
