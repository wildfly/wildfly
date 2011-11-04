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
package org.jboss.as.test.integration.security.loginmodules.database;

import org.apache.http.HttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.security.loginmodules.common.Coding;
import org.jboss.as.test.integration.security.loginmodules.common.Utils;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.test.integration.security.loginmodules.common.Coding.HEX;
import static org.jboss.as.test.integration.security.loginmodules.common.Utils.hash;
import static org.junit.Assert.assertTrue;

/**
 * @author Jan Lanik
 *
 * DatabaseServerLoginModule: hashAlgorithm=MD5, hashEncoding=hex testcase
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DatabaseLoginModuleTestCase3 extends AbstractDatabaseLoginModuleTest {

   static {
      Map<String, String> optionsMap = new HashMap<String, String>();
      optionsMap.put("dsJndiName", "java:jboss/datasources/LoginDS");
      optionsMap.put("principalsQuery", "select Password from Principals where PrincipalID=?");
      optionsMap.put("rolesQuery", "select Role, RoleGroup from Roles where PrincipalID=?");
      optionsMap.put("hashAlgorithm","MD5");
      optionsMap.put("hashEncoding", "hex");
      classModuleOptionsMap.put(DatabaseLoginModuleTestCase3.class, optionsMap);

      Map<String, String> usersMap = new HashMap<String, String>();
      usersMap.put("anil", Utils.hash("anil", "MD5", HEX));
      usersMap.put("marcus", Utils.hash("anil","MD5", HEX));
      classUserMap.put(DatabaseLoginModuleTestCase3.class, usersMap);
   }


   @Deployment
   public static WebArchive deployment() {
      return deployment(DatabaseLoginModuleTestCase3.class);
   }


   @Test
   public void testHashedPassword() throws Exception {
       HttpResponse response = authAndGetResponse(URL, "anil", hash("anil", "MD5", Coding.HEX));
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("The username and password you supplied are not valid."));
   }

   @Test
   public void testCleartextPassword() throws Exception {
      HttpResponse response = authAndGetResponse(URL, "anil", "anil");
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("GOOD"));
   }



}