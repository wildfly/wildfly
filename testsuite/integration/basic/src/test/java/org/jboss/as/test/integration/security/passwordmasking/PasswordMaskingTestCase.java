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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */

@RunWith(Arquillian.class)
public class PasswordMaskingTestCase {
   
   @ArquillianResource URL baseURL;

   @Deployment
   public static WebArchive deploy(){
      //Utils.stop();

      WebArchive war = ShrinkWrap.create(WebArchive.class, "passwordMasking" + ".war");
      war.addClass(PasswordMaskingTestServlet.class);
      war.setWebXML(Utils.getResource("security/deployments/passwordMasking/web.xml"));

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
