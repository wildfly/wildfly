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

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.loginmodules.AbstractLoginModuleTest;
import org.jboss.as.test.integration.security.loginmodules.common.Utils;
import org.junit.AfterClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;

/**
 * @author Jan Lanik
 *  Common functionality for UserRolesLoginModule tests.
 */
public abstract class AbstractUsersRolesLoginModuleTest extends AbstractLoginModuleTest {

   protected static void commonBeforeClass(Class testClass) {
      checkClass(testClass);
      setUsersProperties(classUserMap.get(testClass));

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      setRolesProperties(rolesProps);
   }

   protected static void checkClass(Class testClass) {
      if (!(AbstractUsersRolesLoginModuleTest.class.isAssignableFrom(testClass))) {
         throw new IllegalArgumentException("Class given to a AbstractUsersRolesLoginModuleTest.commonBeforeClass()" +
            "must extend AbstractUsersRolesLoginModuleTest!");
      }
   }



   protected static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
      removeSecurityDomain(client, "users-roles-login-module");
   }

   public String getContextPath() {
      return "users-roles-login-module";
   }

   @AfterClass
   public static void after() throws Exception {
      final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
      // remove test security domains
      removeSecurityDomains(client);
   }

   private static void setPropertiesFile(Map<String, String> props, URL propFile) {
      File userPropsFile = new File(propFile.getFile());
      try {
         Writer writer = new FileWriter(userPropsFile);
         for (Map.Entry<String, String> entry : props.entrySet()) {
            writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
         }
         writer.close();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

   }

   protected static void setUsersProperties(Map<String, String> props) {
      setPropertiesFile(props, Utils.getResource("users-roles-login-module.war/users.properties"));
   }

   protected static void setRolesProperties(Map<String, String> props) {
      setPropertiesFile(props, Utils.getResource("users-roles-login-module.war/roles.properties"));
   }


}
