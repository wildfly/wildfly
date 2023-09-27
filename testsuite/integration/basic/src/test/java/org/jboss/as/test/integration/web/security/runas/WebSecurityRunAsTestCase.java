/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.runas;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Unit Test the RunAs function
 *
 * @author Anil Saldhana
 */
@RunWith(Arquillian.class)
@RunAsClient
@Category(CommonCriteria.class)
public class WebSecurityRunAsTestCase {

    @Deployment
    public static WebArchive deployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure-runas.war");
        war.addClasses(RunAsInitServlet.class, CurrentUserEjb.class, RunAsServlet.class);

        war.addAsWebInfResource(WebSecurityRunAsTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityRunAsTestCase.class.getPackage(), "web.xml", "web.xml");

        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testServletRunAsInInitMethod() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {

            HttpGet httpget = new HttpGet(url.toExternalForm() + "/runAsInit");
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            Assert.assertEquals("anil", result);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }


    @Test
    public void testServletRunAsInMethod() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {

            HttpGet httpget = new HttpGet(url.toExternalForm() + "/runAs");
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            Assert.assertEquals("peter", result);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }
}