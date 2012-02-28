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
package org.jboss.as.test.integration.security.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.security.loginmodules.common.Coding;
import org.jboss.dmr.ModelNode;
import org.jboss.util.Base64;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jan Lanik
 *         <p/>
 *         Common utilities for login modules tests.
 */
public class Utils {

   public static String hash(String target, String algorithm, Coding coding) {
      MessageDigest md = null;
      try {
         md = MessageDigest.getInstance(algorithm);
      } catch (Exception e) {
         e.printStackTrace();
      }
      byte[] bytes = target.getBytes();
      byte[] byteHash = md.digest(bytes);

      String encodedHash = null;

      switch (coding) {
         case BASE_64:
            encodedHash = Base64.encodeBytes(byteHash);
            break;
         case HEX:
            encodedHash = toHex(byteHash);
            break;
         default:
            throw new IllegalArgumentException("Unsuported coding:" + coding.name());
      }

      return encodedHash;
   }


   public static String toHex(byte[] bytes) {
      StringBuffer sb = new StringBuffer(bytes.length * 2);
      for (int i = 0; i < bytes.length; i++) {
         byte b = bytes[i];
         // top 4 bits
         char c = (char) ((b >> 4) & 0xf);
         if (c > 9)
            c = (char) ((c - 10) + 'a');
         else
            c = (char) (c + '0');
         sb.append(c);
         // bottom 4 bits
         c = (char) (b & 0xf);
         if (c > 9)
            c = (char) ((c - 10) + 'a');
         else
            c = (char) (c + '0');
         sb.append(c);
      }
      return sb.toString();
   }

   public static URL getResource(String name) {
      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      return tccl.getResource(name);
   }


   public static synchronized void logToFile(String what, String where) {
      try {
         File file = new File(where);
         if (!file.exists()) {
            file.createNewFile();
         }
         OutputStream os = new FileOutputStream(file, true);
         Writer writer = new OutputStreamWriter(os);
         writer.write(what);
         if (!what.endsWith("\n")) {
            writer.write("\n");
         }
         writer.close();
         os.close();

      } catch (IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   private static final long STOP_DELAY_DEFAULT = 0;

   /**
    *  stops execution of the program indefinitely
    *  useful in testsuite debugging
    */
   public static void stop(){
      stop(STOP_DELAY_DEFAULT);
   }
   
   /**
   stop test execution for a given time interval
   useful for debugging  
   @param delay interval (milliseconds), if delay<=0, interval is considered to be infinite (Long.MAX_VALUE)
    */
   public static void stop(long delay) {
      long currentTime = System.currentTimeMillis();
      long remainingTime = 0<delay ? currentTime + delay - System.currentTimeMillis() : Long.MAX_VALUE;
      while (remainingTime > 0) {
         try {
            Thread.sleep(remainingTime);
         } catch (InterruptedException ex) {
            remainingTime = currentTime + delay - System.currentTimeMillis();
            continue;
         }
      }
   }

   public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
      for (ModelNode update : updates) {
         applyUpdate(update, client);
      }
   }

   public static void applyUpdate(ModelNode update, final ModelControllerClient client) throws Exception {
      ModelNode result = client.execute(new OperationBuilder(update).build());
      if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
         if (result.hasDefined("result")) {
            //System.out.println(result.get("result"));
         }
      } else if (result.hasDefined("failure-description")) {
         throw new RuntimeException(result.get("failure-description").toString());
      } else {
         throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
      }
   }

   public static void removeSecurityDomain(final ModelControllerClient client, final String domainName) throws Exception {
      final List<ModelNode> updates = new ArrayList<ModelNode>();
      ModelNode op = new ModelNode();
      op.get(OP).set(REMOVE);
      op.get(OP_ADDR).add(SUBSYSTEM, "security");
      op.get(OP_ADDR).add(SECURITY_DOMAIN, domainName);
      updates.add(op);

      applyUpdates(updates, client);
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

   public static HttpResponse authAndGetResponse(String URL, String user, String pass) throws Exception {
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

   public static void makeCall(String URL, String user, String pass, int expectedStatusCode) throws Exception {
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


   public static void setPropertiesFile(Map<String, String> props, URL propFile) {
      File userPropsFile = new File(propFile.getFile());
      try {
         Writer writer = new FileWriter(userPropsFile);
         for (Map.Entry<String, String> entry : props.entrySet()) {
            writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
         }
         writer.close();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

   }

   public static File createTempPropFile(Map<String, String> props) {
      try {
         File propsFile = File.createTempFile("props", ".properties");
         propsFile.deleteOnExit();
         Writer writer = new FileWriter(propsFile);
         for (Map.Entry<String, String> entry : props.entrySet()) {
            writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
         }
         writer.close();
         return propsFile;
      } catch (IOException e) {
         throw new RuntimeException("Temporary file could not be created or writen to!", e);
      }
   }

}
