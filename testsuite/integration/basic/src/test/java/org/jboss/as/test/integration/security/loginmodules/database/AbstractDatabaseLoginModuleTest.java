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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import javax.annotation.sql.DataSourceDefinition;
import java.net.InetAddress;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.h2.tools.Server;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.loginmodules.AbstractLoginModuleTest;
import org.jboss.as.test.integration.security.loginmodules.common.Utils;
import org.jboss.as.test.integration.web.security.WebSecurityPasswordBasedBase;
import org.jboss.dmr.ModelNode;
import org.jboss.security.auth.spi.DatabaseServerLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.runner.RunWith;

@DataSourceDefinition(
   name = "java:app/DataSource",
   user = "sa",
   password = "sa",
   className = "org.h2.jdbcx.JdbcDataSource",
   url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
)


/**
 * @author Jan Lanik.
 *
 * Common superclass for DatabaseLogin test cases.
 */
@RunWith(Arquillian.class)
public abstract class AbstractDatabaseLoginModuleTest extends AbstractLoginModuleTest {

   protected static Server server;
   protected static Connection conn;
   protected static Statement statement;

   protected final String URL = "http://localhost:8080/" + getContextPath() + "/secured/";

   static {
      try {
         server = Server.createTcpServer().start();
      } catch (SQLException ex) {
         throw new RuntimeException(ex);
      }
   }

   public static WebArchive deployment(Class caller) {
      // FIXME hack to get things prepared before the deployment happens
      try {
         initDatabase();
         updateDatabase(caller);
         // create required security domains
         createSecurityDomains(caller);

      } catch (Exception e) {
         // ignore
      }

      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      URL webxml = tccl.getResource("web-secure.war/web.xml");
      WebArchive war = create("database-login-module.war", SecuredServletWithDBSetup.class, webxml);
      WebSecurityPasswordBasedBase.printWar(war);

      return war;
   }

   public static WebArchive create(String name, Class<?> servletClass, URL webxml) {
      WebArchive war = ShrinkWrap.create(WebArchive.class, name);
      war.addClass(servletClass);

      war.addAsWebResource(Utils.getResource("web-secure.war/login.jsp"), "login.jsp");
      war.addAsWebResource(Utils.getResource("web-secure.war/error.jsp"), "error.jsp");
      war.addAsWebInfResource(Utils.getResource("database-login-module.war/jboss-web.xml"), "jboss-web.xml");
      war.addClass(DatabaseServerLoginModule.class);
      war.addAsLibrary(Utils.getResource("database-login-module.war/h2-1.2.145.jar"), "h2-1.2.145.jar");

      if (webxml != null) {
         war.setWebXML(webxml);
      }

      return war;
   }

   protected static void createSecurityDomains(Class caller) throws Exception {
       final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
       List<ModelNode> updates = new ArrayList<ModelNode>();
       ModelNode op = new ModelNode();

       String securityDomain =  "database-login-module";
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
       loginModule.get(ModelDescriptionConstants.CODE).set(DatabaseServerLoginModule.class.getName());
       loginModule.get(FLAG).set("required");
       loginModule.get(MODULE).set("org.picketbox");
       op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

       ModelNode moduleOptions = loginModule.get("module-options");

       Map<String, String> optionsMap = classModuleOptionsMap.get(caller);
       assertNotNull(optionsMap);
       for (Map.Entry<String, String> entry : optionsMap.entrySet()) {
           moduleOptions.get(entry.getKey()).set(entry.getValue());
       }

       updates.add(op);
       applyUpdates(updates, client);

   }

   @Override
   protected String getContextPath() {
      return "database-login-module";
   }

   public static void initDatabase() throws SQLException {

      conn = DriverManager.getConnection("jdbc:h2:tcp://localhost/mem:test", "sa", "sa");
      statement = conn.createStatement();

      statement.executeUpdate("CREATE TABLE Principals(PrincipalID Varchar(50), Password Varchar(50))");
      statement.executeUpdate("CREATE TABLE Roles(PrincipalID Varchar(50), Role Varchar(50), RoleGroup Varchar(50))");
   }

   protected static int getRowCount(String tableName) throws SQLException {
      ResultSet resultSet = statement.executeQuery("SELECT COUNT (*) FROM " + tableName);
      resultSet.next();
      return resultSet.getInt(1);
   }

   protected static void updateDatabase(Class caller) throws SQLException {
      assertNotNull(statement);
      Map<String, String> usersMap = classUserMap.get(caller);
      assertNotNull(usersMap);

      // there should be no values left in the tables
      assertEquals(0, getRowCount("Principals"));
      assertEquals(0, getRowCount("Roles"));
      for (Map.Entry<String, String> entry : usersMap.entrySet()) {
         // record width is 50
         String key = entry.getKey();
         assertTrue(key.length()<=50);
         String value = entry.getValue();
         assertTrue(value.length() <= 50);
         statement.executeUpdate("INSERT INTO Principals VALUES ('" + key + "','" + value + "')");
      }
      statement.executeUpdate("INSERT INTO Roles VALUES ('anil','gooduser','Roles')");
      statement.executeUpdate("INSERT INTO Roles VALUES ('marcus','superuser','Roles')");
   }

   public static void removeDatabase() throws SQLException {
      Statement statement = conn.createStatement();
      statement.executeUpdate("DROP TABLE Principals");
      statement.executeUpdate("DROP TABLE Roles");
      conn.close();
   }

   @AfterClass
   public static void afterClass() throws Exception {
      final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
      removeSecurityDomain(client, "database-login-module");
      removeDatabase();
   }

}
