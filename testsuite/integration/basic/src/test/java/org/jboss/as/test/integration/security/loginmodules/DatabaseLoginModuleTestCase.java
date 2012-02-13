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

package org.jboss.as.test.integration.security.loginmodules;

import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.loginmodules.common.Coding;
import org.jboss.as.test.integration.security.loginmodules.common.WebAppDeployment;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SecuredServletWithDBSetupForDep1;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SecuredServletWithDBSetupForDep2;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SecuredServletWithDBSetupForDep3;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SecuredServletWithDBSetupForDep4;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.DatabaseServerLoginModule;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.h2.tools.Server;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.jboss.as.test.integration.security.loginmodules.common.Coding.BASE_64;
import static org.jboss.as.test.integration.security.loginmodules.common.Coding.HEX;
import static org.jboss.as.test.integration.security.common.Utils.authAndGetResponse;
import static org.jboss.as.test.integration.security.common.Utils.getContent;
import static org.jboss.as.test.integration.security.common.Utils.hash;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 *         <p/>
 *         DatabaseLoginModule tests
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DatabaseLoginModuleTestCase {

   private static Logger log = Logger.getLogger(DatabaseLoginModuleTestCase.class);

   private static final Map<String, WebAppDeployment> DEPLOYMENTS = new HashMap<String, WebAppDeployment>();
   private static final Map<String, Integer> DEP_NUM = new HashMap<String, Integer>();

   private static class DatabaseManager {

      DatabaseManager() {
         try {
            server = Server.createTcpServer().start();
         } catch (SQLException ex) {
            throw new RuntimeException(ex);
         }
      }

      private Server server;

      private Map<Integer, Boolean> databaseInitializedFlags = new HashMap<Integer, Boolean>();

      public boolean isDatabaseInitialized(int depNum) {
         if (null == databaseInitializedFlags.get(depNum)) {
            databaseInitializedFlags.put(depNum, false);
         }
         return databaseInitializedFlags.get(depNum);
      }

      public void setDatabaseInitializedFlag(int depNum, boolean flag) {
         databaseInitializedFlags.put(depNum, flag);
      }

      private Map<Integer, Connection> connectionMap = new HashMap<Integer, Connection>();

      private Connection getConnection(int depNum) throws SQLException {
         Connection conn = connectionMap.get(depNum);
         if (null == conn) {
            conn = DriverManager.getConnection("jdbc:h2:tcp://localhost/mem:test" + depNum, "sa", "sa");
            log.debug("connection to dep" + depNum + " database opened (jdbc:h2:tcp://localhost/mem:test" + depNum + ")");
            // we are storing refferences to all connections so that we can check in @AfterClass that all of them are closed.
            // That serves as an integrity check - all connections should be closed after usage
            connectionMap.put(depNum, conn);
         }
         return conn;
      }

      public void executeUpdate(int depNum, String query) throws SQLException {
         Connection connection = getConnection(depNum);
         Statement statement = connection.createStatement();
         statement.execute(query);
         log.debug("dep" + depNum + "database: SQL statement executed: " + query);
      }

      public ResultSet executeQuery(int depNum, String query) throws SQLException {
         Connection connection = getConnection(depNum);
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery(query);
         log.debug("dep" + depNum + "database: SQL statement executed: " + query);
         return result;
      }

      public void initDatabase(int depNum) throws SQLException {
         assertFalse(null == server);
         assertFalse(isDatabaseInitialized(depNum));
         executeUpdate(depNum, "CREATE TABLE Principals(PrincipalID Varchar(50), Password Varchar(50))");
         executeUpdate(depNum, "CREATE TABLE Roles(PrincipalID Varchar(50), Role Varchar(50), RoleGroup Varchar(50))");

         setDatabaseInitializedFlag(depNum, true);
      }

      private int getRowCount(String tableName, int depNum) throws SQLException {
         assertTrue(isDatabaseInitialized(depNum));

         ResultSet resultSet = executeQuery(depNum, "SELECT COUNT (*) FROM " + tableName);

         resultSet.next();
         return resultSet.getInt(1);
      }

      public void updateDatabase(int depNum, Map<String, String> usersMap) throws SQLException {
         if (isDatabaseInitialized(depNum)) {
            clearDatabase(depNum);
         }
         initDatabase(depNum);

         assertTrue(isDatabaseInitialized(depNum));
         assertNotNull(usersMap);

         // there should be no values left in the tables
         Assert.assertEquals(0, getRowCount("Principals", depNum));
         Assert.assertEquals(0, getRowCount("Roles", depNum));
         for (Map.Entry<String, String> entry : usersMap.entrySet()) {
            // record width is 50
            String key = entry.getKey();
            Assert.assertTrue(key.length() <= 50);
            String value = entry.getValue();
            Assert.assertTrue(value.length() <= 50);
            executeUpdate(depNum, "INSERT INTO Principals VALUES ('" + key + "','" + value + "')");
         }
         executeUpdate(depNum, "INSERT INTO Roles VALUES ('anil','gooduser','Roles')");
         executeUpdate(depNum, "INSERT INTO Roles VALUES ('marcus','superuser','Roles')");
      }

      public void clearDatabase(int depNum) throws SQLException {
         assertTrue(isDatabaseInitialized(depNum));
         Statement statement = getConnection(depNum).createStatement();

         statement.executeUpdate("DROP TABLE Principals");
         log.debug("executed: DROP TABLE Principals");
         statement.executeUpdate("DROP TABLE Roles");
         log.debug("DROP TABLE Roles");

         statement.getConnection().close();
         log.debug("connection to dep" + depNum + " database closed");
         setDatabaseInitializedFlag(depNum, false);
      }

      public void releaseResources() {
         try {
            for (Connection conn : connectionMap.values()) {
               conn.close();
            }
         } catch (SQLException ex) {
            throw new RuntimeException("not able to close connection", ex);
         } finally {
            server.shutdown();
         }
      }
   }

   private static DatabaseManager databaseManager = new DatabaseManager();

   @AfterClass
   public static void removeDatabaseTask() throws SQLException {
      databaseManager.releaseResources();
      databaseManager = null;
   }

   private static WebArchive addH2Lib(WebArchive war) {
      war.addAsLibrary(Utils.getResource("database-login-module.war/h2-1.2.145.jar"), "h2-1.2.145.jar");
      return war;
   }

   private static final String DEP1 = "DatabaseLogin-defaultSetting";

   static {
      DEP_NUM.put(DEP1, 1);
   }

   /**
    * plaintext login with no additional options
    */
   @Deployment(name = DEP1, order = 1)
   public static WebArchive appDeployment1() throws SQLException {
      log.info("start" + DEP1 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP1, SecuredServletWithDBSetupForDep1.class, DatabaseServerLoginModule.class);
      DEPLOYMENTS.put(DEP1, dep);

      log.debug("adding module options");
      dep.addModuleOption("dsJndiName", "java:jboss/datasources/LoginDSdep" + DEP_NUM.get(DEP1));
      dep.addModuleOption("principalsQuery", "select Password from Principals where PrincipalID=?");
      dep.addModuleOption("rolesQuery", "select Role, RoleGroup from Roles where PrincipalID=?");

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", "anil");
      usersProps.put("marcus", "marcus");
      databaseManager.updateDatabase(DEP_NUM.get(DEP1), usersProps);

      log.debug(dep.getWar().toString(true));

      WebArchive war = dep.getWar();
      addH2Lib(war);
      return war;
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP1)
   @Test
   public void testSuccesfullAuth(@ArquillianResource URL url) throws Exception {
      Utils.makeCall(url + "secured/", "anil", "anil", 200);
   }

   /**
    * Incorrect login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP1)
   @Test
   public void testUnsucessfulAuth(@ArquillianResource URL url) throws Exception {
      Utils.makeCall(url + "secured/", "marcus", "marcus", 403);
   }


   private static final String DEP2 = "DatabaseLogin-hashMD5";

   static {
      DEP_NUM.put(DEP2, 2);
   }

   /**
    * DatabaseServerLoginModule: hashAlgorithm=MD5 testcase
    */
   @Deployment(name = DEP2, order = 2)
   public static WebArchive appDeployment2() throws SQLException {
      log.info("start" + DEP2 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP2, SecuredServletWithDBSetupForDep2.class, DatabaseServerLoginModule.class);
      DEPLOYMENTS.put(DEP2, dep);

      log.debug("adding module options");
      dep.addModuleOption("dsJndiName", "java:jboss/datasources/LoginDSdep" + DEP_NUM.get(DEP2));
      dep.addModuleOption("principalsQuery", "select Password from Principals where PrincipalID=?");
      dep.addModuleOption("rolesQuery", "select Role, RoleGroup from Roles where PrincipalID=?");
      dep.addModuleOption("hashAlgorithm", "MD5");

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", Utils.hash("anil", "MD5", BASE_64));
      usersProps.put("marcus", Utils.hash("marcus", "MD5", BASE_64));

      databaseManager.updateDatabase(DEP_NUM.get(DEP2), usersProps);

      log.debug(dep.getWar().toString(true));

      WebArchive war = dep.getWar();
      addH2Lib(war);
      return war;
   }

   @OperateOnDeployment(DEP2)
   @Test
   public void testHashedPassword(@ArquillianResource URL url) throws Exception {
      HttpResponse response = authAndGetResponse(url + "secured/", "anil", hash("anil", "MD5", Coding.BASE_64));
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("The username and password you supplied are not valid."));
   }

   @OperateOnDeployment(DEP2)
   @Test
   public void testCleartextPassword(@ArquillianResource URL url) throws Exception {
      HttpResponse response = authAndGetResponse(url + "secured/", "anil", "anil");
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("GOOD"));
   }


   private static final String DEP3 = "DatabaseLogin-hashMD5-base64";

   static {
      DEP_NUM.put(DEP3, 3);
   }

   /**
    * DatabaseServerLoginModule: hashAlgorithm=MD5, hashEncoding=base64 testcase
    */
   @Deployment(name = DEP3, order = 3)
   public static WebArchive appDeployment3() throws SQLException {
      log.info("start" + DEP3 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP3, SecuredServletWithDBSetupForDep3.class, DatabaseServerLoginModule.class);
      DEPLOYMENTS.put(DEP3, dep);

      log.debug("adding module options");
      dep.addModuleOption("dsJndiName", "java:jboss/datasources/LoginDSdep" + DEP_NUM.get(DEP3));
      dep.addModuleOption("principalsQuery", "select Password from Principals where PrincipalID=?");
      dep.addModuleOption("rolesQuery", "select Role, RoleGroup from Roles where PrincipalID=?");
      dep.addModuleOption("hashAlgorithm", "MD5");
      dep.addModuleOption("hashEncoding", "base64");

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", Utils.hash("anil", "MD5", Coding.BASE_64));
      usersProps.put("marcus", Utils.hash("marcus", "MD5", BASE_64));
      databaseManager.updateDatabase(DEP_NUM.get(DEP3), usersProps);

      log.debug(dep.getWar().toString(true));

      WebArchive war = dep.getWar();
      addH2Lib(war);
      return war;
   }

   @OperateOnDeployment(DEP3)
   @Test
   public void testHashedPassword3(@ArquillianResource URL url) throws Exception {
      HttpResponse response = authAndGetResponse(url + "secured/", "anil", hash("anil", "MD5", Coding.BASE_64));
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("The username and password you supplied are not valid."));
   }

   @OperateOnDeployment(DEP3)
   @Test
   public void testCleartextPassword3(@ArquillianResource URL url) throws Exception {
      HttpResponse response = authAndGetResponse(url + "secured/", "anil", "anil");
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("GOOD"));
   }

   private static final String DEP4 = "DatabaseLogin-hashMD5-hex";

   static {
      DEP_NUM.put(DEP4, 4);
   }

   /**
    * DatabaseServerLoginModule: hashAlgorithm=MD5, hashEncoding=hex testcase
    */
   @Deployment(name = DEP4, order = 4)
   public static WebArchive appDeployment4() throws SQLException {
      log.info("start" + DEP4 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP4, SecuredServletWithDBSetupForDep4.class, DatabaseServerLoginModule.class);
      DEPLOYMENTS.put(DEP4, dep);

      log.debug("adding module options");
      dep.addModuleOption("dsJndiName", "java:jboss/datasources/LoginDSdep" + DEP_NUM.get(DEP4));
      dep.addModuleOption("principalsQuery", "select Password from Principals where PrincipalID=?");
      dep.addModuleOption("rolesQuery", "select Role, RoleGroup from Roles where PrincipalID=?");
      dep.addModuleOption("hashAlgorithm", "MD5");
      dep.addModuleOption("hashEncoding", "hex");

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", Utils.hash("anil", "MD5", HEX));
      usersProps.put("marcus", Utils.hash("marcus", "MD5", HEX));
      databaseManager.updateDatabase(DEP_NUM.get(DEP4), usersProps);

      log.debug(dep.getWar().toString(true));

      WebArchive war = dep.getWar();
      addH2Lib(war);
      return war;
   }

   @OperateOnDeployment(DEP4)
   @Test
   public void testHashedPassword4(@ArquillianResource URL url) throws Exception {
      HttpResponse response = authAndGetResponse(url + "secured/", "anil", hash("anil", "MD5", HEX));
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("The username and password you supplied are not valid."));
   }

   @OperateOnDeployment(DEP4)
   @Test
   public void testCleartextPassword2(@ArquillianResource URL url) throws Exception {
      HttpResponse response = authAndGetResponse(url + "secured/", "anil", "anil");
      String pageContent = getContent(response);
      assertTrue(pageContent.contains("GOOD"));
   }

}


