/*
 *
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.jboss.as.test.clustering.single.web.shared;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.DEPLOYMENT_1;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that web applications within an ear can share sessions if configured appropriately, but that its sessions are non-distributable.
 * @author Martin Simka
 */
@RunWith(Arquillian.class)
public class NonDistributableSharedSessionTestCase {

    private static final String MODULE = NonDistributableSharedSessionTestCase.class.getSimpleName();
    private static final String MODULE_1 = "war1";
    private static final String MODULE_2 = "war2";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(name = DEPLOYMENT_1, testable = false)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar");
        jar.addClass(Mutable.class);
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, MODULE_1 + ".war");
        war1.addClass(SimpleServlet.class);
        war1.setWebXML(AbstractWebFailoverTestCase.class.getPackage(), "web.xml");
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, MODULE_2 + ".war");
        war2.addClass(SimpleServlet.class);
        war2.setWebXML(AbstractWebFailoverTestCase.class.getPackage(), "web.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, MODULE + ".ear");
        ear.addAsLibraries(jar);
        ear.addAsModule(war1);
        ear.addAsModule(war2);
        ear.addAsManifestResource(NonDistributableSharedSessionTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
        return ear;
    }

    @Test
    public void test(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURLDep)
            throws URISyntaxException, IOException {
        URI baseURI = new URI(baseURLDep.toExternalForm() + "/");
        URI uri1 = SimpleServlet.createURI(baseURI.resolve(MODULE_1 + "/"));
        URI uri2 = SimpleServlet.createURI(baseURI.resolve(MODULE_2 + "/"));

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            int expected = 1;

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri1))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri2))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            }

            // A distributable web application preserves its web sessions across server restarts, a non-distributable web application does not.
            ServerReload.executeReloadAndWaitForCompletion(this.managementClient);

            expected = 1;
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri1))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri2))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            }
        }
    }
}
