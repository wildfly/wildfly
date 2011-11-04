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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.as.test.integration.web.security.WebSecurityPasswordBasedBase;
import org.jboss.dmr.ModelNode;
import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Lanik
 *
 * this class contains UsersRolesLoginModule tests with only userProperties and rolesProperties options
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UsersRolesLoginModuleTestCase9 extends AbstractUsersRolesLoginModuleTest {

   protected final String URL = "http://localhost:8080/" + getContextPath() + "/secured/";

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

      war.addAsWebResource(tccl.getResource("web-secure.war/login.jsp"), "login.jsp");
      war.addAsWebResource(tccl.getResource("web-secure.war/error.jsp"), "error.jsp");
      war.addAsWebInfResource(tccl.getResource("users-roles-login-module.war/jboss-web.xml"), "jboss-web.xml");
      war.addClass(UsersRolesLoginModule.class);

      if (webxml != null) {
         war.setWebXML(webxml);
      }

      return war;
   }

   @BeforeClass
   public static void before(){
      Map<String,String> userProps = new HashMap<String, String>();
      userProps.put("anil","anil");
      userProps.put("marcus","marcus");
      setUsersProperties(userProps);

      Map<String,String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil","gooduser");
      rolesProps.put("marcus","superuser");
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

      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      URL webxml = tccl.getResource("web-secure.war/web.xml");
      WebArchive war = create("users-roles-login-module.war", SecuredServlet.class, webxml);
      WebSecurityPasswordBasedBase.printWar(war);

      return war;
   }


   @AfterClass
   public static void after() throws Exception {
      final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
      // remove test security domains
      removeSecurityDomains(client);
   }

   @Test
   public void testSucessfulAuth() throws Exception {
      makeCall(URL, "anil", "anil", 200);
   }

   @Test
   public void testUnsucessfulAuth() throws Exception {
      makeCall(URL, "marcus", "marcus", 403);
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

        updates.add(op);
        applyUpdates(updates, client);

    }

   protected static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
      removeSecurityDomain(client, "users-roles-login-module");
   }

}
