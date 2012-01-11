package org.jboss.as.test.integration.security.loginmodules;

import org.apache.http.HttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.security.loginmodules.common.Coding;
import org.jboss.as.test.integration.security.loginmodules.common.Utils;
import org.jboss.as.test.integration.security.loginmodules.common.WebAppDeployment;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.test.integration.security.loginmodules.common.Utils.authAndGetResponse;
import static org.jboss.as.test.integration.security.loginmodules.common.Utils.getContent;
import static org.jboss.as.test.integration.security.loginmodules.common.Utils.getResource;
import static org.jboss.as.test.integration.security.loginmodules.common.Utils.hash;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jan Lanik
 *         UserRolesLoginModule tests.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UsersRolesLoginModuleTestCase {

   private static Logger log = Logger.getLogger(UsersRolesLoginModuleTestCase.class);

   private static final Map<String, WebAppDeployment> DEPLOYMENTS = new HashMap<String, WebAppDeployment>();


   private static final String DEP1 = "UsersRoles-defaultSettings";

   /**
    * plaintext login with no additional options
    */
   @Deployment(name = DEP1, order = 1)
   public static WebArchive appDeployment1() {
      log.info("start" + DEP1 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP1, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP1, dep);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", Utils.getResource("users-roles-login-module.war/users.properties").getFile());
      dep.addModuleOption("rolesProperties", Utils.getResource("users-roles-login-module.war/roles.properties").getFile());

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", "anil");
      usersProps.put("marcus", "marcus");
      Utils.setPropertiesFile(usersProps, getResource("users-roles-login-module.war/users.properties"));

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      rolesProps.put("marcus", "superuser");
      Utils.setPropertiesFile(rolesProps, getResource("users-roles-login-module.war/roles.properties"));

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP1)
   @Test
   public void testCleartextPassword1() throws Exception {
      log.debug("start testCleartextPassword()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP1).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrect login
    */
   @OperateOnDeployment(DEP1)
   @Test
   public void testIncorrectCleartextPassword1() throws Exception {
      log.debug("start testIncorrectCleartextPassword()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP1).getUrl() + "/secured/", "anil", "nonsense");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }

   @OperateOnDeployment(DEP1)
   @Test
   public void testUnauthorizedUser() throws Exception {
      log.debug("start testUnauthorizedUser()");
      Utils.makeCall(DEPLOYMENTS.get(DEP1).getUrl() + "/secured/", "marcus", "marcus", 403);
   }


   private static final String DEP2 = "UsersRoles-MD5";

   /**
    * passwords stored as MD5, no additional options
    */
   @Deployment(name = DEP2, order = 2)
   public static WebArchive appDeployment2() {
      log.info("start" + DEP2 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP2, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP2, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", hash("anil", "MD5", Coding.BASE_64));
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());
      dep.addModuleOption("hashAlgorithm", "MD5");

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP2)
   @Test
   public void testMD5Password1() throws Exception {
      log.debug("start testMD5Password1()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP2).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrect login
    */
   @OperateOnDeployment(DEP2)
   @Test
   public void testIncorrectMD5Password1() throws Exception {
      log.debug("start testIncorrectMD5Password1()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP2).getUrl() + "/secured/", "anil", hash("anil", "MD5", Coding.BASE_64));
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }


   private static final String DEP3 = "UsersRoles-MD5-hex";

   /**
    * passwords stored as MD5 in HEX encoding, no additional options
    */
   @Deployment(name = DEP3, order = 3)
   public static WebArchive appDeployment3() {
      log.info("start" + DEP3 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP3, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP3, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", hash("anil", "MD5", Coding.HEX));
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());
      dep.addModuleOption("hashAlgorithm", "MD5");
      dep.addModuleOption("hashEncoding", "hex");

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP3)
   @Test
   public void testMD5PasswordHex() throws Exception {
      log.debug("start testMD5PasswordHex()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP3).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrect login
    */
   @OperateOnDeployment(DEP3)
   @Test
   public void testIncorrectMD5PasswordHex() throws Exception {
      log.debug("start testIncorrectMD5PasswordHex()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP3).getUrl() + "/secured/", "anil", hash("anil", "MD5", Coding.HEX));
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }


   private static final String DEP4 = "UsersRoles-MD5-base64";

   /**
    * passwords stored as MD5 in BASE64 encoding, no additional options
    */
   @Deployment(name = DEP4, order = 4)
   public static WebArchive appDeployment4() {
      log.info("start" + DEP4 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP4, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP4, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", hash("anil", "MD5", Coding.BASE_64));
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());
      dep.addModuleOption("hashAlgorithm", "MD5");
      dep.addModuleOption("hashEncoding", "base64");

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP4)
   @Test
   public void testMD5PasswordBase64() throws Exception {
      log.debug("start testMD5PasswordBase64()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP4).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrect login
    */
   @OperateOnDeployment(DEP4)
   @Test
   public void testIncorrectMD5PasswordBase64() throws Exception {
      log.debug("start testIncorrectMD5PasswordBase64()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP4).getUrl() + "/secured/", "anil", hash("anil", "MD5", Coding.BASE_64));
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }


   private static final String DEP5a = "UsersRoles-hashUserPassword-false";

   /**
    * tests hashUserPassword=false option
    */
   @Deployment(name = DEP5a, order = 5)
   public static WebArchive appDeployment5A() {
      log.info("start" + DEP5a + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP5a, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP5a, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", hash("anil", "MD5", Coding.BASE_64));
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());
      dep.addModuleOption("hashAlgorithm", "MD5");
      dep.addModuleOption("hashUserPassword", "false");

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP5a)
   @Test
   public void testHashUserPasswordA() throws Exception {
      log.debug("start testHashUserPasswordA()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP5a).getUrl() + "/secured/", "anil", hash("anil", "MD5", Coding.BASE_64));
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrect login
    */
   @OperateOnDeployment(DEP5a)
   @Test
   public void testIncorrectHashUserPasswordA() throws Exception {
      log.debug("start testIncorrectHashUserPasswordA()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP5a).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }


   private static final String DEP5b = "UsersRoles-hashUserPassword-true";

   /**
    * tests hashUserPassword=true option
    */
   @Deployment(name = DEP5b, order = 5)
   public static WebArchive appDeployment5B() {
      log.info("start" + DEP5b + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP5b, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP5b, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", hash("anil", "MD5", Coding.BASE_64));
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());
      dep.addModuleOption("hashAlgorithm", "MD5");
      dep.addModuleOption("hashUserPassword", "true");

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP5b)
   @Test
   public void testHashUserPasswordB() throws Exception {
      log.debug("start testHashUserPasswordB()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP5b).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrect login
    */
   @OperateOnDeployment(DEP5b)
   @Test
   public void testIncorrectHashUserPassword() throws Exception {
      log.debug("start testIncorrectHashUserPasswordB()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP5b).getUrl() + "/secured/", "anil", hash("anil", "MD5", Coding.BASE_64));
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }


   private static final String DEP6 = "UsersRoles-hashStorePassword";

   /**
    * tests hashUserPassword option
    */
   @Deployment(name = DEP6, order = 6)
   public static WebArchive appDeployment6() {
      log.info("start" + DEP6 + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP6, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP6, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", "anil");
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());
      dep.addModuleOption("hashAlgorithm", "MD5");
      dep.addModuleOption("hashStorePassword", "true");

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP6)
   @Test
   public void testHashStorePassword() throws Exception {
      log.debug("start testCleartextPassword()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP6).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrect login
    */
   @OperateOnDeployment(DEP6)
   @Test
   public void testIncorrectHashStorePassword() throws Exception {
      log.debug("start testCleartextPassword()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP6).getUrl() + "/secured/", "anil", hash("anil", "MD5", Coding.BASE_64));
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }


   private static final String DEP7a = "UsersRoles-ignorePasswordCase-true";

   /**
    * tests ignorePasswordCase=true option
    */
   @Deployment(name = DEP7a)
   public static WebArchive appDeployment7a() {
      log.info("start" + DEP7a + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP7a, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP7a, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", "anil");
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());
      dep.addModuleOption("ignorePasswordCase", "true");

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP7a)
   @Test
   public void testIgnorePasswordCaseA() throws Exception {
      log.debug("start testIgnorePasswordCaseA()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP7a).getUrl() + "/secured/", "anil", "ANIL");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Standard login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP7a)
   @Test
   public void testIgnorePasswordCaseA2() throws Exception {
      log.debug("start testCleartextPasswordA2()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP7a).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   private static final String DEP7b = "UsersRoles-ignorePasswordCase-false";

   /**
    * tests ignorePasswordCase=true option
    */
   @Deployment(name = DEP7b)
   public static WebArchive appDeployment7() {
      log.info("start" + DEP7b + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP7b, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP7b, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", "anil");
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());
      dep.addModuleOption("ignorePasswordCase", "false");

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP7b)
   @Test
   public void testIgnorePasswordCaseB() throws Exception {
      log.debug("start testIgnorePasswordCaseB()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP7b).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrert login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP7b)
   @Test
   public void testIgnorePasswordCaseB2() throws Exception {
      log.debug("start testCleartextPassword()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP7b).getUrl() + "/secured/", "anil", "ANIL");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }

   private static final String DEP7c = "UsersRoles-ignorePasswordCase-default";

   /**
    * tests ignorePasswordCase=true option
    */
   @Deployment(name = DEP7c)
   public static WebArchive appDeployment7c() {
      log.info("start" + DEP7c + "deployment");

      WebAppDeployment dep = new WebAppDeployment(DEP7c, SecuredServlet.class, UsersRolesLoginModule.class);
      DEPLOYMENTS.put(DEP7c, dep);

      Map<String, String> usersProps = new HashMap<String, String>();
      usersProps.put("anil", "anil");
      File usersPropsFile = Utils.createTempPropFile(usersProps);

      Map<String, String> rolesProps = new HashMap<String, String>();
      rolesProps.put("anil", "gooduser");
      File rolesPropsFile = Utils.createTempPropFile(rolesProps);

      log.debug("adding module options");
      dep.addModuleOption("usersProperties", usersPropsFile.getAbsolutePath());
      dep.addModuleOption("rolesProperties", rolesPropsFile.getAbsolutePath());

      log.debug(dep.getWar().toString(true));

      return dep.getWar();
   }

   /**
    * Correct login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP7c)
   @Test
   public void testIgnorePasswordCaseC() throws Exception {
      log.debug("start testIgnorePasswordCaseB()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP7c).getUrl() + "/secured/", "anil", "anil");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("GOOD"));
   }

   /**
    * Incorrert login
    *
    * @throws Exception
    */
   @OperateOnDeployment(DEP7c)
   @Test
   public void testIgnorePasswordCaseC2() throws Exception {
      log.debug("start testCleartextPassword()");
      HttpResponse response = authAndGetResponse(DEPLOYMENTS.get(DEP7c).getUrl() + "/secured/", "anil", "ANIL");
      String pageContent = getContent(response);
      log.debug("returned page content:\n" + pageContent);
      assertTrue(pageContent.contains("The username and password you supplied are not valid"));
   }


}
