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

package org.jboss.as.test.integration.ejb.stateful.serialization.deserialization;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test checks if default constructor of non-serializable parent POJO is called in deserialization process.
 * 1. POJO is initialized on client side using it's parent constructor.
 * 2. One of the fields of the object is set to different value.
 * 3. Remote stateless EJB is called and the object is used in the EJB call.
 * 4. Field values received by the server are being checked to see if the default constructor of the non-serializable parent
 *    was called on EJB server side.
 * Test for [ JBMAR-221 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DefaultConstructorNonSerializedParentTestCase {

    public static final String DEPLOYMENT = "deployment";
    public static final String MODULE = "beans";

    @ArquillianResource
    private URL url;

    @Deployment(name = DEPLOYMENT, testable = false)
    public static EnterpriseArchive createDeployment() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT + ".ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar");
        jar.addClasses(HelloBean.class, HelloRemote.class, CacheableObject.class, Response.class, LEContact.class);
        ear.addAsModule(jar);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "servletDeployment.war");
        war.addClasses(DefaultConstructorServlet.class, DefaultConstructorNonSerializedParentTestCase.class, HttpRequest.class);
        war.addAsWebInfResource(DefaultConstructorNonSerializedParentTestCase.class.getPackage(), "WEB-INF/web.xml");
        war.addAsWebInfResource(DefaultConstructorNonSerializedParentTestCase.class.getPackage(), "WEB-INF/jndi.properties", "classes/jndi.properties");
        ear.addAsModule(war);

        return ear;
    }

    @OperateOnDeployment(DEPLOYMENT)
    @Test
    public void testDefaultConstructorNonSerializedParent() throws Exception {
        HttpResponse response = performCall(url);
        StatusLine statusLine = response.getStatusLine();
        assertEquals(200, statusLine.getStatusCode());
    }

    private HttpResponse performCall(URL url) throws Exception {
        HttpResponse response = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String requestURL = url.toExternalForm() + DefaultConstructorServlet.URL_PATTERN;
            final HttpGet request = new HttpGet(requestURL);
            response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            assertTrue(result.contains("mutable_one:true"));
            assertTrue(result.contains("mutable_two:false"));
            assertTrue(result.contains("mutable_three:true"));
        }
        return response;
    }

}
