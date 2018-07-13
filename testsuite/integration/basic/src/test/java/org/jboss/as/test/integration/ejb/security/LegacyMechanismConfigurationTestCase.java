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

package org.jboss.as.test.integration.ejb.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.ejb.security.authorization.LegacyMechanismBean;
import org.jboss.as.test.integration.ejb.security.authorization.LegacyMechanismBeanRemote;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;


/**
 * Test for issue https://issues.jboss.org/browse/WFLY-10353
 *
 * Security is configured via legacy security options in remoting subsystem. Test controls, that anonymous authentication is used.
 *
 *  @author Jiri Ondrusek (jondruse@redhat.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(LegacyMechanismConfigurationTestCase.LegacyMechanismConfigurationSetupTask.class)
public class LegacyMechanismConfigurationTestCase {

   private static final Logger log = Logger.getLogger(LegacyMechanismConfigurationTestCase.class);
   private static final String MODULE = "legacyMechanismConfiguration";

   @ContainerResource
   private ManagementClient managementClient;


   @Deployment(name = MODULE + ".jar", order = 1, testable = false)
   public static Archive<JavaArchive> testAppDeployment() {
      final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar")
              .addClass(LegacyMechanismBean.class)
              .addClass(LegacyMechanismBeanRemote.class);
      return jar;
   }

   @Test
   public void testPrincipal() throws Exception {
      String echoValue = getBean(log).getPrincipal();
      Assert.assertEquals("anonymous", echoValue);
   }

   // ejb client code

   private LegacyMechanismBeanRemote getBean(final Logger log) throws Exception {
      log.trace("**** creating InitialContext");
      InitialContext ctx = new InitialContext(setupEJBClientProperties());
      try {
         log.trace("**** looking up StatelessBean through JNDI");
         LegacyMechanismBeanRemote bean = (LegacyMechanismBeanRemote)
                 ctx.lookup("ejb:/" + MODULE + "/" + LegacyMechanismBean.class.getSimpleName() + "!" + LegacyMechanismBeanRemote.class.getCanonicalName());
         return bean;
      } finally {
         ctx.close();
      }
   }

   private Properties setupEJBClientProperties() throws IOException {
      log.trace("*** reading EJBClientContextSelector properties");
      // setup the properties
      final String clientPropertiesFile = "org/jboss/as/test/integration/ejb/security/jboss-ejb-client.properties";
      final InputStream inputStream = LegacyMechanismConfigurationTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
      if (inputStream == null) {
         throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
      }
      final Properties properties = new Properties();
      properties.load(inputStream);

      properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
      return properties;
   }

   /**
    * Setup task which creates snapshot, adds legacy remoting properties and restores to snapshot afterwards.
    */
   public static class LegacyMechanismConfigurationSetupTask implements ServerSetupTask {

      private static String snapshot;

      private static final PathAddress LOCAL_AUTHENTICATION = PathAddress.pathAddress(ModelDescriptionConstants.CORE_SERVICE, "management")
              .append("security-realm", "ApplicationRealm").append("authentication", "local");

      private static final PathAddress SASL_MECHANISMS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "remoting")
              .append("http-connector", "http-remoting-connector").append("property", "SASL_MECHANISMS");

      private static final PathAddress SASL_POLICY_NOANONYMOUS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "remoting")
              .append("http-connector", "http-remoting-connector").append("property", "SASL_POLICY_NOANONYMOUS");

      @Override
      public void setup(ManagementClient managementClient, String containerId) throws Exception {
         ModelControllerClient mcc = managementClient.getControllerClient();
         snapshot = takeSnapshot(mcc);

         ModelNode compositeOperation = Operations.createCompositeOperation();
         compositeOperation.get(ModelDescriptionConstants.STEPS).add(removeLocalAuthentication());
         compositeOperation.get(ModelDescriptionConstants.STEPS).add(addSaslMechanisms());
         compositeOperation.get(ModelDescriptionConstants.STEPS).add(addPolicyNoanonymous());

         ModelNode response = execute(compositeOperation, mcc);
         Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());

         ServerReload.reloadIfRequired(managementClient.getControllerClient());
      }

      @Override
      public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
         ModelControllerClient mcc = managementClient.getControllerClient();
         restoreSnapshot(mcc, snapshot);
      }

      private ModelNode removeLocalAuthentication() throws IOException {
         ModelNode removeOperation = Operations.createRemoveOperation(LOCAL_AUTHENTICATION.toModelNode());
         return removeOperation;
      }

      private ModelNode addSaslMechanisms() throws IOException {
         ModelNode addRaOperation = Operations.createAddOperation(SASL_MECHANISMS.toModelNode());
         addRaOperation.get("name").set("value");
         addRaOperation.get("value").set("ANONYMOUS,PLAIN");
         return addRaOperation;
      }

      private ModelNode addPolicyNoanonymous() throws IOException {
         ModelNode addRaOperation = Operations.createAddOperation(SASL_POLICY_NOANONYMOUS.toModelNode());
         addRaOperation.get("name").set("value");
         addRaOperation.get("value").set("false");
         return addRaOperation;
      }

      private ModelNode execute(ModelNode operation, ModelControllerClient client) throws IOException {
         return client.execute(operation);
      }

      // snapshots

      private String takeSnapshot(ModelControllerClient client) throws IOException {
         ModelNode operation = new ModelNode();
         operation.get(OP).set("take-snapshot");
         ModelNode result = execute(operation, client);
         String snapshotFileName = result.get("result").asString();

         File snapshotFile = new File(snapshotFileName);
         Assert.assertTrue(snapshotFile.exists());
         return snapshotFileName;
      }

      private void restoreSnapshot(ModelControllerClient client, String snapshot) throws IOException{
         File snapshotFile = new File(snapshot);
         File configurationDir = snapshotFile.getParentFile().getParentFile().getParentFile();
         File standaloneConfiguration = new File(configurationDir, "standalone.xml");
         if (!snapshotFile.renameTo(standaloneConfiguration)) {
            log.warn("File " + snapshotFile.getAbsolutePath() + " could not be renamed to " + standaloneConfiguration.getAbsolutePath());
         }
         // delete snapshot
         ModelNode op = ModelUtil.createOpNode(null, "delete-snapshot");
         op.get("name").set(snapshotFile.getName());
         execute(op, client);

         // check that the file is deleted
         Assert.assertFalse("Snapshot file still exists.", snapshotFile.exists());
      }

   }
}