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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.metrics.TestApplication;
import org.wildfly.test.integration.microprofile.metrics.metadata.resources.MicroProfileMetricsHistogramResource;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.microprofile.util.MetricsHelper.getJSONMetrics;
import static org.wildfly.test.microprofile.util.MetricsHelper.getMetricSubValueFromJSONOutput;

/**
 * Regression test for SmallRye Metrics issue:
 * https://github.com/smallrye/smallrye-metrics/issues/42
 * Histogram - registry.histogram(metadata) returns IllegalArgumentException on second invocation of the endpoint.
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("WFLY-11718")
public class MicroProfileMetricsHistogramMultipleInvocationsTestCase {


   /**
    * Json metrics
    */
   private static String json;

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
      WebArchive war = ShrinkWrap.create(WebArchive.class, MicroProfileMetricsHistogramMultipleInvocationsTestCase.class.getSimpleName() + ".war")
            .addClasses(TestApplication.class)
            .addClasses(MicroProfileMetricsHistogramResource.class)
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
    * Perform 10 invocations on an endpoint. After first invocation, pre-existing histogram should be used.
    */
   @Test
   public void testMultipleInvocationsOfEndpoint(@ArquillianResource URL url) throws Exception {
      try (CloseableHttpClient client = HttpClients.createDefault()) {
         for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 2; j++) { // use each number from 1 to 5 twice
               URL endpointURL = new URL(url.toExternalForm() + "microprofile-metrics-app/histogram/hello/" + i);
               HttpGet httpGet = new HttpGet(endpointURL.toExternalForm());
               CloseableHttpResponse resp = client.execute(httpGet);
               assertEquals(200, resp.getStatusLine().getStatusCode());
               String content = EntityUtils.toString(resp.getEntity());
               assertTrue("'Hello World!' message is expected, but was: " + content, content.contains("Hello World!"));
            }
         }
      }

      json = getJSONMetrics(managementClient, "application/helloHistogram", true);

      checkValue("min", 1.0);
      checkValue("max", 5.0);
      checkValue("mean", 3.0);
      checkValue("stddev", 1.4142135623730951);
      checkValue("count", 10.0);
      checkValue("p50", 3.0);
      checkValue("p75", 4.0);
      checkValue("p95", 5.0);
      checkValue("p98", 5.0);
      checkValue("p99", 5.0);
      checkValue("p999", 5.0);
   }

   private static void checkValue(String key, Double expectedValue) {
      Double realValue = getMetricSubValueFromJSONOutput(json, "helloHistogram", key);
      Assert.assertTrue("Expected value of '" + key + "' is " + expectedValue + ", but was " + realValue +".", expectedValue.equals(realValue));
   }
}
