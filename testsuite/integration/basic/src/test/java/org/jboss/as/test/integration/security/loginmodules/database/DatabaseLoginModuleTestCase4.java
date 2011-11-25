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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jan Lanik
 * basic DatabaseServerLoginModule test
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DatabaseLoginModuleTestCase4 extends AbstractDatabaseLoginModuleTest {

     static {
      Map<String, String> optionsMap = new HashMap<String, String>();
      optionsMap.put("dsJndiName", "java:jboss/datasources/LoginDS");
      optionsMap.put("principalsQuery", "select Password from Principals where PrincipalID=?");
      optionsMap.put("rolesQuery", "select Role, RoleGroup from Roles where PrincipalID=?");
      classModuleOptionsMap.put(DatabaseLoginModuleTestCase4.class, optionsMap);

      Map<String, String> usersMap = new HashMap<String, String>();
      usersMap.put("anil", "anil");
      usersMap.put("marcus", "marcus");
      classUserMap.put(DatabaseLoginModuleTestCase4.class, usersMap);
   }


   @Deployment
   public static WebArchive deployment() {
      return deployment(DatabaseLoginModuleTestCase4.class);
   }


   @Test
   public void testSuccesfullAuth() throws Exception {
      ResultSet resultSet = statement.executeQuery("SELECT * FROM Principals");    //TODO: what is the purpose of thi line?

      makeCall(URL, "anil", "anil", 200);
   }

   @Test
   public void testUnsucessfulAuth() throws Exception {
      makeCall(URL, "marcus", "marcus", 403);
   }

}
