/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.unmanaged.singleton;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.unmanaged.singleton.service.MyService;
import org.jboss.as.test.clustering.unmanaged.singleton.service.MyServiceContextListener;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

@RunWith(Arquillian.class)
@RunAsClient
public class SingletonTestCase {

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

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "singleton.war");
        war.addPackage(MyService.class.getPackage());
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.msc, org.jboss.as.clustering.singleton, org.jboss.as.server, org.jboss.marshalling, org.jgroups\n"));
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
    public void testSingletonService(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException {

        // TODO: This is nasty. I need to start it to be able to inject it later and then stop it again!
        // https://community.jboss.org/thread/176096
        deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);

        DefaultHttpClient client = new DefaultHttpClient();

        // URLs look like "http://IP:PORT/singleton/service"
        String url1 = baseURL1.toString() + "service";
        String url2 = baseURL2.toString() + "service";

        System.out.println("URLs are: " + url1 + ", " + url2);

        try {
            HttpResponse response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("node-udp-0", response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.start(CONTAINER_2);
            deployer.deploy(DEPLOYMENT_2);


            response = client.execute(new HttpGet(url1));
            long startTime = System.currentTimeMillis();
            while(response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK && startTime + GRACE_TIME_TO_MEMBERSHIP_CHANGE > System.currentTimeMillis()) {
                response = client.execute(new HttpGet(url1));
            }
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            response = tryGet(client, url2);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.stop(CONTAINER_2);

            response = tryGet(client, url1);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("node-udp-0", response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.start(CONTAINER_2);

            response = tryGet(client, url1);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            response = tryGet(client, url2);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.stop(CONTAINER_1);

            response = tryGet(client, url2);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("node-udp-1", response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.start(CONTAINER_1);

            response = tryGet(client, url1);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            response = tryGet(client, url2);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();

            deployer.undeploy(DEPLOYMENT_1);
            controller.stop(CONTAINER_1);
            deployer.undeploy(DEPLOYMENT_2);
            controller.stop(CONTAINER_2);
        }
    }

    private HttpResponse tryGet(final DefaultHttpClient client, final String url1) throws IOException {
        final long startTime;
        HttpResponse response = client.execute(new HttpGet(url1));
        startTime = System.currentTimeMillis();
        while(response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK && startTime + GRACE_TIME_TO_MEMBERSHIP_CHANGE > System.currentTimeMillis()) {
            response = client.execute(new HttpGet(url1));
        }
        return response;
    }
}
