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
package org.jboss.as.test.smoke.embedded.deployment.rar.examples;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.ResourceAdapterSubsystemParser;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.smoke.embedded.demos.fakejndi.FakeJndi;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleConnectionFactory1;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;
import java.util.List;
import java.util.Properties;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertNotNull;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5841 test for RA activation
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AfterResourceCreationDeploymentTestCase extends AbstractMgmtTestBase {

    static String deploymentName = "basic-after.rar";


	//@BeforeClass - called from @Test to create resources after deploymnet

	public void setUp() throws Exception{

        initModelControllerClient("localhost",9999);
        String xml=readXmlResource(System.getProperty("jbossas.ts.submodule.dir")+"/src/test/resources/config/basic-after.xml");
        List<ModelNode> operations=XmlToModelOperations(xml,Namespace.CURRENT.getUriString(),new ResourceAdapterSubsystemParser());
        executeOperation(operationListToCompositeOperation(operations));

        //since it is created after deployment it needs activation
        final ModelNode address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.add("resource-adapter", deploymentName);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("activate");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);

	}
	@AfterClass
	public static void tearDown() throws Exception{

		final ModelNode address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.add("resource-adapter",deploymentName);
        address.protect();

        remove(address);
        closeModelControllerClient();

	}

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
   @Deployment(name = "basic-after.rar", order = 2)
    public static ResourceAdapterArchive createDeployment()  throws Exception{


        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
         JavaArchive ja = ShrinkWrap.create(JavaArchive.class,  "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource("rar/" + deploymentName + "/META-INF/ra.xml", "ra.xml")
        .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),"MANIFEST.MF");;
        return raa;
    }

    @Deployment(name = "fakejndi.sar", order = 1)
    public static Archive<?> getDeployment() {
        initModelControllerClient("localhost", 9999);
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working
        return ShrinkWrapUtils.createJavaArchive("demos/fakejndi.sar", FakeJndi.class.getPackage());
    }


   @Resource(mappedName="java:jboss/Name3")
   private MultipleAdminObject1 adminObject1;


    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {

        setUp();

        adminObject1 =lookup("java:jboss/after/Name3", MultipleAdminObject1.class);

        assertNotNull("AO1 not found",adminObject1);
    }


    private static <T> T lookup(String name, Class<T> expected) throws Exception {
            //TODO Don't do this FakeJndi stuff once we have remote JNDI working

            MBeanServerConnection mbeanServer = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:remoting-jmx://127.0.0.1:9999")).getMBeanServerConnection();
            ObjectName objectName = new ObjectName("jboss:name=test,type=fakejndi");
            PollingUtils.retryWithTimeout(10000, new PollingUtils.WaitForMBeanTask(mbeanServer, objectName));
            Object o = mbeanServer.invoke(objectName, "lookup", new Object[] {name}, new String[] {"java.lang.String"});
            return expected.cast(o);
        }
}
