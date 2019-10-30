/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.cdi;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.as.test.integration.jca.ear.RarInsideEarReDeploymentTestCase;
import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the CDI request scope is active in MDB invocations.
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
@ServerSetup({ MDBRAScopeCdiIntegrationTestCase.JmsQueueSetup.class })
public class MDBRAScopeCdiIntegrationTestCase extends ContainerResourceMgmtTestBase {

    public static final String testDeploymentName = "test.jar";
    public static final String deploymentName = "test-ear.ear";
    public static final String subDeploymentName = "ear_packaged.rar";

    private ModelNode raAddress_subdeployment;
    private ModelNode raAddress_regular;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("mdb-cdi-test/queue", MDBProxy.QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("mdb-cdi-test/reply-queue", MDBProxy.REPLY_QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("mdb-cdi-test/queue");
                jmsAdminOperations.removeJmsQueue("mdb-cdi-test/reply-queue");
                jmsAdminOperations.close();
            }
        }
    }

    @Before
    public void setup() throws Exception {
        setupStandaloneRA();
        setupSubdeployedRA();
    }

    @Override
    protected void remove(ModelNode address) throws IOException, MgmtOperationException {
        ModelNode operation = Util.createRemoveOperation(PathAddress.pathAddress(address));
        operation.get(ModelDescriptionConstants.OPERATION_HEADERS, ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ManagementOperations.executeOperation(getModelControllerClient(), operation);
    }

    /**
     *
     */
    private void setupSubdeployedRA() throws Exception {
        // since it is created after deployment it needs activation
        raAddress_subdeployment = new ModelNode();
        raAddress_subdeployment.add("subsystem", "resource-adapters");
        raAddress_subdeployment.add("resource-adapter", deploymentName + "#" + subDeploymentName);
        raAddress_subdeployment.protect();
        setup(raAddress_subdeployment, true);
    }

    /**
     *
     */
    private void setupStandaloneRA() throws Exception {
        raAddress_regular = new ModelNode();
        raAddress_regular.add("subsystem", "resource-adapters");
        raAddress_regular.add("resource-adapter", subDeploymentName);
        raAddress_regular.protect();
        setup(raAddress_regular, false);
    }

    private void setup(final ModelNode address, final boolean activate) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        List<ModelNode> list = address.asList();
        operation.get("archive").set(list.get(list.size() - 1).get("resource-adapter").asString());
        executeOperation(operation);

        ModelNode addr = address.clone();
        addr.add("admin-objects", "Pool3");

        operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(addr);
        operation.get("jndi-name").set("java:jboss/exported/redeployed/Name3");
        operation.get("class-name").set("org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl");
        executeOperation(operation);
        if (activate) {
            operation = new ModelNode();
            operation.get(OP).set("activate");
            operation.get(OP_ADDR).set(address);
            executeOperation(operation);
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            remove(raAddress_subdeployment);
        } finally {
            remove(raAddress_regular);
        }
    }

    @Deployment(name = deploymentName, order = 1)
    public static EnterpriseArchive createEAR() throws Exception {

        ResourceAdapterArchive raa = createRAR();
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "xxx-ejbs.jar");
        ejbJar.addClasses(/* MDBRAScopeCdiIntegrationTestCase.class, */CdiIntegrationMDB.class, RequestScopedCDIBean.class,
                MDBProxy.class, MDBProxyBean.class, JMSMessagingUtil.class, JmsQueueSetup.class).addPackage(
                JMSOperations.class.getPackage());
        ejbJar.addAsManifestResource(new StringAsset(
                "Dependencies: org.jboss.as.controller-client, org.jboss.as.controller, org.jboss.dmr \n"), "MANIFEST.MF");
        ejbJar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, deploymentName);
        ear.addAsModule(raa);
        ear.addAsModule(ejbJar);
        return ear;
    }

    @Deployment(name = subDeploymentName, order = 2)
    public static ResourceAdapterArchive createRAR() throws Exception {

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, subDeploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(MultipleAdminObject1.class.getPackage()).addClasses(MgmtOperationException.class, XMLElementReader.class,
                XMLElementWriter.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(RarInsideEarReDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client, org.jboss.as.controller, org.jboss.dmr,org.jboss.as.cli\n"),
                        "MANIFEST.MF");

        return raa;
    }

    @Deployment(name = testDeploymentName, order = 3)
    public static JavaArchive createTestDeployment() throws Exception {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "test.jar");
        testJar.addClass(MDBRAScopeCdiIntegrationTestCase.class);
        return testJar;
    }

    private static String getEJBJNDIBinding() {

        final String appName = "test-ear";
        final String moduleName = "xxx-ejbs";
        final String beanName = MDBProxyBean.class.getSimpleName();
        final String viewName = MDBProxy.class.getName();

        return "ejb:" + appName + "/" + moduleName + "/" + beanName + "!" + viewName;
    }

    @Test
    public void testMe() throws Exception {
        Assert.assertNotNull(getModelControllerClient());
        try {
            // deployer.deploy(deploymentName);

            MDBProxy mdbProxy = (MDBProxy) getInitialContext().lookup(getEJBJNDIBinding());
            mdbProxy.trigger();
        } finally {
            try {

            } finally {
                // deployer.undeploy(deploymentName);
            }
        }
    }

    private static InitialContext getInitialContext() throws NamingException {
        final Hashtable env = new Hashtable();
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":" + 8080);
        return new InitialContext(env);
    }
}
