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
package org.jboss.as.test.clustering.cluster.singleton.deployment;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_2;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.managed.ManagedDeployer;
import org.jboss.as.test.clustering.ServiceListenerServlet;
import org.jboss.as.test.clustering.ViewChangeListenerServlet;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.jboss.msc.service.ServiceController;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class SingletonDeploymentTestCase {

    private static final String DEPLOYMENT_NAME = "singleton-deployment.war";
    private static final String MEMBERSHIP_DEPLOYMENT_1 = "membership-deployment-0";
    private static final String MEMBERSHIP_DEPLOYMENT_2 = "membership-deployment-1";
    private static final int DEPLOY_TIMEOUT = 1000;

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private ManagedDeployer deployer;

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
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClass(SimpleServlet.class)
        ;
    }

    @Deployment(name = MEMBERSHIP_DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> membershipDeployment0() {
        return createMembershipDeployment();
    }

    @Deployment(name = MEMBERSHIP_DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> membershipDeployment1() {
        return createMembershipDeployment();
    }
    
    private static Archive<?> createMembershipDeployment() {
        return ShrinkWrap.create(WebArchive.class, "membership.war")
                .addClass(ViewChangeListenerServlet.class)
                .addClass(ServiceListenerServlet.class)
                .setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan, org.jboss.msc, org.jboss.as.clustering.common\n"))
        ;
    }

    @Test
    @InSequence(1)
    public void testArquillianWorkaround() {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER_1);
        deployer.deploy(MEMBERSHIP_DEPLOYMENT_1, "standard");
        deployer.deploy(DEPLOYMENT_1, "standard");

        controller.start(CONTAINER_2);
        deployer.deploy(MEMBERSHIP_DEPLOYMENT_2, "standard");
        deployer.deploy(DEPLOYMENT_2, "standard");
    }

    @Test
    @InSequence(2)
    public void testSingletonService(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL deploymentURL1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL deploymentURL2,
            @ArquillianResource @OperateOnDeployment(MEMBERSHIP_DEPLOYMENT_1) URL membershipURL1,
            @ArquillianResource @OperateOnDeployment(MEMBERSHIP_DEPLOYMENT_2) URL membershipURL2)
            throws IOException, InterruptedException, URISyntaxException {
        HttpClient client = HttpClientUtils.relaxedCookieHttpClient();

        URI uri1 = SimpleServlet.createURI(deploymentURL1);
        URI uri2 = SimpleServlet.createURI(deploymentURL2);
        
        try {
            // Redeploy each deployment using the singleton policy
            deployer.undeploy(DEPLOYMENT_1);
            deployer.deploy(DEPLOYMENT_1, "singleton");
            
            this.wait(client, membershipURL1, NODE_1);
            
            deployer.undeploy(DEPLOYMENT_2);
            deployer.deploy(DEPLOYMENT_2, "singleton");
            
            this.wait(client, membershipURL1, NODE_1, NODE_2);
            this.wait(client, membershipURL1);
            
            // Deployment should be started on the coordinator
            this.assertOK(client, uri1);
            this.assertNotFound(client, uri2);
            
            deployer.undeploy(DEPLOYMENT_1);
            this.wait(client, membershipURL2, NODE_2);
            this.wait(client, membershipURL2);
            
            // Deployment should failover to the new coordinator
            this.assertNotFound(client, uri1);
            this.assertOK(client, uri2);

            deployer.deploy(DEPLOYMENT_1, "singleton");
            this.wait(client, membershipURL2, NODE_1, NODE_2);
            
            // Deployment should still use the new coordinator
            this.assertNotFound(client, uri1);
            this.assertOK(client, uri2);
            
            deployer.undeploy(DEPLOYMENT_2);
            this.wait(client, membershipURL1, NODE_1);
            this.wait(client, membershipURL1);
            
            // Deployment should fail-back to the new coordinator
            this.assertOK(client, uri1);
            this.assertNotFound(client, uri2);

            deployer.deploy(DEPLOYMENT_2, "singleton");
            this.wait(client, membershipURL1, NODE_1, NODE_2);
            
            // Deployment should still use the new coordinator
            this.assertOK(client, uri1);
            this.assertNotFound(client, uri2);
            
            deployer.undeploy(DEPLOYMENT_1);
            this.wait(client, membershipURL2, NODE_2);
            this.wait(client, membershipURL2);
            
            this.assertNotFound(client, uri1);
            this.assertOK(client, uri2);

            // Verify that we can redeploy the same deployment using the standard policy
            deployer.deploy(DEPLOYMENT_1, "standard");
            this.wait(client, membershipURL2, NODE_2);
            
            this.assertOK(client, uri1);
            this.assertOK(client, uri2);
            
            deployer.undeploy(DEPLOYMENT_2);
            deployer.deploy(DEPLOYMENT_2, "standard");
            
            this.assertOK(client, uri1);
            this.assertOK(client, uri2);
        } finally {
            client.getConnectionManager().shutdown();

            deployer.undeploy(DEPLOYMENT_1);
            deployer.undeploy(MEMBERSHIP_DEPLOYMENT_1);
            controller.stop(CONTAINER_1);
            deployer.undeploy(MEMBERSHIP_DEPLOYMENT_2);
            deployer.undeploy(DEPLOYMENT_2);
            controller.stop(CONTAINER_2);
        }
    }

    // Validate the cluster membership
    private void wait(HttpClient client, URL baseURL, String... nodes) throws IOException, URISyntaxException {
        Assert.assertEquals(HttpServletResponse.SC_OK, this.invoke(client, ViewChangeListenerServlet.createURI(baseURL, "cluster", nodes)));
    }

    // Ensure that the singleton service is started
    private void wait(HttpClient client, URL baseURL) throws IOException, URISyntaxException {
        Assert.assertEquals(HttpServletResponse.SC_OK, this.invoke(client, ServiceListenerServlet.createURI(baseURL, org.jboss.as.server.deployment.Services.deploymentUnitName(DEPLOYMENT_NAME).append("service"), ServiceController.State.UP)));
    }

    // Just because the expected cluster membership was established, and the deployment service is up
    // doesn't mean that the web context has started - so we retry up to 10 times for a successful response
    private void assertOK(HttpClient client, URI uri) throws IOException {
        long now = System.currentTimeMillis();
        long start = now;
        long timeout = start + DEPLOY_TIMEOUT;
        int status = this.invoke(client, uri);
        while (status == HttpServletResponse.SC_NOT_FOUND) {
            Thread.yield();
            now = System.currentTimeMillis();
            if (now >= timeout) {
                Assert.fail(String.format("%s returned %d after %d ms of retries.", uri, status, DEPLOY_TIMEOUT));
            }
            status = this.invoke(client, uri);
        }
        Assert.assertEquals(HttpServletResponse.SC_OK, status);
    }

    private void assertNotFound(HttpClient client, URI uri) throws IOException {
        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, this.invoke(client, uri));
    }
    
    private int invoke(HttpClient client, URI uri) throws IOException {
        System.out.println("Invoking " + uri);
        HttpResponse response = client.execute(new HttpGet(uri));
        int status = response.getStatusLine().getStatusCode();
        response.getEntity().getContent().close();
        return status;
    }
}
