/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.servlet.servletSessionConfig;


import static org.junit.Assert.fail;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;



@RunAsClient
@RunWith(Arquillian.class)
public class SessionConfigTestCase {

   @ArquillianResource
   private URL url;

   @Deployment
   public static WebArchive deployment() {
      final WebArchive war = ShrinkWrap.create(WebArchive.class, "session-config.war");
      war.addClass(SessionPositiveServlet.class);
      war.addAsWebInfResource(SessionConfigTestCase.class.getPackage(), "web.xml", "web.xml");

      return war;
   }

   @Test
   public void testCookieStuff() throws Exception {
       final HttpClient httpclient = HttpClientBuilder.create().build();
       final HttpGet httpget = new HttpGet(url.toString() + "/test");
       final HttpResponse response = httpclient.execute(httpget);
       final Header[] hdrs = response.getHeaders("Set-Cookie");
       Assert.assertEquals(1, hdrs.length);
       final Header h = hdrs[0];
       final Map<String, String> values = convertHeader(h);
       Assert.assertNotNull(values.get("Expires"));
       Assert.assertNotNull(values.get("session"));
       Assert.assertNotNull(values.get("path"));
       Assert.assertEquals("/", values.get("path"));
       Assert.assertNotNull(values.get("domain"));
       Assert.assertEquals(".jboss.org", values.get("domain"));
       Assert.assertNotNull(values.get("Max-Age"));
       Assert.assertEquals("10", values.get("Max-Age"));
       Assert.assertNotNull(values.get("SameSite"));
       Assert.assertEquals("None", values.get("SameSite"));
       Assert.assertNotNull(values.get("foo"));
       Assert.assertEquals("bar", values.get("foo"));
       Assert.assertTrue(values.containsKey("secure"));
       Assert.assertTrue(values.containsKey("HttpOnly"));
   }

   private Map<String, String> convertHeader(Header h) {
       //NOTE: h.getElements wont cover attribs
       final Map<String, String> map = new TreeMap<String, String>();
       for(String kvp: h.getValue().split(";")) {
           final String[] kvpSplit = kvp.split("=");
           final String k = kvpSplit[0].trim();
           final String v = kvpSplit.length == 2 ? kvpSplit[1] : null;
           if(!map.containsKey(k)) {
               map.put(k, v);
           } else {
               fail("Duplicate: "+k);
           }
       }
       return map;
   }

}