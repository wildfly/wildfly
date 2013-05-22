/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.smoke.deployment.rar.tests.configproperty;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.smoke.deployment.rar.configproperty.ConfigPropertyAdminObjectInterface;
import org.jboss.as.test.smoke.deployment.rar.configproperty.ConfigPropertyConnection;
import org.jboss.as.test.smoke.deployment.rar.configproperty.ConfigPropertyConnectionFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 *         Test case for AS7-1452: Resource Adapter config-property value passed incorrectly
 */
@RunWith(Arquillian.class)
@ServerSetup(ConfigPropertyTestCase.ConfigPropertyTestClassSetup.class)
public class ConfigPropertyTestCase extends ContainerResourceMgmtTestBase {

    /**
     * Class that performs setup before the deployment is deployed
     */
    static class ConfigPropertyTestClassSetup extends AbstractMgmtServerSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            final ModelNode address = new ModelNode();
            address.add("subsystem", "resource-adapters");
            address.add("resource-adapter", "as7_1452.rar");
            address.protect();

            final ModelNode operation = new ModelNode();
            operation.get(OP).set("add");
            operation.get(OP_ADDR).set(address);
            operation.get("archive").set("as7_1452.rar");
            operation.get("transaction-support").set("NoTransaction");
            executeOperation(operation);

            final ModelNode addressConfigRes = address.clone();
            addressConfigRes.add("config-properties", "Property");
            addressConfigRes.protect();

            final ModelNode operationConfigRes = new ModelNode();
            operationConfigRes.get(OP).set("add");
            operationConfigRes.get(OP_ADDR).set(addressConfigRes);
            operationConfigRes.get("value").set("A");
            executeOperation(operationConfigRes);

            final ModelNode addressAdmin = address.clone();
            addressAdmin.add("admin-objects", "java:jboss/ConfigPropertyAdminObjectInterface1");
            addressAdmin.protect();

            final ModelNode operationAdmin = new ModelNode();
            operationAdmin.get(OP).set("add");
            operationAdmin.get(OP_ADDR).set(addressAdmin);
            operationAdmin.get("class-name").set("org.jboss.as.test.smoke.deployment.rar.configproperty.ConfigPropertyAdminObjectImpl");
            operationAdmin.get("jndi-name").set(AO_JNDI_NAME);
            executeOperation(operationAdmin);

            final ModelNode addressConfigAdm = addressAdmin.clone();
            addressConfigAdm.add("config-properties", "Property");
            addressConfigAdm.protect();

            final ModelNode operationConfigAdm = new ModelNode();
            operationConfigAdm.get(OP).set("add");
            operationConfigAdm.get(OP_ADDR).set(addressConfigAdm);
            operationConfigAdm.get("value").set("C");
            executeOperation(operationConfigAdm);

            final ModelNode addressConn = address.clone();
            addressConn.add("connection-definitions", "java:jboss/ConfigPropertyConnectionFactory1");
            addressConn.protect();

            final ModelNode operationConn = new ModelNode();
            operationConn.get(OP).set("add");
            operationConn.get(OP_ADDR).set(addressConn);
            operationConn.get("class-name").set("org.jboss.as.test.smoke.deployment.rar.configproperty.ConfigPropertyManagedConnectionFactory");
            operationConn.get("jndi-name").set(CF_JNDI_NAME);
            operationConn.get("pool-name").set("ConfigPropertyConnectionFactory");
            executeOperation(operationConn);

            final ModelNode addressConfigConn = addressConn.clone();
            addressConfigConn.add("config-properties", "Property");
            addressConfigConn.protect();

            final ModelNode operationConfigConn = new ModelNode();
            operationConfigConn.get(OP).set("add");
            operationConfigConn.get(OP_ADDR).set(addressConfigConn);
            operationConfigConn.get("value").set("B");
            executeOperation(operationConfigConn);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode address = new ModelNode();
            address.add("subsystem", "resource-adapters");
            address.add("resource-adapter", "as7_1452.rar");
            address.protect();
            remove(address);
        }
    }


    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() throws Exception {

        String deploymentName = "as7_1452.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "as7_1452.jar");
        ja.addPackage("org.jboss.as.test.smoke.deployment.rar.configproperty")
                .addClasses(ConfigPropertyTestCase.class, MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(ConfigPropertyTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"), "MANIFEST.MF");

        return raa;
    }


    /**
     * CF
     */
    private static final String CF_JNDI_NAME = "java:jboss/ConfigPropertyConnectionFactory1";

    /**
     * AO
     */
    private static final String AO_JNDI_NAME = "java:jboss/ConfigPropertyAdminObjectInterface1";

    /**
     * Test config properties
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfigProperties() throws Throwable {

        Context ctx = new InitialContext();

        ConfigPropertyConnectionFactory connectionFactory = (ConfigPropertyConnectionFactory) ctx.lookup(CF_JNDI_NAME);

        assertNotNull(connectionFactory);

        ConfigPropertyAdminObjectInterface adminObject = (ConfigPropertyAdminObjectInterface) ctx.lookup(AO_JNDI_NAME);


        assertNotNull(adminObject);

        ConfigPropertyConnection connection = connectionFactory.getConnection();
        assertNotNull(connection);

        assertEquals("A", connection.getResourceAdapterProperty());
        assertEquals("B", connection.getManagedConnectionFactoryProperty());

        assertEquals("C", adminObject.getProperty());
        connection.close();
    }

}
