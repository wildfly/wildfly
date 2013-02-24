/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.security.perimeter;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.container.NetworkUtils;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Test verifies that the web management console is secured
 *
 * @author jlanik@redhat.com
 */

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebConsoleSecurityTestCase.ConnectionManager.class)
public class WebConsoleSecurityTestCase {

   public static class ConnectionManager implements ServerSetupTask{

      private static Logger log = Logger.getLogger(WebConsoleSecurityTestCase.ConnectionManager.class);
      private static URL consoleURL;
      
      private static URL getConsoleUrl() throws MalformedURLException{
         if(null == consoleURL){
            throw new AssertionError("consoleURL should have been initialized!");
         }
         return consoleURL;
      }
      
      public void setup(ManagementClient managementClient, String containerId) throws Exception {
         log.trace("Setup started");
         consoleURL = new URL(getBinding("http", "management-http", managementClient.getControllerClient()).toURL() + "/management");
         log.debug("management web console URL retrieved: " + consoleURL);
      }

      public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
         // this is intentionaly empty
      }
      

      private URI getBinding(final String protocol, final String socketBinding, ModelControllerClient client) {
         try {
            //TODO: resolve socket binding group correctly (copied from ModelControllerClient)
            final String NAME = "name";
            final String socketBindingGroupName = readRootNode(client).get("socket-binding-group").keys().iterator().next();

            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).get("socket-binding-group").set(socketBindingGroupName);
            operation.get(OP_ADDR).get("socket-binding").set(socketBinding);
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("bound-address");
            String ip = executeForResult(operation, client).asString();
            //it appears some system can return a binding with the zone specifier on the end
            if(ip.contains(":") && ip.contains("%")) {
               ip = ip.split("%")[0];
            }

            final ModelNode portOp = new ModelNode();
            portOp.get(OP_ADDR).get("socket-binding-group").set(socketBindingGroupName);
            portOp.get(OP_ADDR).get("socket-binding").set(socketBinding);
            portOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
            portOp.get(NAME).set("bound-port");
            final int port = defined(executeForResult(portOp, client), socketBindingGroupName + " -> " + socketBinding + " -> bound-port is undefined").asInt();

            return URI.create(protocol + "://" + NetworkUtils.formatPossibleIpv6Address(ip) + ":" + port);
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }

      private ModelNode executeForResult(final ModelNode operation, ModelControllerClient client) throws Exception {
         final ModelNode result = client.execute(operation);
         checkSuccessful(result, operation);
         return result.get(RESULT);
      }

      private static ModelNode defined(final ModelNode node, final String message) {
         if (!node.isDefined())
            throw new IllegalStateException(message);
         return node;
      }

      private ModelNode readRootNode(ModelControllerClient client) throws Exception {
         final ModelNode operation = new ModelNode();
         operation.get(OP).set(READ_RESOURCE_OPERATION);
         operation.get(RECURSIVE).set("true");
         operation.get(OP_ADDR).set(new ModelNode());

         return executeForResult(operation, client);
      }

      private void checkSuccessful(final ModelNode result,
                                   final ModelNode operation) throws UnSuccessfulOperationException {
         if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new UnSuccessfulOperationException(result.get(
               FAILURE_DESCRIPTION).toString());
         }
      }

      private static class UnSuccessfulOperationException extends Exception {
         private static final long serialVersionUID = 1L;

         public UnSuccessfulOperationException(String message) {
            super(message);
         }
      }
   }


   private static HttpURLConnection connection;
   private static Logger log = Logger.getLogger(WebConsoleSecurityTestCase.class);

   private HttpURLConnection getConnection() throws Exception {
      connection = (HttpURLConnection) ConnectionManager.getConsoleUrl().openConnection();
      assertNotNull(connection);
      log.debug("connection opened");
      connection.setDoInput(true);
      connection.setRequestProperty("Cookie", "MODIFY ME IF NEEDED");
      return connection;
   }
   
   @Deployment
   public static JavaArchive createTestArchive() {
      return ShrinkWrap.create(JavaArchive.class, "test.jar");
   }

   @Test
   public void testGet() throws Exception {
      getConnection().setRequestMethod(HttpGet.METHOD_NAME);
      getConnection().connect();
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getConnection().getResponseCode());
   }

   @Test
   public void testPost() throws Exception {
      getConnection().setRequestMethod(HttpPost.METHOD_NAME);
      getConnection().connect();
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getConnection().getResponseCode());
   }

   @Test
   public void testHead() throws Exception {
      getConnection().setRequestMethod(HttpHead.METHOD_NAME);
      getConnection().connect();
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getConnection().getResponseCode());
   }

   @Test
   public void testOptions() throws Exception {
      getConnection().setRequestMethod(HttpOptions.METHOD_NAME);
      getConnection().connect();
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getConnection().getResponseCode());
   }

   @Test
   public void testPut() throws Exception {
      getConnection().setRequestMethod(HttpPut.METHOD_NAME);
      getConnection().connect();
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getConnection().getResponseCode());
   }

   @Test
   public void testTrace()  throws Exception {
      getConnection().setRequestMethod(HttpTrace.METHOD_NAME);
      getConnection().connect();
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getConnection().getResponseCode());
   }

   @Test
   public void testDelete()  throws Exception {
      getConnection().setRequestMethod(HttpDelete.METHOD_NAME);
      getConnection().connect();
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getConnection().getResponseCode());
   }

}
