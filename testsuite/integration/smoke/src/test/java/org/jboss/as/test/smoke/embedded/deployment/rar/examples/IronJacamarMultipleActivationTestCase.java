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

import static org.junit.Assert.assertNotNull;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleConnectionFactory1;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleAdminObject2;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleConnectionFactory2;
import org.jboss.as.test.smoke.embedded.deployment.rar.MultipleConnectionFactory2Impl;
import org.jboss.as.test.smoke.embedded.deployment.rar.AS7_1452.ConfigPropertyConnectionFactory;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.*;
import org.junit.runner.RunWith;
import org.jboss.shrinkwrap.api.asset.StringAsset;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *        JBQA-5736 -IronJacamar deployment test
 */
@RunWith(Arquillian.class)
@Ignore("AS7-3249")
public class IronJacamarMultipleActivationTestCase extends AbstractMgmtTestBase {
	
    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
   @Deployment(order=2)
    public static ResourceAdapterArchive createDeployment()  throws Exception{
        String deploymentName = "archive_ij1.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
         JavaArchive ja = ShrinkWrap.create(JavaArchive.class,  "multiple2.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).
        addClasses(IronJacamarMultipleActivationTestCase.class,AbstractMgmtTestBase.class,MgmtOperationException.class);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource("rar/" + deploymentName + "/META-INF/ra.xml", "ra.xml")
        .addAsManifestResource("rar/" + deploymentName + "/META-INF/ironjacamar.xml", "ironjacamar.xml")
        .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),"MANIFEST.MF");;
        return raa; 
    }
   @Deployment(order=1,name="second")
   public static ResourceAdapterArchive newDeployment()  throws Exception{
	  // ctx=new InitialContext();
       String deploymentName = "archive_ij2.rar";

       ResourceAdapterArchive raa =
               ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class,  "multiple1.jar");
       ja.addPackage(MultipleConnectionFactory1.class.getPackage());
       raa.addAsLibrary(ja);

       raa.addAsManifestResource("rar/" + deploymentName + "/META-INF/ra.xml", "ra.xml")
       .addAsManifestResource("rar/" + deploymentName + "/META-INF/ironjacamar.xml", "ironjacamar.xml")
       .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),"MANIFEST.MF");;
       return raa; 
   }
   
   @Resource(mappedName = "java:jboss/D1CF1")
   private MultipleConnectionFactory1 connectionFactory1;

   @Resource(mappedName = "java:jboss/D1CF2")
   private MultipleConnectionFactory2 connectionFactory2;

   @Resource(mappedName="java:jboss/D1AO1")
   private MultipleAdminObject1 adminObject1;
   
   @Resource(mappedName="java:jboss/D1AO2")
   private MultipleAdminObject2 adminObject2; 

   @Resource(mappedName = "java:jboss/D2CF1")
   private MultipleConnectionFactory1 connectionFactory3;

   @Resource(mappedName = "java:jboss/D2CF2")
   private MultipleConnectionFactory2 connectionFactory4;

   @Resource(mappedName="java:jboss/D2AO1")
   private MultipleAdminObject1 adminObject3;
   
   @Resource(mappedName="java:jboss/D2AO2")
   private MultipleAdminObject2 adminObject4; 
   
  // @Resource
  // private static Context ctx;
   
    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
   @Test 
   public void testConfiguration1() throws Throwable {
   	assertNotNull("D1CF1 not found",connectionFactory1);
   }   
   @Test 
   public void testConfiguration11() throws Throwable {
   	MultipleConnectionFactory1 connectionFactory=(MultipleConnectionFactory1)new InitialContext().lookup("java:jboss/D1CF1");
   	assertNotNull("D1CF1 not found by lookup",connectionFactory);
   }
   @Test 
   public void testConfiguration2() throws Throwable {
   	assertNotNull("D1CF2 not found",connectionFactory2);
   }   
   @Test 
   public void testConfiguration21() throws Throwable {
   	MultipleConnectionFactory2 connectionFactory=(MultipleConnectionFactory2)new InitialContext().lookup("java:jboss/D1CF2");
   	assertNotNull("D1CF2 not found by lookup",connectionFactory);
   }
   @Test 
   public void testConfiguration3() throws Throwable {
   	assertNotNull("D2CF1 not found",connectionFactory3);
   }   
   @Test 
   public void testConfiguration31() throws Throwable {
   	MultipleConnectionFactory1 connectionFactory=(MultipleConnectionFactory1)new InitialContext().lookup("java:jboss/D2CF1");
   	assertNotNull("D2CF1 not found by lookup",connectionFactory);
   }
   @Test 
   public void testConfiguration4() throws Throwable {
   	assertNotNull("D2CF2 not found",connectionFactory4);
   }   
   @Test 
   public void testConfiguration41() throws Throwable {
   	MultipleConnectionFactory2 connectionFactory=(MultipleConnectionFactory2)new InitialContext().lookup("java:jboss/D2CF2");
   	assertNotNull("D2CF2 not found by lookup",connectionFactory);
   }
   @Test 
   public void testConfiguration5() throws Throwable {
   	assertNotNull("D1AO1 not found",adminObject1);
   }   
   @Test 
   public void testConfiguration51() throws Throwable {
   	MultipleAdminObject1 object=(MultipleAdminObject1)new InitialContext().lookup("java:jboss/D1AO1");
   	assertNotNull("D1AO1 not found by lookup",object);
   }
   @Test 
   public void testConfiguration6() throws Throwable {
   	assertNotNull("D1AO2 not found",adminObject2);
   }   
   @Test 
   public void testConfiguration61() throws Throwable {
   	MultipleAdminObject2 object=(MultipleAdminObject2)new InitialContext().lookup("java:jboss/D1AO2");
   	assertNotNull("D1AO2 not found by lookup",object);
   }
   @Test 
   public void testConfiguration7() throws Throwable {
   	assertNotNull("D2AO1 not found",adminObject3);
   }   
   @Test 
   public void testConfiguration71() throws Throwable {
   	MultipleAdminObject1 object=(MultipleAdminObject1)new InitialContext().lookup("java:jboss/D2AO1");
   	assertNotNull("D2AO1 not found by lookup",object);
   }
   @Test 
   public void testConfiguration8() throws Throwable {
   	assertNotNull("D2AO2 not found",adminObject4);
   }   
   @Test 
   public void testConfiguration81() throws Throwable {
   	MultipleAdminObject2 object=(MultipleAdminObject2)new InitialContext().lookup("java:jboss/D2AO2");
   	assertNotNull("D2AO2 not found by lookup",object);
   }
    
}
