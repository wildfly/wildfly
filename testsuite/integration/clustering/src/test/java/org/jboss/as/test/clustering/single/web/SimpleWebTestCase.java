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
package org.jboss.as.test.clustering.single.web;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validate the <distributable/> works for single node.
 *
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup(SimpleWebTestCase.ServerSetupTask.class)
public class SimpleWebTestCase {

    private static final String MODULE_NAME = SimpleWebTestCase.class.getSimpleName();
    private static final String APPLICATION_NAME = MODULE_NAME + ".war";

    @Deployment(name = DEPLOYMENT_1, testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APPLICATION_NAME);
        war.addClasses(SimpleServlet.class, Mutable.class);
        war.setWebXML(SimpleWebTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    public void test(@ArquillianResource(SimpleServlet.class) URL baseURL, @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient managementClient) throws IOException, URISyntaxException {
        // Validate existence of runtime resource for deployment cache
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "infinispan"), PathElement.pathElement("cache-container", "web"), PathElement.pathElement("cache", APPLICATION_NAME));
        ModelNode operation = Util.createOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, address);
        operation.get(ModelDescriptionConstants.NAME).set(new ModelNode("number-of-entries"));
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(0, result.get(ModelDescriptionConstants.RESULT).asInt());

        URI uri = SimpleServlet.createURI(baseURL);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
                Assert.assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
                // This won't be true unless we have somewhere to which to replicate or session persistence is configured (current default)
                Assert.assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }

        result = managementClient.getControllerClient().execute(operation);
        Assert.assertNotEquals(0, result.get(ModelDescriptionConstants.RESULT).asInt());
    }

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder.node("single")
                    .setup("/subsystem=infinispan/cache-container=web/local-cache=passivation:write-attribute(name=statistics-enabled, value=true)")
                    .teardown("/subsystem=infinispan/cache-container=web/local-cache=passivation:undefine-attribute(name=statistics-enabled)")
                    ;
        }
    }
}
