/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.loginmodules.common.CustomEjbAccessingLoginModule;
import org.jboss.as.test.integration.security.loginmodules.common.SimpleSecuredEJB;
import org.jboss.as.test.integration.security.loginmodules.common.SimpleSecuredEJBImpl;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.PrincipalPrintingServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.util.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.security.auth.spi.RunAsLoginModule;


import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.FLAG;
import static org.junit.Assert.assertTrue;

/**
 * This is a test case for RunAsLoginModule
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RunAsLoginModuleTestCase.SecurityDomainSetup.class)
public class RunAsLoginModuleTestCase {

   public static class SecurityDomainSetup extends AbstractSecurityDomainSetup {

       protected String getSecurityDomainName() {
           return "RunAsLoginModuleTest";
       }

       public void setup(ManagementClient managementClient, String containerId) throws Exception {

           List<ModelNode> updates = new ArrayList<ModelNode>();
           ModelNode op = new ModelNode();

           op.get(OP).set(ADD);
           op.get(OP_ADDR).add(SUBSYSTEM, "security");
           op.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());
           updates.add(op);

           op = new ModelNode();
           op.get(OP).set(ADD);
           op.get(OP_ADDR).add(SUBSYSTEM, "security");
           op.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());
           op.get(OP_ADDR).add(AUTHENTICATION, CLASSIC);

           ModelNode loginModule = op.get(LOGIN_MODULES).add();
           loginModule.get(CODE).set(RunAsLoginModule.class.getName());
           loginModule.get(FLAG).set("optional");
           ModelNode moduleOptions = loginModule.get("module-options");
           moduleOptions.get("roleName").set("RunAsLoginModuleRole");

           ModelNode loginModule2 = op.get(LOGIN_MODULES).add();
           loginModule2.get(CODE).set(CustomEjbAccessingLoginModule.class.getName());
           loginModule2.get(FLAG).set("required");

           op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

           updates.add(op);

           applyUpdates(managementClient.getControllerClient(), updates);

       }
   }

   private static Logger log = Logger.getLogger(RunAsLoginModuleTestCase.class);

   private static final String DEP1 = "RunAsLoginModule";

   /**
    * Test deployment
    */
   @Deployment(name = DEP1, order = 1)
   public static WebArchive appDeployment1() {
      log.info("start" + DEP1 + "deployment");

      WebArchive war = ShrinkWrap.create(WebArchive.class, DEP1 + ".war");
      war.addClass(PrincipalPrintingServlet.class);
      war.setWebXML(Utils.getResource("org/jboss/as/test/integration/security/loginmodules/deployments/RunAsLoginModule/web.xml"));
      war.addAsWebInfResource(Utils.getResource("org/jboss/as/test/integration/security/loginmodules/deployments/RunAsLoginModule/jboss-web.xml"),"jboss-web.xml");

      war.addClasses(SimpleSecuredEJB.class, SimpleSecuredEJBImpl.class, CustomEjbAccessingLoginModule.class);

      log.debug(war.toString(true));

      return war;
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP1)
   @Test
   public void testCleartextPassword1(@ArquillianResource URL url) throws Exception {
      DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response;
      log.debug("URL: " + url.toString());

      HttpGet httpget = new HttpGet(url.toString());
      String headerValue = Base64.encodeBytes("anil:anil".getBytes());
      Assert.assertNotNull(headerValue);
      httpget.addHeader("Authorization", "Basic " + headerValue);
      String text;

      try {
         response = httpclient.execute(httpget);
         text = Utils.getContent(response);
      } catch (IOException e) {
         throw new RuntimeException("Servlet response IO exception", e);
      }

      assertTrue("An unexpected response: " + text, text.contains("anil"));
   }

}
