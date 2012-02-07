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

import static org.junit.Assert.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.test.smoke.embedded.deployment.RaServlet;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;


import org.jboss.shrinkwrap.api.asset.StringAsset;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5828 RAR inside EAR
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarDeploymentTestCase extends AbstractMgmtTestBase {

	@ArquillianResource
    private ManagementClient managementClient;
	
	static String subdeploymentName = "complex_ij.rar";
	static String deploymentName ="new.ear";
	
	
	public static void setUp() throws Exception{
		initModelControllerClient("localhost",9999);
	}
	@AfterClass
	public static void tearDown() throws Exception{
        closeModelControllerClient();
	}
    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
   @Deployment
    public static EnterpriseArchive createDeployment()  throws Exception{
	   setUp(); 
	  

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, subdeploymentName);
         JavaArchive ja = ShrinkWrap.create(JavaArchive.class,  "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage());
        raa.addAsManifestResource("rar/" + subdeploymentName + "/META-INF/ironjacamar.xml", "ironjacamar.xml")
        .addAsManifestResource("rar/" + subdeploymentName + "/META-INF/ra.xml", "ra.xml")
        .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,javax.inject.api,org.jboss.as.connector\n"),"MANIFEST.MF");;

        WebArchive wa = ShrinkWrap.create(WebArchive.class, "servlet.war");
        wa.addClasses(RaServlet.class);
        
        EnterpriseArchive ea=ShrinkWrap.create(EnterpriseArchive.class,deploymentName);
        ea.addAsModule(raa)
        .addAsModule(wa)
        .addAsLibrary(ja);
        return ea; 
    }

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test 
    public void testWebConfiguration() throws Throwable {
        URL servletURL = new URL("http://localhost:8080/servlet"+RaServlet.URL_PATTERN);
        BufferedReader br = new BufferedReader(new InputStreamReader(servletURL.openStream()));
        String message = br.readLine();
        assertEquals(RaServlet.SUCCESS, message);
    } 
    @Test
    @Ignore
    public void testConfiguration() throws Throwable{
        assertNotNull("Deployment metadata for ear not found",managementClient.getDeploymentMetaData(deploymentName));
        
        final ModelNode address=new ModelNode();
        address.add("deployment",deploymentName).add("subdeployment",subdeploymentName).add("subsystem","resource-adapters");
        address.protect();
        final ModelNode snapshot=new ModelNode();
        snapshot.get(OP).set("read-resource");
        snapshot.get("recursive").set("true");
        snapshot.get(OP_ADDR).set(address);
        executeOperation(snapshot);
    }
}
