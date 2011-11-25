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

package org.jboss.as.test.clustering.cluster;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
/**
 * Validate the <distributable/> works for single node
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusteredWebTestCase {

    @BeforeClass
    public static void printSysProps() {
        Properties sysprops = System.getProperties() ;
        // System.out.println("system properties:\n" + sysprops) ;
    }

    @Deployment(name="deployment-0")
    @TargetsContainer("clustering-udp-0")
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "distributable.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource(ClusteredWebTestCase.class.getPackage(), "web.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @Test
    @OperateOnDeployment("deployment-0")
    public void test(@ArquillianResource(SimpleServlet.class)URL baseURL) throws ClientProtocolException, IOException {
        DefaultHttpClient client = new DefaultHttpClient();

        // returns the URL of the deployment (http://127.0.0.1:8180/distributable)
        String url = baseURL.toString();
        System.out.println("URL = " + url);

        try {
            HttpResponse response = client.execute(new HttpGet(url+"simple"));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(Integer.parseInt(response.getFirstHeader("value").getValue()), 1);
            Assert.assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            response.getEntity().getContent().close();

            response = client.execute(new HttpGet(url+"simple"));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(Integer.parseInt(response.getFirstHeader("value").getValue()), 2);
            // This won't be true unless we have somewhere to which to replicate
            Assert.assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
