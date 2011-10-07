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
package org.jboss.as.testsuite.integration.security.loginmodules.usersroles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.jboss.as.testsuite.integration.security.loginmodules.common.Utils.*;

import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.testsuite.integration.security.loginmodules.common.Coding;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author  Jan Lanik
 * this class contains UsersRolesLoginModule tests with hashAlgorithm=MD5 hashEncoding=base64
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UsersRolesLoginModuleTestCase4 extends AbstractSecuredOnlyUsersRolesLoginModuleTest {



   static{
      classUserMap.put(UsersRolesLoginModuleTestCase2.class, new HashMap<String, String>());
      classUserMap.get(UsersRolesLoginModuleTestCase2.class).put("anil", hash("anil","MD5", Coding.BASE_64));

      classModuleOptionsMap.put(UsersRolesLoginModuleTestCase2.class, new HashMap<String, String>());
      classModuleOptionsMap.get(UsersRolesLoginModuleTestCase2.class).put("hashAlgorithm", "MD5");
      classModuleOptionsMap.get(UsersRolesLoginModuleTestCase2.class).put("hashEncoding", "base64");
   }

   @Deployment
   public static WebArchive deployment() {
      return commonDeployment(UsersRolesLoginModuleTestCase2.class);
   }

   @BeforeClass
   public static void beforeClass() {
      commonBeforeClass(UsersRolesLoginModuleTestCase2.class);
   }




   @Test
   public void testCleartextPassword() throws Exception {
      HttpResponse response = authAndGetResponse(URL, "anil", "anil");
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("GOOD"));
   }

   @Test
   public void testHashedPassword() throws Exception {
      HttpResponse response = authAndGetResponse(URL, "anil", hash("anil","MD5",Coding.BASE_64));
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("The username and password you supplied are not valid."));
   }
}