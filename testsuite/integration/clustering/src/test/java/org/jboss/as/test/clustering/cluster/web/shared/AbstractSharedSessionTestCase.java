/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.web.shared;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Base class for distributed shared session tests.
 * @author Paul Ferraro
 */
public abstract class AbstractSharedSessionTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_1 = "web1";
    private static final String MODULE_2 = "web2";

    static Archive<?> getDeployment(String module, String fileName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, module + ".jar");
        jar.addClass(Mutable.class);
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, MODULE_1 + ".war");
        war1.addClass(SimpleServlet.class);
        war1.setWebXML(AbstractWebFailoverTestCase.class.getPackage(), "web.xml");
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, MODULE_2 + ".war");
        war2.addClass(SimpleServlet.class);
        war2.setWebXML(AbstractWebFailoverTestCase.class.getPackage(), "web.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, module + ".ear");
        ear.addAsLibraries(jar);
        ear.addAsModule(war1);
        ear.addAsModule(war2);
        ear.addAsManifestResource(AbstractSharedSessionTestCase.class.getPackage(), fileName, "jboss-all.xml");
        return ear;
    }

    @Test
    public void test(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURLDep1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL baseURLDep2)
            throws URISyntaxException, IOException {
        URI baseURI1 = new URI(baseURLDep1.toExternalForm() + "/");
        URI baseURI2 = new URI(baseURLDep2.toExternalForm() + "/");

        URI uri11 = SimpleServlet.createURI(baseURI1.resolve(MODULE_1 + "/"));
        URI uri12 = SimpleServlet.createURI(baseURI1.resolve(MODULE_2 + "/"));
        URI uri21 = SimpleServlet.createURI(baseURI2.resolve(MODULE_1 + "/"));
        URI uri22 = SimpleServlet.createURI(baseURI2.resolve(MODULE_2 + "/"));

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            int expected = 1;
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri11))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri12))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri21))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri22))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(expected++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            }
        }
    }
}
