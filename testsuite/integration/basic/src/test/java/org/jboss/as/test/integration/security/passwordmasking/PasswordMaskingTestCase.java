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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.VaultHandler;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */

@RunWith(Arquillian.class)
@ServerSetup(PasswordMaskingTestCase.PasswordMaskingTestCaseSetup.class)
public class PasswordMaskingTestCase {

   private static Logger LOGGER = Logger.getLogger(PasswordMaskingTestCase.class);

   @ArquillianResource URL baseURL;

   static class PasswordMaskingTestCaseSetup extends SnapshotRestoreSetupTask {

       private Server server;
       private VaultHandler vaultHandler;

       @Override
       public void doSetup(ManagementClient managementClient, String containerId) throws Exception {

           VaultHandler.cleanFilesystem(RESOURCE_LOCATION, true);

           ModelNode op;

           // setup DB
           server = Server.createTcpServer("-tcpAllowOthers").start();

           // create new vault
           vaultHandler = new VaultHandler(RESOURCE_LOCATION);

           // create security attributes
           String attributeName = "password";
           String vaultPasswordString = vaultHandler.addSecuredAttribute(VAULT_BLOCK, attributeName,
                   DS_CLEAR_TEXT_PASSWORD.toCharArray());

           LOGGER.debug("vaultPasswordString=" + vaultPasswordString);

           // create new vault setting in standalone
           op = new ModelNode();
           op.get(OP).set(ADD);
           op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
           ModelNode vaultOption = op.get(VAULT_OPTIONS);
           vaultOption.get("KEYSTORE_URL").set(vaultHandler.getKeyStore());
           vaultOption.get("KEYSTORE_PASSWORD").set(vaultHandler.getMaskedKeyStorePassword());
           vaultOption.get("KEYSTORE_ALIAS").set(vaultHandler.getAlias());
           vaultOption.get("SALT").set(vaultHandler.getSalt());
           vaultOption.get("ITERATION_COUNT").set(vaultHandler.getIterationCountAsString());
           vaultOption.get("ENC_FILE_DIR").set(vaultHandler.getEncodedVaultFileDirectory());
           managementClient.getControllerClient().execute(new OperationBuilder(op).build());

           LOGGER.debug("Vault created in sever configuration");

           // create new datasource with right password
           ModelNode address = new ModelNode();
           address.add(SUBSYSTEM, "datasources");
           address.add("data-source", VAULT_BLOCK);
           address.protect();
           op = new ModelNode();
           op.get(OP).set(ADD);
           op.get(OP_ADDR).set(address);
           op.get("jndi-name").set("java:jboss/datasources/" + VAULT_BLOCK);
           op.get("use-java-context").set("true");
           op.get("driver-name").set("h2");
           op.get("pool-name").set(VAULT_BLOCK);
           op.get("connection-url").set("jdbc:h2:tcp://" + Utils.getSecondaryTestAddress(managementClient) + "/mem:masked");
           op.get("user-name").set("sa");
           op.get("password").set("${" + vaultPasswordString + "}");
           op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
           managementClient.getControllerClient().execute(new OperationBuilder(op).build());

           LOGGER.debug(VAULT_BLOCK + " datasource created");

       }

       @Override
       protected void nonManagementCleanUp() throws Exception {
           // remove temporary files
           vaultHandler.cleanUp();

           // stop DB
           server.shutdown();
       }
   }

   static final String RESOURCE_LOCATION = PasswordMaskingTestCase.class.getResource("/").getPath()
           + "security/pwdmsk-vault/";
   static final String VAULT_BLOCK = "MaskedDS";
   static final String DS_CLEAR_TEXT_PASSWORD = "sa";

   @Deployment
   public static WebArchive deploy(){
      //Utils.stop();

      WebArchive war = ShrinkWrap.create(WebArchive.class, "passwordMasking" + ".war");
      war.addClass(PasswordMaskingTestServlet.class);
      war.setWebXML(PasswordMaskingTestCase.class.getPackage(), "web.xml");

      return war;
   }

   /**
    * Tests if masked ds can be accessed from servlet
    */
   @RunAsClient
   @Test
   public void servletDatasourceInjectionTest(){
      DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response;
      HttpGet httpget = new HttpGet(baseURL.toString());

      String responseText;
      try {
         response = httpclient.execute(httpget);
         HttpEntity entity = response.getEntity();
         responseText = EntityUtils.toString(entity);
      } catch (IOException ex) {
         throw new RuntimeException("No response from servlet!", ex);
      }

      assertTrue("Masked datasource not injected correctly to the servlet! Servlet response text: " + responseText, responseText.contains("true"));
   }

}
