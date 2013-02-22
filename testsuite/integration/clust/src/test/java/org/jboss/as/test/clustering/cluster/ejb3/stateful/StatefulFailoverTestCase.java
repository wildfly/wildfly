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

package org.jboss.as.test.clustering.cluster.ejb3.stateful;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_2;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.AbstractEJBDirectory;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.LocalEJBDirectory;
import org.jboss.as.test.clustering.ViewChangeListener;
import org.jboss.as.test.clustering.ViewChangeListenerBean;
import org.jboss.as.test.clustering.ViewChangeListenerServlet;
import org.jboss.as.test.clustering.cluster.ejb3.stateful.bean.CounterDecorator;
import org.jboss.as.test.clustering.cluster.ejb3.stateful.bean.StatefulBean;
import org.jboss.as.test.clustering.cluster.ejb3.stateful.bean.StatefulCDIInterceptor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("AS7-5208 Intermittent failures")
public class StatefulFailoverTestCase {

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void printSysProps() {
        Properties sysprops = System.getProperties();
        System.out.println("System properties:\n" + sysprops);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "stateful.war");
        war.addPackage(StatefulBean.class.getPackage());
        war.addClasses(EJBDirectory.class, AbstractEJBDirectory.class, LocalEJBDirectory.class);
        war.setWebXML(StatefulBean.class.getPackage(), "web.xml");
        war.addAsWebInfResource(new StringAsset("<beans>" +
                "<interceptors><class>" + StatefulCDIInterceptor.class.getName() + "</class></interceptors>" +
                "<decorators><class>" + CounterDecorator.class.getName() + "</class></decorators>" +
                "</beans>"), "beans.xml");
        war.addClasses(ViewChangeListener.class, ViewChangeListenerBean.class, ViewChangeListenerServlet.class);
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.msc, org.jboss.as.clustering.common, org.infinispan\n"));
        System.out.println(war.toString(true));
        return war;
    }

    @Test
    @InSequence(1)
    public void testArquillianWorkaround() {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);

        // TODO: This is nasty. I need to start it to be able to inject it later and then stop it again!
        // https://community.jboss.org/thread/176096
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
    }

    @Test
    @InSequence(2)
    public void testRestart(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, URISyntaxException {

        // TODO: This is nasty. I need to start it to be able to inject it later and then stop it again!
        // https://community.jboss.org/thread/176096
        deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        String url1 = baseURL1.toString() + "count";
        String url2 = baseURL2.toString() + "count";

        System.out.println("URLs are: " + url1 + ", " + url2);

        try {
            this.establishView(client, baseURL1, NODE_1);
            
            assertEquals(20010101, this.queryCount(client, url1));
            assertEquals(20020202, this.queryCount(client, url1));

            controller.start(CONTAINER_2);
            deployer.deploy(DEPLOYMENT_2);

            this.establishView(client, baseURL1, NODE_1, NODE_2);
            
            assertEquals(20030303, this.queryCount(client, url1));
            assertEquals(20040404, this.queryCount(client, url1));

            assertEquals(20050505, this.queryCount(client, url2));
            assertEquals(20060606, this.queryCount(client, url2));

            controller.stop(CONTAINER_2);

            this.establishView(client, baseURL1, NODE_1);
            
            assertEquals(20070707, this.queryCount(client, url1));
            assertEquals(20080808, this.queryCount(client, url1));

            controller.start(CONTAINER_2);

            this.establishView(client, baseURL1, NODE_1, NODE_2);
            
            assertEquals(20090909, this.queryCount(client, url1));
            assertEquals(20101010, this.queryCount(client, url1));

            assertEquals(20111111, this.queryCount(client, url2));
            assertEquals(20121212, this.queryCount(client, url2));

            controller.stop(CONTAINER_1);
            
            this.establishView(client, baseURL2, NODE_2);
            
            assertEquals(20131313, this.queryCount(client, url2));
            assertEquals(20141414, this.queryCount(client, url2));

            controller.start(CONTAINER_1);

            this.establishView(client, baseURL2, NODE_1, NODE_2);
            
            assertEquals(20151515, this.queryCount(client, url1));
            assertEquals(20161616, this.queryCount(client, url1));

            assertEquals(20171717, this.queryCount(client, url1));
            assertEquals(20181818, this.queryCount(client, url1));
        } finally {
            HttpClientUtils.closeQuietly(client);

            this.cleanup(DEPLOYMENT_1, CONTAINER_1);
            this.cleanup(DEPLOYMENT_2, CONTAINER_2);
        }
    }

    @Test
    @InSequence(10)
    public void testArquillianWorkaroundSecond() {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);

        // TODO: This is nasty. I need to start it to be able to inject it later and then stop it again!
        // https://community.jboss.org/thread/176096
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
    }

    @Test
    @InSequence(11)
    public void testRedeploy(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, URISyntaxException {

        // TODO: This is nasty. I need to start it to be able to inject it later and then stop it again!
        // https://community.jboss.org/thread/176096
        deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        String url1 = baseURL1.toString() + "count";
        String url2 = baseURL2.toString() + "count";

        try {
            this.establishView(client, baseURL1, NODE_1);
            
            assertEquals(20010101, this.queryCount(client, url1));
            assertEquals(20020202, this.queryCount(client, url1));

            controller.start(CONTAINER_2);
            deployer.deploy(DEPLOYMENT_2);

            this.establishView(client, baseURL1, NODE_1, NODE_2);
            
            assertEquals(20030303, this.queryCount(client, url1));
            assertEquals(20040404, this.queryCount(client, url1));

            assertEquals(20050505, this.queryCount(client, url2));
            assertEquals(20060606, this.queryCount(client, url2));

            deployer.undeploy(DEPLOYMENT_2);

            this.establishView(client, baseURL1, NODE_1);
            
            assertEquals(20070707, this.queryCount(client, url1));
            assertEquals(20080808, this.queryCount(client, url1));

            deployer.deploy(DEPLOYMENT_2);

            this.establishView(client, baseURL1, NODE_1, NODE_2);
            
            assertEquals(20090909, this.queryCount(client, url1));
            assertEquals(20101010, this.queryCount(client, url1));

            assertEquals(20111111, this.queryCount(client, url2));
            assertEquals(20121212, this.queryCount(client, url2));

            deployer.undeploy(DEPLOYMENT_1);

            this.establishView(client, baseURL2, NODE_2);
            
            assertEquals(20131313, this.queryCount(client, url2));
            assertEquals(20141414, this.queryCount(client, url2));

            deployer.deploy(DEPLOYMENT_1);

            this.establishView(client, baseURL2, NODE_1, NODE_2);
            
            assertEquals(20151515, this.queryCount(client, url1));
            assertEquals(20161616, this.queryCount(client, url1));

            assertEquals(20171717, this.queryCount(client, url2));
            assertEquals(20181818, this.queryCount(client, url2));
        } finally {
            HttpClientUtils.closeQuietly(client);

            this.cleanup(DEPLOYMENT_1, CONTAINER_1);
            this.cleanup(DEPLOYMENT_2, CONTAINER_2);
        }
    }

    private int queryCount(HttpClient client, String url) throws IOException {
        HttpResponse response = client.execute(new HttpGet(url));
        try {
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            return Integer.parseInt(response.getFirstHeader("count").getValue());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private void cleanup(String deployment, String container) {
        try {
            this.deployer.undeploy(deployment);
            this.controller.stop(container);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    private void establishView(HttpClient client, URL baseURL, String... members) throws URISyntaxException, IOException {
        ClusterHttpClientUtil.establishView(client, baseURL, "web", members);
        ClusterHttpClientUtil.establishView(client, baseURL, "ejb", members);
    }
}
