/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.management.console;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.test.http.Authentication;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case to test custom / constant headers are applied to /console.
 * See WFCORE-1110.
 *
 * @author <a href="mailto:tterem@redhat.com">Tomas Terem</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpManagementConstantHeadersTestCase {

   private static final int MGMT_PORT = 9990;
   private static final String ROOT_CTX = "/";
   private static final String CONSOLE_CTX = "/console";
   private static final String ERROR_CTX = "/error";
   private static final String METRICS_CTX = "/metrics";

   private static final String TEST_VALUE = "TestValue";

   private static final PathAddress INTERFACE_ADDRESS = PathAddress.pathAddress(PathElement.pathElement("core-service", "management"),
         PathElement.pathElement("management-interface", "http-interface"));

   @ContainerResource
   protected ManagementClient managementClient;

   private URL managementConsoleUrl;
   private URL errorUrl;
   private URL metricsUrl;
   private HttpClient httpClient;

   @Before
   public void createClient() throws Exception {
      String address = managementClient.getMgmtAddress();
      this.managementConsoleUrl = new URL("http", address, MGMT_PORT, CONSOLE_CTX);
      this.errorUrl = new URL("http", address, MGMT_PORT, ERROR_CTX);
      this.metricsUrl = new URL("http", address, MGMT_PORT, METRICS_CTX);

      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(new AuthScope(managementConsoleUrl.getHost(), managementConsoleUrl.getPort()), new UsernamePasswordCredentials(Authentication.USERNAME, Authentication.PASSWORD));

      this.httpClient = HttpClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .build();
   }

   @After
   public void closeClient() {
      if (httpClient instanceof Closeable) {
         try {
            ((Closeable) httpClient).close();
         } catch (IOException e) {
            Logger.getLogger(HttpManagementConstantHeadersTestCase.class).error("Failed closing client", e);
         }
      }
   }

   private void activateHeaders() throws Exception {
      Map<String, List<Map<String, String>>> headersMap = new HashMap<>();
      headersMap.put(ROOT_CTX, Collections.singletonList(Collections.singletonMap("X-All", "All")));
      headersMap.put(CONSOLE_CTX, Collections.singletonList(Collections.singletonMap("X-Management", "Management")));
      headersMap.put(ERROR_CTX, Collections.singletonList(Collections.singletonMap("X-Error", "Error")));

      managementClient.getControllerClient().execute(createConstantHeadersOperation(headersMap));

      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
   }

   private static ModelNode createConstantHeadersOperation(final Map<String, List<Map<String, String>>> constantHeadersValues) {
      ModelNode writeAttribute = new ModelNode();
      writeAttribute.get("address").set(INTERFACE_ADDRESS.toModelNode());
      writeAttribute.get("operation").set("write-attribute");
      writeAttribute.get("name").set("constant-headers");

      ModelNode constantHeaders = new ModelNode();
      for (Entry<String, List<Map<String, String>>> entry : constantHeadersValues.entrySet()) {
         for (Map<String, String> header: entry.getValue()) {
            constantHeaders.add(createHeaderMapping(entry.getKey(), header));
         }
      }

      writeAttribute.get("value").set(constantHeaders);

      return writeAttribute;
   }

   private static ModelNode createHeaderMapping(final String path, final Map<String, String> headerValues) {
      ModelNode headerMapping = new ModelNode();
      headerMapping.get("path").set(path);
      ModelNode headers = new ModelNode();
      headers.add();     // Ensure the type of 'headers' is List even if no content is added.
      headers.remove(0);
      for (Entry<String, String> entry : headerValues.entrySet()) {
         ModelNode singleMapping = new ModelNode();
         singleMapping.get("name").set(entry.getKey());
         singleMapping.get("value").set(entry.getValue());
         headers.add(singleMapping);
      }
      headerMapping.get("headers").set(headers);

      return headerMapping;
   }

   @After
   public void removeHeaders() throws Exception {
      ModelNode undefineAttribute = new ModelNode();
      undefineAttribute.get("address").set(INTERFACE_ADDRESS.toModelNode());
      undefineAttribute.get("operation").set("undefine-attribute");
      undefineAttribute.get("name").set("constant-headers");

      managementClient.getControllerClient().execute(undefineAttribute);

      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
   }

   /**
    * Test that a call to the '/console' endpoint returns the expected headers.
    */
   @Test
   public void testConsole() throws Exception {
      activateHeaders();

      HttpGet get = new HttpGet(managementConsoleUrl.toURI().toString());
      HttpResponse response = httpClient.execute(get);
      assertEquals(200, response.getStatusLine().getStatusCode());

      Header header = response.getFirstHeader("X-All");
      assertEquals("All", header.getValue());

      header = response.getFirstHeader("X-Management");
      assertEquals("Management", header.getValue());

      header = response.getFirstHeader("X-Error");
      assertNull("Header X-Error Unexpected", header);
   }

   /**
    * Test that a call to the '/error' endpoint returns the expected headers.
    */
   @Test
   public void testError() throws Exception {
      activateHeaders();

      HttpGet get = new HttpGet(errorUrl.toURI().toString());
      HttpResponse response = httpClient.execute(get);
      assertEquals(200, response.getStatusLine().getStatusCode());

      Header header = response.getFirstHeader("X-All");
      assertEquals("All", header.getValue());

      header = response.getFirstHeader("X-Error");
      assertEquals("Error", header.getValue());

      header = response.getFirstHeader("X-Management");
      assertNull("Header X-Management Unexpected", header);
   }

   /**
    * Test that security headers can be configured and are returned from '/console' endpoint
    */
   @Test
   public void testSecurity() throws Exception {
      Map<String, List<Map<String, String>>> headersMap = new HashMap<>();

      List<Map<String, String>> headers = new LinkedList<>();
      headers.add(Collections.singletonMap("X-XSS-Protection", "1; mode=block"));
      headers.add(Collections.singletonMap("X-Frame-Options", "SAMEORIGIN"));
      headers.add(Collections.singletonMap("Content-Security-Policy", "default-src https: data: 'unsafe-inline' 'unsafe-eval'"));
      headers.add(Collections.singletonMap("Strict-Transport-Security", "max-age=31536000; includeSubDomains;"));
      headers.add(Collections.singletonMap("X-Content-Type-Options", "nosniff"));

      headersMap.put(CONSOLE_CTX, headers);

      managementClient.getControllerClient().execute(createConstantHeadersOperation(headersMap));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());

      HttpGet get = new HttpGet(managementConsoleUrl.toURI().toString());
      HttpResponse response = httpClient.execute(get);
      assertEquals(200, response.getStatusLine().getStatusCode());

      Header header = response.getFirstHeader("X-XSS-Protection");
      assertEquals("1; mode=block", header.getValue());

      header = response.getFirstHeader("X-Frame-Options");
      assertEquals("SAMEORIGIN", header.getValue());

      header = response.getFirstHeader("Content-Security-Policy");
      assertEquals("default-src https: data: 'unsafe-inline' 'unsafe-eval'", header.getValue());

      header = response.getFirstHeader("Strict-Transport-Security");
      assertEquals("max-age=31536000; includeSubDomains;", header.getValue());

      header = response.getFirstHeader("X-Content-Type-Options");
      assertEquals("nosniff", header.getValue());
   }

   /**
    * Test that configured headers override original headers set by /console endpoint (except for disallowed headers)
    */
   @Test
   public void testHeadersOverride() throws Exception {
      HttpGet get = new HttpGet(managementConsoleUrl.toURI().toString());
      HttpResponse response = httpClient.execute(get);
      assertEquals(200, response.getStatusLine().getStatusCode());

      Header[] headerArray = response.getAllHeaders();

      List<Header> headerList = Arrays.stream(headerArray)
            .filter(header -> !header.getName().equals("Connection") && !header.getName().equals("Date")
                  && !header.getName().equals("Transfer-Encoding") && !header.getName().equals("Content-Type")
                  && !header.getName().equals("Content-Length"))
            .collect(Collectors.toList());

      Map<String, List<Map<String, String>>> headersMap = new HashMap<>();
      List<Map<String, String>> headers = new LinkedList<>();

      for (Header header : headerList) {
         headers.add(Collections.singletonMap(header.getName(), TEST_VALUE));
      }

      headersMap.put(CONSOLE_CTX, headers);

      managementClient.getControllerClient().execute(createConstantHeadersOperation(headersMap));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());

      get = new HttpGet(managementConsoleUrl.toURI().toString());
      response = httpClient.execute(get);
      assertEquals(200, response.getStatusLine().getStatusCode());

      for (Header header : headerList) {
         assertEquals(TEST_VALUE, response.getFirstHeader(header.getName()).getValue());
      }
   }

   /**
    * Test that configured headers override original headers set by non-management /metrics endpoint (except for disallowed headers)
    */
   @Test
   public void testHeadersOverrideNonManagementEndpoint() throws Exception {
      Assume.assumeTrue(checkIfMetricsUsable());
      HttpGet get = new HttpGet(metricsUrl.toURI().toString());
      HttpResponse response = httpClient.execute(get);
      assertEquals(200, response.getStatusLine().getStatusCode());

      Header[] headerArray = response.getAllHeaders();

      List<Header> headerList = Arrays.stream(headerArray)
            .filter(header -> !header.getName().equals("Connection") && !header.getName().equals("Date")
                  && !header.getName().equals("Transfer-Encoding") && !header.getName().equals("Content-Type")
                  && !header.getName().equals("Content-Length"))
            .collect(Collectors.toList());

      Map<String, List<Map<String, String>>> headersMap = new HashMap<>();
      List<Map<String, String>> headers = new LinkedList<>();

      for (Header header : headerList) {
         headers.add(Collections.singletonMap(header.getName(), TEST_VALUE));
      }

      headersMap.put(METRICS_CTX, headers);

      managementClient.getControllerClient().execute(createConstantHeadersOperation(headersMap));
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());

      get = new HttpGet(metricsUrl.toURI().toString());
      response = httpClient.execute(get);
      assertEquals(200, response.getStatusLine().getStatusCode());

      for (Header header : headerList) {
         assertEquals(TEST_VALUE, response.getFirstHeader(header.getName()).getValue());
      }
   }

   /**
    * Check if we cannot assume the use of metrics to run a test. We could have this situation when we are testing layers
    * and the current server provision does not configure metrics.
    */
   private boolean checkIfMetricsUsable() throws IOException {
      if (Boolean.getBoolean("ts.layers")) {
         ModelNode metricsReadOp = Operations.createReadResourceOperation(PathAddress.pathAddress(SUBSYSTEM, "metrics"));
         ModelNode result = managementClient.getControllerClient().execute(metricsReadOp);
         if (!result.get(OUTCOME).asString().equals(SUCCESS)) {
            return false;
         }
      }
      return false;
   }
}
