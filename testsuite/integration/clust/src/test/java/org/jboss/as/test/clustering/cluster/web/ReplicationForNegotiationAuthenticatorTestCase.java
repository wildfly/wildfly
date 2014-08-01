/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

public class ReplicationForNegotiationAuthenticatorTestCase extends ClusteredWebFailoverAbstractCase {
    private static final String DEPLOYMENT_NAME = "negotiationAuthenticator.war";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }
       
    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "negotiationAuthenticator.war");
        war.addClasses(SimpleServlet.class, Mutable.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        // Take web.xml from the managed test.
        war.setWebXML(ClusteredWebSimpleTestCase.class.getPackage(), "web.xml");    
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.jboss.security.negotiation"),"jboss-deployment-structure.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset("other", "org.jboss.security.negotiation.NegotiationAuthenticator"), "jboss-web.xml");
        return war;
    }

    @Test
    public void testOneRequestSimpleFailover(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        URI uri1 = SimpleServlet.createURI(baseURL1);
        URI uri2 = SimpleServlet.createURI(baseURL2);

        try {
            
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                log.info("Requested " + uri1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
            
            // Now check on the 2nd server
            
            response = client.execute(new HttpGet(uri2));
            try {
                log.info("Requested " + uri2 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", 2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
           
            //and back on the 1st server
            response = client.execute(new HttpGet(uri1));
            try {
                log.info("Requested " + uri1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was brough up.", 3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
            
        } finally {
            HttpClientUtils.closeQuietly(client);
        }

    }

    @Override
    protected String getDeploymentName() {
        return DEPLOYMENT_NAME;
    }
}