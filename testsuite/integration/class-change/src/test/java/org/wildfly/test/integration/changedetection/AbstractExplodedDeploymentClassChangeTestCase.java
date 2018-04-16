/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.changedetection;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.undertow.util.StatusCodes;

/**
 * Tests that class and resource replacement works when simply replacing the classes
 * in the exploded deployment
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractExplodedDeploymentClassChangeTestCase {

    @Test
    public void testExplodedDeploymentChanges() throws Exception {
        try (ExplodedDeploymentManager manager = new ExplodedDeploymentManager.Builder(AbstractExplodedDeploymentClassChangeTestCase.class, "test.war")
                .setStrategy(getStrategy())
                .addClasses(TestServlet.class)
                .addWebResources("web-file.txt").buildAndDeploy()) {
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(manager.getDeploymentUrl() + "web-file.txt");
                CloseableHttpResponse response = client.execute(get);
                String test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Web File", test);
                manager.replaceWebResource("web-file.txt", "web-file1.txt");
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Replaced Web File", test);

                get = new HttpGet(manager.getDeploymentUrl() + "test");
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Original", test);
                String uuid = response.getFirstHeader("uuid").getValue();
                manager.replaceClass(TestServlet.class, TestServlet1.class);
                manager.addClass(MessageClass.class);
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Replaced", test);
                Assert.assertEquals(uuid, response.getFirstHeader("uuid").getValue());

                get = new HttpGet(manager.getDeploymentUrl() + "dir/new-file.txt");
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals(StatusCodes.NOT_FOUND, response.getStatusLine().getStatusCode());
                manager.addWebResource("dir/new-file.txt");
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("New File", test);
            }
        }
    }

    protected abstract ExplodedReplacementStrategy getStrategy();
}
