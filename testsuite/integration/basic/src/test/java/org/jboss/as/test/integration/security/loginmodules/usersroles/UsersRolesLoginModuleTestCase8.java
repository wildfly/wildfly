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

import org.apache.http.HttpResponse;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.loginmodules.common.Utils;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.as.test.integration.web.security.WebSecurityPasswordBasedBase;
import org.jboss.dmr.ModelNode;
import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Ignore;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.lang.RuntimeException;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.junit.Assert.assertTrue;

/**
 * @author Jan Lanik
 *
 * To change this template use File | Settings | File Templates.
 */
@Ignore("Configuration problems. More Investigation needed.")
public class UsersRolesLoginModuleTestCase8 extends AbstractSecuredUsersRolesLoginModuleTest{

    static{
      classUserMap.put(UsersRolesLoginModuleTestCase8.class, new HashMap<String, String>());
      classUserMap.get(UsersRolesLoginModuleTestCase8.class).put("foo", "bar");

      classModuleOptionsMap.put(UsersRolesLoginModuleTestCase8.class, new HashMap<String, String>());
      classModuleOptionsMap.get(UsersRolesLoginModuleTestCase8.class).put("password-stacking", "useFirstPass");
   }

   public static class PrepareLoginModule implements LoginModule {

      private Map sharedState;

      public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
         this.sharedState = sharedState;
      }

      public boolean login() throws LoginException {
         sharedState.put("javax.security.auth.login.name","anil");
         sharedState.put("javax.security.auth.login.password","anil");
         return false;
      }

      public boolean commit() throws LoginException {
         return false;
      }

      public boolean abort() throws LoginException {
         return false;
      }

      public boolean logout() throws LoginException {
         return true;
      }
   }

           /**
    * Base method to create a {@link org.jboss.shrinkwrap.api.spec.WebArchive}
    *
    * @param name         Name of the war file
    * @param servletClass a class that is the servlet
    * @param webxml       {@link java.net.URL} to the web.xml. This can be null
    * @return
    */
   protected static WebArchive create(String name, Class<?> servletClass, URL webxml) {
      WebArchive war = ShrinkWrap.create(WebArchive.class, name);
      war.addClass(servletClass);

      war.addAsWebResource(Utils.getResource("web-secure.war/login.jsp"), "login.jsp");
      war.addAsWebResource(Utils.getResource("web-secure.war/error.jsp"), "error.jsp");
      war.addAsWebInfResource(Utils.getResource("users-roles-login-module.war/jboss-web.xml"), "jboss-web.xml");
      war.addClass(UsersRolesLoginModule.class);
      war.addClass(PrepareLoginModule.class);

      if (webxml != null) {
         war.setWebXML(webxml);
      }

      return war;
   }

    public static WebArchive deployment() {
      // FIXME hack to get things prepared before the deployment happens
      try {
         // create required security domains
         createSecurityDomains();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      URL webxml = Utils.getResource("web-secure.war/web.xml");
      WebArchive war = create("users-roles-login-module.war", SecuredServlet.class, webxml);
      WebSecurityPasswordBasedBase.printWar(war);

      return war;
   }

      protected static void createSecurityDomains() throws Exception {

      final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
      List<ModelNode> updates = new ArrayList<ModelNode>();

      ModelNode op1 = new ModelNode();

        op1.get(OP).set(ADD);
        op1.get(OP_ADDR).add(SUBSYSTEM, "security");
        op1.get(OP_ADDR).add(SECURITY_DOMAIN, "prepare-login-module");
        updates.add(op1);

        op1 = new ModelNode();
        op1.get(OP).set(ADD);
        op1.get(OP_ADDR).add(SUBSYSTEM, "security");
        op1.get(OP_ADDR).add(SECURITY_DOMAIN, "prepare-login-module");
        op1.get(OP_ADDR).add(Constants.AUTHENTICATION, Constants.CLASSIC);

        ModelNode loginModule = op1.get(Constants.LOGIN_MODULES).add();
        loginModule.get(ModelDescriptionConstants.CODE).set(UsersRolesLoginModule.class.getName());
        loginModule.get(FLAG).set("optional");
        op1.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

      updates.add(op1);


      String securityDomain =  "users-roles-login-module";
      ModelNode op2 = new ModelNode();
        op2.get(OP).set(ADD);
        op2.get(OP_ADDR).add(SUBSYSTEM, "security");
        op2.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        updates.add(op2);

        op2 = new ModelNode();
        op2.get(OP).set(ADD);
        op2.get(OP_ADDR).add(SUBSYSTEM, "security");
        op2.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        op2.get(OP_ADDR).add(Constants.AUTHENTICATION, Constants.CLASSIC);

        loginModule = op2.get(Constants.LOGIN_MODULES).add();
        loginModule.get(ModelDescriptionConstants.CODE).set(UsersRolesLoginModule.class.getName());
        loginModule.get(FLAG).set("required");
        op2.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

      URL usersProp = Utils.getResource("users-roles-login-module.war/users.properties");
      URL rolesProp = Utils.getResource("users-roles-login-module.war/roles.properties");
      ModelNode moduleOptions = loginModule.get("module-options");

      Map<String, String> moduleOptionsMap = classModuleOptionsMap.get(UsersRolesLoginModuleTestCase8.class);

      moduleOptions.get("usersProperties").set(usersProp.getFile());
      moduleOptions.get("rolesProperties").set(rolesProp.getFile());
      for (Map.Entry<String, String> entry : moduleOptionsMap.entrySet()) {
         moduleOptions.get(entry.getKey()).set(entry.getValue());
      }

      updates.add(op2);

      applyUpdates(updates, client);

   }

   @AfterClass
   public static void after() throws Exception {
      final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
      removeSecurityDomain(client, "prepare-login-module");
   }

   @Ignore("Configuration problems. More Investigation needed.")
   @Test
   public void testLoginInfoPassing() throws Exception {
      HttpResponse response = authAndGetResponse(URL, "anil", "anil");
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("GOOD"));
   }
}
