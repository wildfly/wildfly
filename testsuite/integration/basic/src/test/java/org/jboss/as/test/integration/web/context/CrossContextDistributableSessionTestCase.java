/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.web.context;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * Test deploys two web applications using the cross-contex and accesses the servlet in the first deployment twice. All the http requests should pass without error.
 * Test for [ UNDERTOW-1415 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CrossContextDistributableSessionTestCase {

    private static final String DEPLOYMENT1 = "deployment1";
    private static final String DEPLOYMENT2 = "deployment2";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT1)
    private URL url1;

    @Deployment(name = DEPLOYMENT2)
    public static WebArchive deployment2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT2 + ".war")
                .addClass(Servlet2.class)
                .addAsWebInfResource(CrossContextDistributableSessionTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(CrossContextDistributableSessionTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        return war;
    }

    @Deployment(name = DEPLOYMENT1)
    public static WebArchive deployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT1 + ".war")
                .addClass(Servlet1.class)
                .addAsWebInfResource(CrossContextDistributableSessionTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(CrossContextDistributableSessionTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                .addAsWebResource(CrossContextDistributableSessionTestCase.class.getPackage(), "index.jsp", "index.jsp");
        return war;
    }

    @Test
    public void testCrossContextDistributableSession()
            throws Exception {

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String jspUrl = url1.toExternalForm() + "servlet1?op=include";
            HttpGet httpget = new HttpGet(jspUrl);
            httpclient.execute(httpget);

            HttpResponse response2 = httpclient.execute(httpget);
            assertEquals(200, response2.getStatusLine().getStatusCode());
        }
    }
}
