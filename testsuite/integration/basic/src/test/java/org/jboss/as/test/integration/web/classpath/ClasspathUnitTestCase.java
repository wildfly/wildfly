/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.web.classpath;

import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.web.classpath.util.Debug;
import org.jboss.as.test.integration.web.classpath.util.Util;
import org.jboss.as.test.integration.web.classpath.util2.Util2;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of web app classpath issues
 *
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClasspathUnitTestCase {

    private static Logger log = Logger.getLogger(ClasspathUnitTestCase.class);
    
    @ArquillianResource
    protected URL baseURL;

    public String getContextPath() {
        return "manifest";
    }

    @Deployment(testable = false)
    public static EnterpriseArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/classpath/resources/";

        JavaArchive servletJar = ShrinkWrap.create(JavaArchive.class, "jbosstest-web-libservlet.jar");
        servletJar.addClass(SimpleServlet.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "manifest-web.war");
        war.setManifest(resourcesLocation + "manifest-web.mf");
        war.setWebXML(tccl.getResource(resourcesLocation + "manifest-web.xml"));
        war.addAsWebResource(tccl.getResource(resourcesLocation + "classpath.jsp"), "classpath.jsp");
        war.addClass(ClasspathServlet2.class);
        war.addClass(Debug.class);
        war.addAsLibrary(servletJar);

        JavaArchive utilJar = ShrinkWrap.create(JavaArchive.class, "jbosstest-web-util.jar");
        utilJar.setManifest(resourcesLocation + "manifest-util.mf");
        utilJar.addClass(Util.class);

        JavaArchive utilJar2 = ShrinkWrap.create(JavaArchive.class, "jbosstest-web-util2.jar");
        utilJar2.addPackage(Util2.class.getPackage());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "manifest-web.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation+ "application.xml"));
        ear.addAsModule(war);
        ear.addAsModule(utilJar);
        ear.addAsModule(utilJar2);

        System.out.println(ear.toString(true));
        System.out.println(war.toString(true));
        System.out.println(servletJar.toString(true));
        System.out.println(utilJar.toString(true));
        System.out.println(utilJar2.toString(true));
        return ear;
    }

    /**
     * Test of a war that accesses classes referred to via the war manifest
     * classpath. Access the http://{host}/manifest/classpath.jsp
     */
    @Test
    public void testWarManifest() throws Exception {
        URL url = new URL(baseURL + "/classpath.jsp");
        testURL(url);
    }

    /**
     * Access the http://{host}/manifest/ClassesServlet
     */
    @Test
    public void testClassesServlet() throws Exception {
        URL url = new URL(baseURL + "/ClassesServlet?class=org.jboss.as.test.integration.web.classpath.util2.Util2");
        testURL(url);
    }

    /**
     * Access the http://{host}/manifest/LibServlet
     */
    @Test
    public void testLibServlet() throws Exception {
        URL url = new URL(baseURL + "/LibServlet");
        testURL(url);
    }

    private void testURL(URL url) throws Exception {
        HttpGet httpget = new HttpGet(url.toURI());
        DefaultHttpClient httpclient = new DefaultHttpClient();

        log.info("executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-Exception");
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-Exception(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);
    }
}
