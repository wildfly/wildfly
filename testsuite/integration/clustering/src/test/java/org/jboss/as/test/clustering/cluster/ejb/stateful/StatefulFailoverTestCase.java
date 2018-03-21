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

package org.jboss.as.test.clustering.cluster.ejb.stateful;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.CounterDecorator;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.IncrementorDDInterceptor;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.JDBCResourceManagerConnectionFactoryIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.JMSResourceManagerConnectionFactoryIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.NestedIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.PassivationIncapableIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.PersistenceIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.SimpleIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.servlet.StatefulServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates failover of a SFSB in different contexts
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class StatefulFailoverTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = StatefulFailoverTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(Incrementor.class.getPackage());
        war.addPackage(StatefulServlet.class.getPackage());
        war.addPackage(EJBDirectory.class.getPackage());
        war.setWebXML(StatefulServlet.class.getPackage(), "web.xml");
        war.addAsWebInfResource(new StringAsset("<beans>" +
                "<interceptors><class>" + IncrementorDDInterceptor.class.getName() + "</class></interceptors>" +
                "<decorators><class>" + CounterDecorator.class.getName() + "</class></decorators>" +
                "</beans>"), "beans.xml");
        war.addAsResource(PersistenceIncrementorBean.class.getPackage(), "persistence.xml", "/META-INF/persistence.xml");
        return war;
    }

    /**
     * Validates that a @Stateful(passivationCapable=false) bean does not replicate
     */
    @Test
    public void noFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        URI uri1 = StatefulServlet.createURI(baseURL1, MODULE_NAME, PassivationIncapableIncrementorBean.class.getSimpleName());
        URI uri2 = StatefulServlet.createURI(baseURL2, MODULE_NAME, PassivationIncapableIncrementorBean.class.getSimpleName());

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            assertEquals(1, queryCount(client, uri1));
            assertEquals(2, queryCount(client, uri1));

            assertEquals(0, queryCount(client, uri2));

            undeploy(DEPLOYMENT_2);

            assertEquals(1, queryCount(client, uri1));
            assertEquals(2, queryCount(client, uri1));

            deploy(DEPLOYMENT_2);

            assertEquals(3, queryCount(client, uri1));
            assertEquals(4, queryCount(client, uri1));

            assertEquals(0, queryCount(client, uri2));

            undeploy(DEPLOYMENT_1);

            assertEquals(1, queryCount(client, uri2));
            assertEquals(2, queryCount(client, uri2));

            deploy(DEPLOYMENT_1);

            assertEquals(0, queryCount(client, uri1));
        }
    }

    /**
     * Validates failover on redeploy of a simple @Stateful bean
     */
    @Test
    public void simpleFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        this.failover(SimpleIncrementorBean.class, baseURL1, baseURL2);
    }

    /**
     * Validates failover on redeploy of a @Stateful bean containing injected JDBC resource manager connection factories
     * test for WFLY-30 @Resource injection of Datasource on clustered SFSB fails with serialization error
     */
    @Test
    public void connectionFactoryFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        this.failover(JDBCResourceManagerConnectionFactoryIncrementorBean.class, baseURL1, baseURL2);
    }

    /**
     * Validates failover on redeploy of a @Stateful bean containing injected JMS resource manager connection factories
     */
    @Test
    public void jmsConnectionFactoryFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        this.failover(JMSResourceManagerConnectionFactoryIncrementorBean.class, baseURL1, baseURL2);
    }

    /**
     * Validates failover on redeploy of a simple @Stateful bean
     */
    @Test
    public void persistenceFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        this.failover(PersistenceIncrementorBean.class, baseURL1, baseURL2);
    }

    private void failover(Class<?> beanClass, URL baseURL1, URL baseURL2) throws Exception {

        URI uri1 = StatefulServlet.createURI(baseURL1, MODULE_NAME, beanClass.getSimpleName());
        URI uri2 = StatefulServlet.createURI(baseURL2, MODULE_NAME, beanClass.getSimpleName());

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            assertEquals(1, queryCount(client, uri1));
            assertEquals(2, queryCount(client, uri1));

            assertEquals(3, queryCount(client, uri2));
            assertEquals(4, queryCount(client, uri2));

            undeploy(DEPLOYMENT_2);

            assertEquals(5, queryCount(client, uri1));
            assertEquals(6, queryCount(client, uri1));

            deploy(DEPLOYMENT_2);

            assertEquals(7, queryCount(client, uri1));
            assertEquals(8, queryCount(client, uri1));

            assertEquals(9, queryCount(client, uri2));
            assertEquals(10, queryCount(client, uri2));

            undeploy(DEPLOYMENT_1);

            assertEquals(11, queryCount(client, uri2));
            assertEquals(12, queryCount(client, uri2));

            deploy(DEPLOYMENT_1);

            assertEquals(13, queryCount(client, uri1));
            assertEquals(14, queryCount(client, uri1));

            assertEquals(15, queryCount(client, uri2));
            assertEquals(16, queryCount(client, uri2));
        }
    }

    /**
     * Validates failover on server restart of a simple @Stateful bean
     */
    @Test
    public void failoverOnStop(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        URI uri1 = StatefulServlet.createURI(baseURL1, MODULE_NAME, SimpleIncrementorBean.class.getSimpleName());
        URI uri2 = StatefulServlet.createURI(baseURL2, MODULE_NAME, SimpleIncrementorBean.class.getSimpleName());

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            assertEquals(1, queryCount(client, uri1));
            assertEquals(2, queryCount(client, uri1));

            assertEquals(3, queryCount(client, uri2));
            assertEquals(4, queryCount(client, uri2));

            stop(NODE_2);

            assertEquals(5, queryCount(client, uri1));
            assertEquals(6, queryCount(client, uri1));

            start(NODE_2);

            assertEquals(7, queryCount(client, uri1));
            assertEquals(8, queryCount(client, uri1));

            assertEquals(9, queryCount(client, uri2));
            assertEquals(10, queryCount(client, uri2));

            stop(NODE_1);

            assertEquals(11, queryCount(client, uri2));
            assertEquals(12, queryCount(client, uri2));

            start(NODE_1);

            assertEquals(13, queryCount(client, uri1));
            assertEquals(14, queryCount(client, uri1));

            assertEquals(15, queryCount(client, uri2));
            assertEquals(16, queryCount(client, uri2));
        }
    }

    /**
     * Validates failover of a @Stateful bean containing a nested @Stateful bean
     * containing an injected CDI bean that uses an interceptor and decorator.
     */
    @Test
    public void nestedBeanFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        URI uri1 = StatefulServlet.createURI(baseURL1, MODULE_NAME, NestedIncrementorBean.class.getSimpleName());
        URI uri2 = StatefulServlet.createURI(baseURL2, MODULE_NAME, NestedIncrementorBean.class.getSimpleName());

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            assertEquals(20010101, queryCount(client, uri1));
            assertEquals(20020202, queryCount(client, uri1));

            assertEquals(20030303, queryCount(client, uri2));
            assertEquals(20040404, queryCount(client, uri2));

            undeploy(DEPLOYMENT_2);

            assertEquals(20050505, queryCount(client, uri1));
            assertEquals(20060606, queryCount(client, uri1));

            deploy(DEPLOYMENT_2);

            assertEquals(20070707, queryCount(client, uri1));
            assertEquals(20080808, queryCount(client, uri1));

            assertEquals(20090909, queryCount(client, uri2));
            assertEquals(20101010, queryCount(client, uri2));

            undeploy(DEPLOYMENT_1);

            assertEquals(20111111, queryCount(client, uri2));
            assertEquals(20121212, queryCount(client, uri2));

            deploy(DEPLOYMENT_1);

            assertEquals(20131313, queryCount(client, uri1));
            assertEquals(20141414, queryCount(client, uri1));

            assertEquals(20151515, queryCount(client, uri2));
            assertEquals(20161616, queryCount(client, uri2));
        }
    }

    private static int queryCount(HttpClient client, URI uri) throws IOException {
        HttpResponse response = client.execute(new HttpGet(uri));
        try {
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            return Integer.parseInt(response.getFirstHeader("count").getValue());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }
}
