/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.web.servlet.preservepath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

@RunAsClient
@RunWith(Arquillian.class)
public class PreservePathTestCase {

   @ArquillianResource
   private URL url;

   @ArquillianResource
   private ManagementClient managementClient;

   final String tempDir = TestSuiteEnvironment.getTmpDir();

   @Before
   public void setUp() throws Exception {
   }

   @After
   public void tearDown() throws Exception {
      File file = new File(tempDir + "/output.txt");
      if (file.exists()) {
         file.delete();
      }

      ModelNode setPreservePathOp = createOpNode("system-property=io.undertow.servlet.dispatch.preserve_path_of_forward", ModelDescriptionConstants.REMOVE);
      CoreUtils.applyUpdate(setPreservePathOp, managementClient.getControllerClient());
      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
   }

   @Deployment
   public static WebArchive deployment() {
      WebArchive war = ShrinkWrap.create(WebArchive.class);
      war.addClass(ForwardingServlet.class);
      war.addClass(PreservePathFilter.class);
      war.add(new UrlAsset(PreservePathTestCase.class.getResource("preserve-path.jsp")), "preserve-path.jsp");
      war.addAsWebInfResource(PreservePathTestCase.class.getPackage(), "web.xml", "web.xml");
      return war;
   }

   @Test
   public void testPreservePath() throws Exception {
      setPreservePathOnForward("true");

      HttpClient httpclient = HttpClientBuilder.create().build();
      HttpGet httpget = new HttpGet(url.toString() + "/test?path="+ URLEncoder.encode(tempDir));
      HttpResponse response = httpclient.execute(httpget);

      Thread.sleep(100);
      FileReader fr = new FileReader(new File(tempDir + "/output.txt"));
      Assert.assertEquals("servletPath: /test", new BufferedReader(fr).readLine());
   }

   @Test
   public void testDontPreservePath() throws Exception {
      setPreservePathOnForward("false");

      HttpClient httpclient = HttpClientBuilder.create().build();
      HttpGet httpget = new HttpGet(url.toString() + "/test?path="+ URLEncoder.encode(tempDir));
      HttpResponse response = httpclient.execute(httpget);

      Thread.sleep(100);
      FileReader fr = new FileReader(new File(tempDir + "/output.txt"));
      Assert.assertEquals("servletPath: /preserve-path.jsp", new BufferedReader(fr).readLine());
   }

   private void setPreservePathOnForward(String s) throws Exception {
      ModelNode setPreservePathOp = createOpNode("system-property=io.undertow.servlet.dispatch.preserve_path_of_forward", ModelDescriptionConstants.ADD);
      setPreservePathOp.get("value").set(s);

      CoreUtils.applyUpdate(setPreservePathOp, managementClient.getControllerClient());

      ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
   }
}
