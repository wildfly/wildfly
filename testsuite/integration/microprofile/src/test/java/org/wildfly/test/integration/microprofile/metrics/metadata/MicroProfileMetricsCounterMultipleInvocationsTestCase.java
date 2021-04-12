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

package org.wildfly.test.integration.microprofile.metrics.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getJSONMetrics;

import java.io.StringReader;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.metrics.TestApplication;
import org.wildfly.test.integration.microprofile.metrics.metadata.resources.CustomCounterMetric;
import org.wildfly.test.integration.microprofile.metrics.metadata.resources.MicroProfileMetricsCounterResource;


/**
 * Regression test for SmallRye Metrics issue:
 * https://github.com/smallrye/smallrye-metrics/issues/43
 * Counter - registry.counter(metadata) returns IllegalArgumentException on second invocation of the endpoint.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileMetricsCounterMultipleInvocationsTestCase {


   /**
    * ManagementClient provides management port and hostname
    */
   @ContainerResource
   ManagementClient managementClient;

   /**
    * Prepare deployment
    */
   @Deployment
   public static Archive<?> deploy() {
      WebArchive war = ShrinkWrap.create(WebArchive.class, MicroProfileMetricsCounterMultipleInvocationsTestCase.class.getSimpleName() + ".war")
              .addClasses(TestApplication.class)
              .addClasses(MicroProfileMetricsCounterResource.class, CustomCounterMetric.class)
              .addAsManifestResource(new StringAsset("org.wildfly.test.integration.microprofile.metrics.metadata.resources.CustomCounterMetric.multiplier=2"), "microprofile-config.properties")
              .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      return war;
   }

   /**
    * https://issues.jboss.org/browse/WFLY-11499
    */
   @BeforeClass
   public static void skipSecurityManager() {
      AssumeTestGroupUtil.assumeSecurityManagerDisabled();
   }

   /**
    * Perform 5 invocations on an endpoint. After first invocation, pre-existing counter should be used.
    */
   @Test
   public void testMultipleInvocationsOfEndpoint(@ArquillianResource URL url) throws Exception {
      try (CloseableHttpClient client = HttpClients.createDefault()) {
         for (int i = 1; i <= 5; i++) {
            URL endpointURL = new URL(url.toExternalForm() + "microprofile-metrics-app/counter/hello");
            HttpGet httpGet = new HttpGet(endpointURL.toExternalForm());
            CloseableHttpResponse resp = client.execute(httpGet);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(resp.getEntity());
            assertTrue("'Hello World!' message is expected, but was: " + content, content.contains("Hello World!"));
         }
      }

      String jsonString = getJSONMetrics(managementClient, "application/helloCounter", true);
      JsonReader reader = Json.createReader(new StringReader(jsonString));
      JsonObject jsonObject = reader.readObject();

      int helloCounter = jsonObject.getInt("helloCounter");
      Assert.assertEquals("Unexpected value of 'helloCounter'", 5, helloCounter);

      jsonString = getJSONMetrics(managementClient, "application/customCounter", true);
      reader = Json.createReader(new StringReader(jsonString));
      jsonObject = reader.readObject();
      int customCounter = jsonObject.getInt("customCounter");
      Assert.assertEquals("Unexpected value of 'customCounter'", 10, customCounter);
   }
}