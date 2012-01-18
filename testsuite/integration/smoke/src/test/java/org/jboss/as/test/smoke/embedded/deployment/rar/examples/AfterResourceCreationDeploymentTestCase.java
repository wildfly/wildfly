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
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleConnectionFactory1;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.impl.base.spec.JavaArchiveImpl;
import org.jboss.staxmapper.XMLElementReader;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.test.smoke.embedded.deployment.rar.examples.ResourceAdapterTestUtilities.XmlToRAModelOperations;
import static org.jboss.as.test.smoke.embedded.deployment.rar.examples.ResourceAdapterTestUtilities.operationListToCompositeOperation;
import static org.jboss.as.test.smoke.embedded.deployment.rar.examples.ResourceAdapterTestUtilities.readResource;
import static org.junit.Assert.assertNotNull;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5737 basic subsystem deployment
 */
@RunWith(Arquillian.class)
@Ignore
public class AfterResourceCreationDeploymentTestCase extends AbstractMgmtTestBase {


	//@BeforeClass - called from @Test to create resources after deploymnet
    //@Deployment(order=2)
	public JavaArchive setUpa() throws Exception{
        Thread.sleep(10000);
        initModelControllerClient("localhost",9999);
        String xml=readResource("../test-classes/config/basic.xml");
        List<ModelNode> operations=XmlToRAModelOperations(xml);
        executeOperation(operationListToCompositeOperation(operations));

        //since it is created after deployment it needs activation
        final ModelNode address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.add("resource-adapter", "archive.rar");
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("activate");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);
        return ShrinkWrap.create(JavaArchive.class,  "empty.jar");

	}
	@AfterClass
	public static void tearDown() throws Exception{

		final ModelNode address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.add("resource-adapter","archive.rar");
        address.protect();

        remove(address);
        closeModelControllerClient();

	}

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
   @Deployment()
    public static ResourceAdapterArchive createDeployment()  throws Exception{


        String deploymentName = "archive.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
         JavaArchive ja = ShrinkWrap.create(JavaArchive.class,  "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).
        addClasses(AfterResourceCreationDeploymentTestCase.class,AbstractMgmtTestBase.class,MgmtOperationException.class,
                ResourceAdapterTestUtilities.class, XMLElementReader.class, ResourceAdaptersExtension.class,
                ResourceAdaptersExtension.ResourceAdapterSubsystemParser.class);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource("rar/" + deploymentName + "/META-INF/ra.xml", "ra.xml")
        .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),"MANIFEST.MF");;
        return raa;
    }

   @Resource(mappedName = "java:jboss/name1")
   private MultipleConnectionFactory1 connectionFactory1;


   @Resource(mappedName="java:jboss/Name3")
   private MultipleAdminObject1 adminObject1;


    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        setUpa()                 ;

    	assertNotNull("CF1 not found",connectionFactory1);
    	assertNotNull("AO1 not found",adminObject1);
    }
}
