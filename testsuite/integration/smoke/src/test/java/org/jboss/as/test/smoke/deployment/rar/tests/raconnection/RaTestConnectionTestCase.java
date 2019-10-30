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
package org.jboss.as.test.smoke.deployment.rar.tests.raconnection;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5967 test connection in pool
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RaTestConnectionTestCase.RaTestConnectionTestCaseSetup.class)
public class RaTestConnectionTestCase extends ContainerResourceMgmtTestBase {
    private static ModelNode address;
    private static String deploymentName = "testcon_mult.rar";

    static class RaTestConnectionTestCaseSetup extends AbstractMgmtServerSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            address = new ModelNode();
            address.add("subsystem", "resource-adapters");
            address.add("resource-adapter", deploymentName);
            address.protect();
            String xml = FileUtils.readFile(RaTestConnectionTestCase.class, "testcon_multiple.xml");
            List<ModelNode> operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_0.getUriString(), new ResourceAdapterSubsystemParser());
            executeOperation(operationListToCompositeOperation(operations));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
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
        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage());
        raa.addAsLibrary(ja);
        raa.addAsManifestResource(RaTestConnectionTestCase.class.getPackage(), "ra.xml", "ra.xml");
        return raa;
    }


    @Test
    public void testConnection() throws Exception {
        ModelNode testAddress = address.clone();
        testAddress.add("connection-definitions", "Pool1");
        ModelNode op = new ModelNode();
        op.get(OP).set("test-connection-in-pool");
        op.get(OP_ADDR).set(testAddress);
        assertTrue(executeOperation(op).asBoolean());

    }

    @Test
    public void flushConnections() throws Exception {
        ModelNode testAddress = address.clone();
        testAddress.add("connection-definitions", "Pool1");
        ModelNode op = new ModelNode();
        op.get(OP).set("flush-idle-connection-in-pool");
        op.get(OP_ADDR).set(testAddress);
        executeOperation(op);
    }
}
