/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.stateful;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.TimeoutIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.servlet.AbstractStatefulServlet;
import org.jboss.as.test.clustering.cluster.ejb.stateful.servlet.StatefulServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractStatefulTimeoutTestCase extends AbstractClusteringTestCase {

    private final String module;

    protected AbstractStatefulTimeoutTestCase(String module) {
        this.module = module;
    }

    private static final long WAIT_FOR_TIMEOUT = TimeoutUtil.adjust(5000);

    protected static WebArchive createDeployment(String module) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, module + ".war");
        war.addClasses(TimeoutIncrementorBean.class, Incrementor.class);
        war.addClasses(StatefulServlet.class, AbstractStatefulServlet.class);
        war.addPackage(EJBDirectory.class.getPackage());
        war.setWebXML(StatefulServlet.class.getPackage(), "web.xml");
        return war;
    }

    /**
     * Validates that a @Stateful(passivationCapable=false) bean does not replicate
     */
    @Test
    public void timeout(@ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        URI uri1 = StatefulServlet.createURI(baseURL1, this.module, TimeoutIncrementorBean.class.getSimpleName());
        URI uri2 = StatefulServlet.createURI(baseURL2, this.module, TimeoutIncrementorBean.class.getSimpleName());

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            assertEquals(1, queryCount(client, uri1));
            assertEquals(2, queryCount(client, uri1));

            // Make sure state replicated correctly
            assertEquals(3, queryCount(client, uri2));
            assertEquals(4, queryCount(client, uri2));

            Thread.sleep(WAIT_FOR_TIMEOUT);

            // SFSB should have timed out
            assertEquals(0, queryCount(client, uri1));
            // Subsequent request will create it again
            assertEquals(1, queryCount(client, uri1));

            Thread.sleep(WAIT_FOR_TIMEOUT);

            // Make sure SFSB times out on other node too
            assertEquals(0, queryCount(client, uri2));
        }
    }

    private static int queryCount(CloseableHttpClient client, URI uri) throws IOException {
        try (CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            return Integer.parseInt(response.getFirstHeader("count").getValue());
        }
    }
}
