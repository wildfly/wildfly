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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusteringTestConstants;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;

/**
 * Tests that if a local EJB receiver handled an invocation on a clustered EJB app and if that app is
 * undeployed later, then subsequent invocations on that bean are redirected to a remote node which is part
 * of that cluster
 *
 * @author Jaikiran Pai
 * @see https://issues.jboss.org/browse/AS7-3492
 */
@RunWith(Arquillian.class)
public class LocalEJBClientFailoverTestCase {

    private static final Logger logger = Logger.getLogger(LocalEJBClientFailoverTestCase.class);

    private static final String CLIENT_APP_MODULE_NAME = "client-app";
    private static final String NODE_NAME_APP_MODULE_NAME = "node-name-app";

    private static final String CLIENT_ARQ_DEPLOYMENT = "client-arq-deployment";
    private static final String NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_1 = "node-name-arq-deployment-container-1";
    private static final String NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_2 = "node-name-arq-deployment-container-2";

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private Context jndiContext;

    private boolean containerOneStarted;
    private boolean containerTwoStarted;
    private boolean clientAppDeployed;
    private boolean nodeNameAppDeployedOnContainerOne;
    private boolean nodeNameAppDeployedOnContainerTwo;

    @Deployment(name = CLIENT_ARQ_DEPLOYMENT, testable = false, managed = false)
    @TargetsContainer(ClusteringTestConstants.CONTAINER_2)
    public static Archive createClientApplication() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, CLIENT_APP_MODULE_NAME + ".jar");
        jar.addClasses(ClientSFSB.class, ClientSFSBRemote.class, NodeNameRetriever.class);
        jar.addAsManifestResource(LocalEJBClientFailoverTestCase.class.getPackage(), "local-ejb-client-failover-jboss-ejb-client.xml", "jboss-ejb-client.xml");

        return jar;
    }

    @Deployment(name = NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_1, testable = false, managed = false)
    @TargetsContainer(ClusteringTestConstants.CONTAINER_1)
    public static Archive createNodeNameApplicationForContainer1() {
        return createNodeNameApplication();
    }

    @Deployment(name = NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_2, testable = false, managed = false)
    @TargetsContainer(ClusteringTestConstants.CONTAINER_2)
    public static Archive createNodeNameApplicationForContainer2() {
        return createNodeNameApplication();
    }

    private static Archive createNodeNameApplication() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, NODE_NAME_APP_MODULE_NAME + ".jar");
        jar.addClasses(NodeNameRetriever.class, NodeNameSFSB.class);
        return jar;
    }

    @Before
    public void beforeTest() throws Exception {
        final Properties props = new Properties();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiContext = new InitialContext(props);
    }

    @After
    public void afterTest() throws Exception {
        if (clientAppDeployed) {
            try {
                this.deployer.undeploy(CLIENT_ARQ_DEPLOYMENT);
            } catch (Exception e) {
                logger.error("Could not undeploy " + CLIENT_ARQ_DEPLOYMENT, e);
            }
        }
        if (nodeNameAppDeployedOnContainerOne) {
            try {
                this.deployer.undeploy(NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_1);
            } catch (Exception e) {
                logger.error("Could not undeploy " + NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_1, e);
            }
        }

        if (nodeNameAppDeployedOnContainerTwo) {
            try {
                this.deployer.undeploy(NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_2);
            } catch (Exception e) {
                logger.error("Could not undeploy " + NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_2, e);
            }
        }

        if (containerOneStarted) {
            try {
                this.container.stop(CONTAINER_1);
            } catch (Exception e) {
                logger.error("Failed to stop " + CONTAINER_1, e);
            }
        }

        if (containerTwoStarted) {
            try {
                this.container.stop(CONTAINER_2);
            } catch (Exception e) {
                logger.error("Failed to stop " + CONTAINER_2, e);
            }
        }

    }

    /**
     * - 2 instances of server, A and B, forming a cluster
     * - Instance A contains foo-app and bar-client apps. foo-app contains a clustered SFSB and bar-client
     * just acts as a client invoking on that clustered SFSB
     * - Instance B contains (only) foo-app with that clustered SFSB
     * - bar-client on Instance A invokes on the clustered SFSB when both server are up and when foo-app is deployed
     * on both servers. It's expected that the call to that clustered SFSB stays local to Instance A, on this occasion
     * - foo-app on Instance A is undeployed
     * - bar-client invokes (again) on the same instance of clustered SFSB. It's expected that this call is now routed to
     * the remote Instance B server
     *
     * @throws Exception
     */
    @Test
    public void testFailoverWhenLocalTargetApplicationIsUndeployed() throws Exception {
        // Container is unmanaged, so start it ourselves
        this.container.start(CONTAINER_1);
        containerOneStarted = true;
        // start the other container too
        this.container.start(CONTAINER_2);
        containerTwoStarted = true;

        this.deployer.deploy(CLIENT_ARQ_DEPLOYMENT);
        clientAppDeployed = true;
        this.deployer.deploy(NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_1);
        nodeNameAppDeployedOnContainerOne = true;
        this.deployer.deploy(NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_2);
        nodeNameAppDeployedOnContainerTwo = true;

        final ContextSelector<EJBClientContext> previousSelector = EJBClientContextSelector.setup("cluster/ejb3/stateful/failover/local-ejb-sfsb-failover-jboss-ejb-client.properties");
        try {
            final ClientSFSBRemote clientSFSB = (ClientSFSBRemote) jndiContext.lookup("ejb:/" + CLIENT_APP_MODULE_NAME + "//" + ClientSFSB.class.getSimpleName() + "!" + ClientSFSBRemote.class.getName() + "?stateful");
            // invoke on non-clustered SFSB which invokes/delegates to clustered SFSB on same node
            final String sfsbNodeName = clientSFSB.invokeAndFetchNodeNameFromClusteredSFSBRemoteBean();
            // the clustered sfsb should be invoked on the same instance as the non-clustered slsb, since the clustered sfsb deployment
            // is available on that node
            Assert.assertEquals("Clustered SFSB created on unexpected node", ClusteringTestConstants.NODE_2, sfsbNodeName);

            // now undeploy the clustered sfsb app on the node which has the client application
            this.deployer.undeploy(NODE_NAME_ARQ_DEPLOYMENT_CONTAINER_2);
            nodeNameAppDeployedOnContainerTwo = false;

            // now invoke again on the same non-clustered sfsb
            final String sfsbNodeNameAfterUndeployment = clientSFSB.invokeAndFetchNodeNameFromClusteredSFSBRemoteBean();
            // the invocation on the clustered sfsb from within the non-clustered sfsb, should failover to the remote
            // node since on the local node, the app has been undeployed
            Assert.assertEquals("Clustered SFSB did not failover to remote node even after the local app was undeployed", ClusteringTestConstants.NODE_1, sfsbNodeNameAfterUndeployment);

        } finally {
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
        }

    }
}
