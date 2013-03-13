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

package org.jboss.as.test.clustering.extended.ejb2.stateful.passivation;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.NodeInfoServlet;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

/**
 * Clustering ejb passivation of EJB2 beans defined by annotation.
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusterPassivationTestCase extends ClusterPassivationTestBase {
    private static Logger log = Logger.getLogger(ClusterPassivationTestCase.class);

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        Archive<?> archive = createDeployment();
        return archive;
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        Archive<?> archive = createDeployment();
        return archive;
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(StatefulBeanBase.class, StatefulBean.class, StatefulRemote.class, StatefulRemoteHome.class);
        war.addClasses(NodeNameGetter.class, NodeInfoServlet.class);
        log.info(war.toString(true));
        return war;
    }

    @Override
    protected void startServers(ManagementClient client1, ManagementClient client2) {
        if (client1 == null || !client1.isServerInRunningState()) {
            log.info("Starting server: " + CONTAINER_1);
            controller.start(CONTAINER_1);
            deployer.deploy(DEPLOYMENT_1);
        }
        if (client2 == null || !client2.isServerInRunningState()) {
            log.info("Starting server: " + CONTAINER_2);
            controller.start(CONTAINER_2);
            deployer.deploy(DEPLOYMENT_2);
        }
    }


    @Test
    @InSequence(-2)
    public void arquillianStartServers() {
        // Container is unmanaged, need to start manually - see https://community.jboss.org/thread/176096
        startServers(null, null);
    }

    /**
     * Associtation of node names to deployment,container names and client context
     */
    @Test
    @InSequence(-1)
    public void defineMaps(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
                           @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
                           @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
                           @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {

        String nodeName1 = HttpRequest.get(baseURL1.toString() + NodeInfoServlet.URL, HTTP_REQUEST_WAIT_TIME_S, TimeUnit.SECONDS);
        node2deployment.put(nodeName1, DEPLOYMENT_1);
        node2container.put(nodeName1, CONTAINER_1);
        log.info("URL1 nodename: " + nodeName1);

        String nodeName2 = HttpRequest.get(baseURL2.toString() + NodeInfoServlet.URL, HTTP_REQUEST_WAIT_TIME_S, TimeUnit.SECONDS);

        node2deployment.put(nodeName2, DEPLOYMENT_2);
        node2container.put(nodeName2, CONTAINER_2);
        log.info("URL2 nodename: " + nodeName2);
    }

    @Ignore("JBPAPP-8774")
    @Test
    @InSequence(1)
    public void testPassivationBeanAnnotated(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {
        setPassivationAttributes(client1.getControllerClient());
        setPassivationAttributes(client2.getControllerClient());

        // Setting context from .properties file to get ejb:/ remote context
        setupEJBClientContextSelector();

        StatefulRemoteHome home = directory.lookupHome(StatefulBean.class, StatefulRemoteHome.class);
        StatefulRemote statefulBean = home.create();

        runPassivation(statefulBean, controller, deployer);
        startServers(client1, client2);
    }

    @Test
    @InSequence(100)
    public void stopAndClean(
            @OperateOnDeployment(DEPLOYMENT_1) @ArquillianResource ManagementClient client1,
            @OperateOnDeployment(DEPLOYMENT_2) @ArquillianResource ManagementClient client2) throws Exception {
        log.info("Stop&Clean...");

        // returning to the previous context selector, @see {RemoteEJBClientDDBasedSFSBFailoverTestCase}
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }

        // unset & undeploy & stop
        if (client1.isServerInRunningState()) {
            unsetPassivationAttributes(client1.getControllerClient());
            deployer.undeploy(DEPLOYMENT_1);
            controller.stop(CONTAINER_1);
        }
        if (client2.isServerInRunningState()) {
            unsetPassivationAttributes(client2.getControllerClient());
            deployer.undeploy(DEPLOYMENT_2);
            controller.stop(CONTAINER_2);
        }
    }
}
