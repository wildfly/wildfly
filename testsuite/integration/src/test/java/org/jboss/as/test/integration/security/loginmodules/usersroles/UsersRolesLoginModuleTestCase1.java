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
package org.jboss.as.test.integration.security.loginmodules.usersroles;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.security.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import java.io.IOException;
import java.io.OutputStream;
import java.lang.RuntimeException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.loginmodules.common.UnsecuredEJB;
import org.jboss.as.test.integration.security.loginmodules.common.UnsecuredEJBImpl;
import org.jboss.as.test.integration.security.loginmodules.common.UnsecuredServlet;
import org.jboss.as.test.integration.web.security.WebSecurityPasswordBasedBase;
import org.jboss.dmr.ModelNode;
import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Lanik
 * this class contains UsersRolesLoginModule tests with unauthenticatedIdentity property.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UsersRolesLoginModuleTestCase1 extends AbstractUsersRolesLoginModuleTest {

   protected final String URL = "http://localhost:8080/" + getContextPath() + "/unsecured/";
   protected static final String ANNONYMOUS_USER_NAME = "annonymous";

   /**
    * Base method to create a {@link org.jboss.shrinkwrap.api.spec.WebArchive}
    *
    * @param name         Name of the war file
    * @param servletClass a class that is the servlet
    * @param webxml       {@link java.net.URL} to the web.xml. This can be null
    * @return
    */
   public static WebArchive create(String name, Class<?> servletClass, URL webxml) {
      WebArchive war = ShrinkWrap.create(WebArchive.class, name);
      war.addClass(servletClass);

      ClassLoader tccl = Thread.currentThread().getContextClassLoader();

      war.addAsWebInfResource(tccl.getResource("users-roles-login-module.war/jboss-web.xml"), "jboss-web.xml");
      war.addClass(UsersRolesLoginModule.class);
      war.addClass(UnsecuredEJB.class);
      war.addClass(UnsecuredEJBImpl.class);

      if (webxml != null) {
         war.setWebXML(webxml);
      }

      OutputStream os = new OutputStream() {
         StringBuilder builder = new StringBuilder();

         @Override
         public void write(int b) throws IOException {
            builder.append((char) b);
         }

         public String toString() {
            return builder.toString();
         }
      };

      war.writeTo(os, Formatters.VERBOSE);

      return war;
   }

   @BeforeClass
   public static void before(){
      Map<String,String> userProps = new HashMap<String, String>();
      userProps.put("anil","anil");
      setUsersProperties(userProps);

      Map<String,String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil","gooduser");
      setRolesProperties(rolesProps);
   }


   @Deployment
   public static WebArchive deployment() {
      // FIXME hack to get things prepared before the deployment happens
      try {
         final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
         // create required security domains
         createSecurityDomains(client);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      WebArchive war = create("users-roles-login-module.war", UnsecuredServlet.class, null);
      WebSecurityPasswordBasedBase.printWar(war);


      return war;
   }


   @AfterClass
   public static void after() throws Exception {
      final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
      // remove test security domains
      removeSecurityDomains(client);

   }

   /**
    * This tests that annonymous caller gets a proper principal when an appropriate login-module option is set
    * Currently doesn't work from some reason. It is not known yet whether it is a test setup problem or a bug in the server
    * @throws Exception
    */
   @Ignore("Currently doesn't work from some reason. It is not known yet whether it is a test setup problem or a bug in the server")
   @Test
   public void testAnnonymousUserAuth() throws Exception {
      HttpResponse response;
      HttpGet httpget = new HttpGet(URL);

      DefaultHttpClient httpclient = new DefaultHttpClient();
      response = httpclient.execute(httpget);

      assertEquals(200, response.getStatusLine().getStatusCode());
      assertTrue(getContent(response).contains(ANNONYMOUS_USER_NAME));

   }

    protected static void createSecurityDomains(final ModelControllerClient client) throws Exception {
        List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();

        String securityDomain =  "users-roles-login-module";
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        op.get(OP_ADDR).add(Constants.AUTHENTICATION, Constants.CLASSIC);

        ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();
        loginModule.get(ModelDescriptionConstants.CODE).set(UsersRolesLoginModule.class.getName());
        loginModule.get(FLAG).set("required");
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        URL usersProp = tccl.getResource("users-roles-login-module.war/users.properties");
        URL rolesProp = tccl.getResource("users-roles-login-module.war/roles.properties");
        ModelNode moduleOptions = loginModule.get("module-options");
        moduleOptions.get("usersProperties").set(usersProp.getFile());
        moduleOptions.get("rolesProperties").set(rolesProp.getFile());
        moduleOptions.get("unauthenticatedIdentity").set(ANNONYMOUS_USER_NAME);

        updates.add(op);
        applyUpdates(updates, client);

    }




}