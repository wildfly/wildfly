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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.ResourceAdapterSubsystemParser;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import javax.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.*;
import org.junit.runner.RunWith;
import org.jboss.dmr.*;

import java.util.List;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5740 multiple resources deployment
 */
@RunWith(Arquillian.class)
public class MultipleActivationTestCase extends AbstractMgmtTestBase {

	//@BeforeClass - called from @Deployment
	public static void setUp() throws Exception{
		initModelControllerClient("localhost",9999);
	    String xml=readXmlResource(System.getProperty("jbossas.ts.submodule.dir")+"/src/test/resources/config/simple.xml");
        List<ModelNode> operations=XmlToModelOperations(xml,Namespace.CURRENT.getUriString(),new ResourceAdapterSubsystemParser());
        executeOperation(operationListToCompositeOperation(operations));


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
   @Deployment
    public static ResourceAdapterArchive createDeployment()  throws Exception{
    	setUp();

        String deploymentName = "archive.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
         JavaArchive ja = ShrinkWrap.create(JavaArchive.class,  "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).
        addClasses(MultipleActivationTestCase.class,AbstractMgmtTestBase.class,MgmtOperationException.class,XMLElementReader.class,XMLElementWriter.class);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource("rar/" + deploymentName + "/META-INF/ra.xml", "ra.xml")
        .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),"MANIFEST.MF");;
        return raa;
    }

   @Resource(mappedName = "java:jboss/name1")
   private MultipleConnectionFactory1 connectionFactory1;

   @Resource(mappedName = "java:jboss/name2")
   private MultipleConnectionFactory1 connectionFactory2;

   @Resource(mappedName="java:jboss/Name3")
   private MultipleAdminObject1 adminObject1;

   @Resource(mappedName="java:jboss/Name4")
   private MultipleAdminObject1 adminObject2;

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
    	assertNotNull("CF1 not found",connectionFactory1);
    	assertNotNull("CF2 not found",connectionFactory2);
    	assertNotNull("AO1 not found",adminObject1);
    	assertNotNull("AO2 not found",adminObject2);
    	assertEquals("not equal AOs",adminObject1, adminObject2);
    }

}
