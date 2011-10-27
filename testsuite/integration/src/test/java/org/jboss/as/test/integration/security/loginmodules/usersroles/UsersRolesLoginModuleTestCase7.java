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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author Jan Lanik
 * this class contains UsersRolesLoginModule tests with ignorePasswordCase=true
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UsersRolesLoginModuleTestCase7 extends AbstractSecuredOnlyUsersRolesLoginModuleTest {



   static{
      classUserMap.put(UsersRolesLoginModuleTestCase7.class, new HashMap<String, String>());
      classUserMap.get(UsersRolesLoginModuleTestCase7.class).put("anil", "anil");

      classModuleOptionsMap.put(UsersRolesLoginModuleTestCase7.class, new HashMap<String, String>());
      classModuleOptionsMap.get(UsersRolesLoginModuleTestCase7.class).put("ignorePasswordCase", "true");
   }

   @Deployment
   public static WebArchive deployment() {
      return commonDeployment(UsersRolesLoginModuleTestCase7.class);
   }

   @BeforeClass
   public static void beforeClass() {
      commonBeforeClass(UsersRolesLoginModuleTestCase7.class);
   }




   @Test
   public void testUpperCasePassword() throws Exception {

      HttpResponse response = authAndGetResponse(URL, "anil", "ANIL");
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("GOOD"));
   }

}
