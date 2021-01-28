/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jsf.undertow;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jsf.undertow.deployment.SpecificOrder;
import org.jboss.as.test.integration.jsf.undertow.deployment.OrdersSearchQuery;
import org.jboss.as.test.integration.jsf.undertow.deployment.Order;
import org.jboss.as.test.integration.jsf.undertow.deployment.PersonalID;
import org.jboss.as.test.integration.jsf.undertow.deployment.PersonalDetails;
import org.jboss.as.test.integration.jsf.undertow.deployment.PersonalOrder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * Test calls deployed application with a Jakarta Server Faces page. No error should occur when
 * the class called from the Jakarta Server Faces has a superclass and the called method has an
 * Enum as a parameter.
 *
 * Automated test for [ WFLY-11224 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CallEnumParameterMethodTestCase {

    private static final String DEPLOYMENT = "DEPLOYMENT";

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        final Package DEPLOYMENT_PACKAGE = SpecificOrder.class.getPackage();

        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war")
                .addClasses(SpecificOrder.class, Order.class, PersonalOrder.class, PersonalDetails.class, PersonalID.class, OrdersSearchQuery.class)
                .addAsWebResource(DEPLOYMENT_PACKAGE, "search.xhtml", "search.xhtml")
                .addAsWebInfResource(DEPLOYMENT_PACKAGE, "web.xml", "web.xml")
                .addAsWebInfResource(DEPLOYMENT_PACKAGE, "faces-config.xml", "faces-config.xml");
        return war;
    }

    @Test
    public void testCallEnumParameterMethodFromJsf() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String jspUrl = url.toExternalForm() + "search.jsf";
            HttpGet httpget = new HttpGet(jspUrl);
            httpclient.execute(httpget);

            HttpResponse response2 = httpclient.execute(httpget);
            assertEquals(200, response2.getStatusLine().getStatusCode());
        }
    }
}
