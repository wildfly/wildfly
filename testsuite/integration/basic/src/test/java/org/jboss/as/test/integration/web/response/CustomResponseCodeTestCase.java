/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.response;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple test to check if act properly when custom code should be returned.
 *
 * @author baranowb
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomResponseCodeTestCase extends ContainerResourceMgmtTestBase {

    @ArquillianResource
    URL url;

    private HttpClient httpclient = null;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, CustomResponseCodeTestCase.class.getSimpleName() + ".war");
        war.addClasses(SimpleServlet.class, Constants.class);
        return war;
    }

    @Before
    public void setup() {
        this.httpclient = HttpClientBuilder.create().build();
    }

    @Test
    public void testNormalOpMode() throws Exception{
        HttpGet httpget = new HttpGet(url.toString() + Constants.URL_PATTERN);
        HttpResponse response = this.httpclient.execute(httpget);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        httpget = new HttpGet(url.toString() + Constants.URL_PATTERN);
        response = this.httpclient.execute(httpget);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testCustomResponseCode() throws Exception {
        ModelNode operation = createOpNode("subsystem=undertow/server=default-server/host=default-host", "write-attribute");
        operation.get("name").set("custom-response-code");
        operation.get("value").set(506);
        executeOperation(operation);
        reload();
        try {
            HttpGet httpget = new HttpGet(url.toString() + Constants.URL_PATTERN);
            HttpResponse response = this.httpclient.execute(httpget);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            httpget = new HttpGet(url.toString() + Constants.URL_PATTERN);
            response = this.httpclient.execute(httpget);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            operation = createOpNode("subsystem=undertow/server=default-server/host=default-host", "undefine-attribute");
            operation.get("name").set("custom-response-code");
            executeOperation(operation);
            reload();
        }
    }
    
    public void reload() throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        executeOperation(operation);
        boolean reloaded = false;
        int i = 0;
        while (!reloaded) {
            try {
                Thread.sleep(5000);
                if (getManagementClient().isServerInRunningState())
                    reloaded = true;
            } catch (Throwable t) {
                // nothing to do, just waiting
            } finally {
                if (!reloaded && i++ > 10)
                    throw new Exception("Server reloading failed");
            }
        }
    }
}
