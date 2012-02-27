/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.loginmodules;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.PrincipalPrintingServlet;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.IdentityLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Tests of login via IdentityLoginModule
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IdentityLoginModuleTestCase {

   private static Logger log = Logger.getLogger(IdentityLoginModuleTestCase.class);

   private static final String DEP1 = "IdentityLoginModule-defaultPrincipal";

   /**
    * Test deployment with
    *  <module-option name="roles" value="role1,role2"/>
    */
   @Deployment(name = DEP1, order = 1)
   public static WebArchive appDeployment1() {
      log.info("start" + DEP1 + "deployment");

      WebArchive war = ShrinkWrap.create(WebArchive.class, DEP1 + ".war");
      war.addClass(PrincipalPrintingServlet.class);
      war.setWebXML(Utils.getResource("loginmodules/deployments/IdentityLoginModule/web.xml"));
      war.addAsWebInfResource(Utils.getResource("loginmodules/deployments/IdentityLoginModule/dep1/jboss-web.xml"),"jboss-web.xml");
      log.debug(war.toString(true));

      log.debug("adding module options");
      Map<String,String> moduleOptionsMap = new HashMap<String,String>();
      moduleOptionsMap.put("roles", "role1,role2");

      log.info("creating security domain: TestIdentityLoginDomain");
      Utils.createSecurityDomain("TestIdentityLoginDomain", "localhost", 9999, IdentityLoginModule.class, moduleOptionsMap);
      log.info("security domain created");

      return war;
   }

   private static final String DEP2 = "IdentityLoginModule-customPrincipal";

   /**
    * Test deployment with
    *  <module-option name="prinipal" value="SomeName"/>
    *  <module-option name="roles" value="role1,role2"/>
    */
   @Deployment(name = DEP2, order = 2)
   public static WebArchive appDeployment2() {
      log.info("start" + DEP2 + "deployment");

      WebArchive war = ShrinkWrap.create(WebArchive.class, DEP2 + ".war");
      war.addClass(PrincipalPrintingServlet.class);
      war.setWebXML(Utils.getResource("loginmodules/deployments/IdentityLoginModule/web.xml"));
      war.addAsWebInfResource(Utils.getResource("loginmodules/deployments/IdentityLoginModule/dep2/jboss-web.xml"),"jboss-web.xml");
      log.debug(war.toString(true));

      log.debug("adding module options");
      Map<String,String> moduleOptionsMap = new HashMap<String,String>();
      moduleOptionsMap.put("roles", "role1,role2");
      moduleOptionsMap.put("principal","SomeName");

      log.info("creating security domain: TestIdentityLoginDomain");
      Utils.createSecurityDomain("TestIdentityLoginDomain2","localhost", 9999, IdentityLoginModule.class, moduleOptionsMap);
      log.info("security domain created");

      return war;
   }

   @OperateOnDeployment(DEP1)
   @ArquillianResource
   URL URL1;

   /**
    * Tests assignment of default principal name to the caller
    */
   @OperateOnDeployment(DEP1)
   @Test
   public void testDefaultPrincipal(){

      DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response;
      HttpGet httpget = new HttpGet(URL1.toString());
      httpget.addHeader("Authorization", "Basic Yzpj");  //I'm not sure why this have to be here, however it does not work without it
      String text;

      try {
         response = httpclient.execute(httpget);
         text = Utils.getContent(response);
      } catch (IOException e) {
         throw new RuntimeException("Servlet response IO exception", e);         
      }

      assertTrue("default principal ('guest') not assigned to the request by IdentityLoinModule: returned text = " +
         text, text.contains("guest"));
   }

   @OperateOnDeployment(DEP2)
   @ArquillianResource
   URL URL2;

   /**
    * Tests assignment of custom principal name to the caller
    */
   @OperateOnDeployment(DEP2)
   @Test
   public void testCustomPrincipal(){

      DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response;
      //HttpGet httpget = new HttpGet("http://localhost:8080/" + DEP2 + "/");
      HttpGet httpget = new HttpGet(URL2.toString());
      httpget.addHeader("Authorization", "Basic Yzpj");//I'm not sure why this have to be here, however it does not work without it
      String text;

      try {
         response = httpclient.execute(httpget);
         text = Utils.getContent(response);
      } catch (IOException e) {
         throw new RuntimeException("Servlet response IO exception", e);
      }

      assertTrue("default principal ('guest') not assigned to the request by IdentityLoinModule: returned text = " +
         text, text.contains("SomeName"));
   }
}
