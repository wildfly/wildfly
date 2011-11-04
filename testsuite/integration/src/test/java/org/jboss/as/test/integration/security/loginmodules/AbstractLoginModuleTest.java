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
package org.jboss.as.test.integration.security.loginmodules;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jan Lanik
 *
 * Contains common functionality for logim modules tests.
 */
@RunWith(Arquillian.class)
public abstract class AbstractLoginModuleTest {

   protected static Map<Class, Map<String, String>> classUserMap = new HashMap<Class, Map<String, String>>();

   protected static Map<Class, Map<String, String>> classModuleOptionsMap = new HashMap<Class, Map<String, String>>();

   protected abstract String getContextPath();

   protected static void removeSecurityDomain(final ModelControllerClient client, final String domainName) throws Exception {

      ModelNode op = new ModelNode();
      op.get(OP).set(REMOVE);
      op.get(OP_ADDR).add(SUBSYSTEM, "security");
      op.get(OP_ADDR).add(SECURITY_DOMAIN, domainName);
      // Don't rollback when the AS detects the war needs the module
      op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

      applyUpdate(op, client, true);
   }

   protected HttpResponse authAndGetResponse(String URL, String user, String pass) throws Exception {
      DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response;
      HttpGet httpget = new HttpGet(URL);

      response = httpclient.execute(httpget);

      HttpEntity entity = response.getEntity();
      if (entity != null)
         EntityUtils.consume(entity);

      // We should get the Login Page
      StatusLine statusLine = response.getStatusLine();
      System.out.println("Login form get: " + statusLine);
      assertEquals(200, statusLine.getStatusCode());

      System.out.println("Initial set of cookies:");
      List<Cookie> cookies = httpclient.getCookieStore().getCookies();
      if (cookies.isEmpty()) {
         System.out.println("None");
      } else {
         for (int i = 0; i < cookies.size(); i++) {
            System.out.println("- " + cookies.get(i).toString());
         }
      }

      // We should now login with the user name and password
      HttpPost httpost = new HttpPost(URL + "/j_security_check");

      List<NameValuePair> nvps = new ArrayList<NameValuePair>();
      nvps.add(new BasicNameValuePair("j_username", user));
      nvps.add(new BasicNameValuePair("j_password", pass));

      httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

      response = httpclient.execute(httpost);


      int statusCode = response.getStatusLine().getStatusCode();

      assertTrue((302 == statusCode) || (200 == statusCode));
      // Post authentication - if succesfull, we have a 302 and have to redirect
      if (302 == statusCode) {
         entity = response.getEntity();
         if (entity != null) {
            EntityUtils.consume(entity);
         }
         Header locationHeader = response.getFirstHeader("Location");
         String location = locationHeader.getValue();
         HttpGet httpGet = new HttpGet(location);
         response = httpclient.execute(httpGet);
      }

      return response;
   }

   protected void makeCall(String URL, String user, String pass, int expectedStatusCode) throws Exception {
      DefaultHttpClient httpclient = new DefaultHttpClient();
      try {
         HttpGet httpget = new HttpGet(URL);

         HttpResponse response = httpclient.execute(httpget);

         HttpEntity entity = response.getEntity();
         if (entity != null)
            EntityUtils.consume(entity);

         // We should get the Login Page
         StatusLine statusLine = response.getStatusLine();
         System.out.println("Login form get: " + statusLine);
         assertEquals(200, statusLine.getStatusCode());

         System.out.println("Initial set of cookies:");
         List<Cookie> cookies = httpclient.getCookieStore().getCookies();
         if (cookies.isEmpty()) {
            System.out.println("None");
         } else {
            for (int i = 0; i < cookies.size(); i++) {
               System.out.println("- " + cookies.get(i).toString());
            }
         }

         // We should now login with the user name and password
         HttpPost httpost = new HttpPost(URL + "/j_security_check");

         List<NameValuePair> nvps = new ArrayList<NameValuePair>();
         nvps.add(new BasicNameValuePair("j_username", user));
         nvps.add(new BasicNameValuePair("j_password", pass));

         httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

         response = httpclient.execute(httpost);
         entity = response.getEntity();
         if (entity != null)
            EntityUtils.consume(entity);

         statusLine = response.getStatusLine();

         // Post authentication - we have a 302
         assertEquals(302, statusLine.getStatusCode());
         Header locationHeader = response.getFirstHeader("Location");
         String location = locationHeader.getValue();

         HttpGet httpGet = new HttpGet(location);
         response = httpclient.execute(httpGet);

         entity = response.getEntity();
         if (entity != null)
            EntityUtils.consume(entity);

         System.out.println("Post logon cookies:");
         cookies = httpclient.getCookieStore().getCookies();
         if (cookies.isEmpty()) {
            System.out.println("None");
         } else {
            for (int i = 0; i < cookies.size(); i++) {
               System.out.println("- " + cookies.get(i).toString());
            }
         }

         // Either the authentication passed or failed based on the expected status code
         statusLine = response.getStatusLine();
         assertEquals(expectedStatusCode, statusLine.getStatusCode());
      } finally {
         // When HttpClient instance is no longer needed,
         // shut down the connection manager to ensure
         // immediate deallocation of all system resources
         httpclient.getConnectionManager().shutdown();
      }
   }

   public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
      for (ModelNode update : updates) {
         applyUpdate(update, client, false);
      }
   }

   public static void applyUpdate(ModelNode update, final ModelControllerClient client, boolean allowFailure) throws Exception {
      ModelNode result = client.execute(new OperationBuilder(update).build());
      if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
         if (result.hasDefined("result")) {
            //System.out.println(result.get("result"));
         };
      } else if (result.hasDefined("failure-description")) {
         throw new RuntimeException(result.get("failure-description").toString());
      } else {
         throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
      }
   }

   public static String getContent(HttpResponse response) throws IOException {
      InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
      StringBuilder content = new StringBuilder();
      int c;
      while (-1 != (c = reader.read())) {
         content.append((char) c);
      }
      reader.close();
      return content.toString();
   }

}

