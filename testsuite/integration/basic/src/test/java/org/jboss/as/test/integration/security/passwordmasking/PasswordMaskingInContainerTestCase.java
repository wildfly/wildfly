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

package org.jboss.as.test.integration.security.passwordmasking;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
public class PasswordMaskingInContainerTestCase {

   @Deployment
   public static WebArchive deploy(){

      WebArchive war = ShrinkWrap.create(WebArchive.class, "passwordMasking" + ".war");
      war.addClass(PasswordMaskingTestServlet.class);
      war.addAsLibrary(Utils.getResource("database-login-module.war/h2-1.2.145.jar"), "h2-1.2.145.jar");
      return war;
   }

   /**
    * Tests if masked DS deployed by servlet supports standard operations.
    */

   @Test
   public void datasourceOperationsTest(){
      DataSource ds;
      try {
      Context ctx = new InitialContext();
      ds = (DataSource)ctx.lookup("java:jboss/datasources/MaskedDS");
      } catch (NamingException ex) {
         throw new AssertionError("Masked datasource not found!");
      }
      assertNotNull("Datasource injection failed.", ds);

      try {
         Connection conn = ds.getConnection();
         Statement statement = conn.createStatement();
         statement.execute("CREATE TABLE FooBars(ID Varchar(50), Password Varchar(50))");
         statement.execute("INSERT INTO FooBars VALUES ('foo','foo'),('bar','bar')");
         ResultSet resultSet = statement.executeQuery("SELECT COUNT (*) FROM FooBars");
         resultSet.next();
         int size = resultSet.getInt(1);
         Assert.assertEquals(2,size);
         statement.execute("DROP TABLE FooBars");
         conn.close();
      } catch (SQLException ex) {
         throw new AssertionError("Masked datasource is not operable!");
      }
   }

}
