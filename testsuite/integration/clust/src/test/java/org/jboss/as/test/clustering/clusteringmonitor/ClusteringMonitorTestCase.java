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
package org.jboss.as.test.clustering.clusteringmonitor;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.clustering.single.web.SimpleWebTestCase;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.clustering.monitor.extension.ClusterExtension;

/**
 * Tests basic functionality of the clustering-monitor subsystem.
 *
 * We should at a minimum test the following:
 * - deploying a distributable web app results in all expected resources
 * - deploying a @Clustered bean results in all expected resources
 * - specifying a @Cache name results in a cache name change (???)
 * - un-deploying results in resources becoming non-available
 *
 * @author Richard Achmatowicz (c) RedHat Inc 2013
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusteringMonitorTestCase {

    private static final Logger log = Logger.getLogger(ClusteringMonitorTestCase.class);

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    private static Archive<?> getWarDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-deployment.war");
        war.addClass(SimpleServlet.class);
        war.setWebXML(SimpleWebTestCase.class.getPackage(), "web.xml");
        log.info(war.toString(true));
        return war;
    }

    private static Archive<?> getEjbDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-deployment.jar");
        jar.addClass(Stateful.class);
        jar.addClass(StatefulBean.class);
        log.info(jar.toString(true));
        return jar;
    }

    // auto deployed by the base class
    @Deployment(name = "war")
    public static Archive<?> webDeployment() {
        return getWarDeployment();
    }

    @Deployment(name = "jar")
    public static Archive<?> ejbDeployment() {
        return getEjbDeployment();
    }

    /*
     * This method checks that upon deployment of the war web-deployment.war, we have the following resources available:
     *
     *   subsystem=clustering-monitor/cluster=web/deployment=web-deployment.war/web=WEB
     *
     */
    @Test
    @OperateOnDeployment("war")
    public void testWebDeploymentResources(@ArquillianResource ManagementClient managementClient) throws IOException, MgmtOperationException {

        ModelNode result = null;

        // check that the cluster=web resource is available
        ModelNode clusterReadResourceOp = getClusterReadResourceOperation("web");
        result = ManagementOperations.executeOperationRaw(managementClient.getControllerClient(), clusterReadResourceOp);
        // check the result was successful
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // check that the cluster=web/deployment=web-deployment.war resource is available
        ModelNode deploymentReadResource = getDeploymentReadResourceOperation("web", "web-deployment.war");
        result = ManagementOperations.executeOperationRaw(managementClient.getControllerClient(), deploymentReadResource);
        // check the result was successful
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // check that the cluster=web/deployment=web-deployment.war/web=WEB resource is available
        ModelNode webReadResource = getWebReadResourceOperation("web", "web-deployment.war");
        result = ManagementOperations.executeOperationRaw(managementClient.getControllerClient(), webReadResource);
        // check the result was successful
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
    }

     /*
     * This method checks that upon deployment of the jar ejb-deployment.jar, we have the following resources available:
     *
     *   subsystem=clustering-monitor/cluster=ejb/deployment=ejb-deployment.jar/bean=StatefulBean
     *
     */
    @Test
    @OperateOnDeployment("jar")
    public void testEjbDeploymentResources(@ArquillianResource ManagementClient managementClient) throws IOException, MgmtOperationException {

        ModelNode result = null;

        // check that the cluster=web resource is available
        ModelNode clusterReadResourceOp = getClusterReadResourceOperation("ejb");
        result = ManagementOperations.executeOperationRaw(managementClient.getControllerClient(), clusterReadResourceOp);
        // check the result was successful
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // check that the cluster=web/deployment=web-deployment.war resource is available
        ModelNode deploymentReadResource = getDeploymentReadResourceOperation("ejb", "ejb-deployment.jar");
        result = ManagementOperations.executeOperationRaw(managementClient.getControllerClient(), deploymentReadResource);
        // check the result was successful
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // check that the cluster=web/deployment=web-deployment.war/web=WEB resource is available
        ModelNode beanReadResource = getBeanReadResourceOperation("ejb", "ejb-deployment.jar", "StatefulBean");
        result = ManagementOperations.executeOperationRaw(managementClient.getControllerClient(), beanReadResource);
        // check the result was successful
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
    }


    private static ModelNode getClusterReadResourceOperation(String cluster) {
        PathAddress address = getClusterAddress(cluster);

        ModelNode readResourceOp = new ModelNode() ;
        readResourceOp.get(OP).set(READ_RESOURCE_OPERATION);
        readResourceOp.get(OP_ADDR).set(address.toModelNode());
        // additional attributes
        readResourceOp.get(INCLUDE_RUNTIME).set(true);
        return readResourceOp ;
    }

    private static ModelNode getDeploymentReadResourceOperation(String cluster, String deployment) {
        PathAddress address = getDeploymentAddress(cluster, deployment);

        ModelNode readResourceOp = new ModelNode() ;
        readResourceOp.get(OP).set(READ_RESOURCE_OPERATION);
        readResourceOp.get(OP_ADDR).set(address.toModelNode());
        // additional attributes
        readResourceOp.get(INCLUDE_RUNTIME).set(true);
        return readResourceOp ;
    }

    private static ModelNode getWebReadResourceOperation(String cluster, String deployment) {
        PathAddress address = getWebAddress(cluster, deployment);

        ModelNode readResourceOp = new ModelNode() ;
        readResourceOp.get(OP).set(READ_RESOURCE_OPERATION);
        readResourceOp.get(OP_ADDR).set(address.toModelNode());
        // additional attributes
        readResourceOp.get(INCLUDE_RUNTIME).set(true);
        return readResourceOp ;
    }

    private static ModelNode getBeanReadResourceOperation(String cluster, String deployment, String bean) {
        PathAddress address = getBeanAddress(cluster, deployment, bean);

        ModelNode readResourceOp = new ModelNode() ;
        readResourceOp.get(OP).set(READ_RESOURCE_OPERATION);
        readResourceOp.get(OP_ADDR).set(address.toModelNode());
        // additional attributes
        readResourceOp.get(INCLUDE_RUNTIME).set(true);
        return readResourceOp ;
    }


    private static PathAddress getClusterAddress(String cluster) {
        PathAddress address = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, ClusterExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("cluster",cluster));
        return address ;
    }

    private static PathAddress getDeploymentAddress(String cluster, String deployment) {
        return getClusterAddress(cluster).append("deployment", deployment) ;
    }

    private static PathAddress getWebAddress(String cluster, String deployment) {
        return getDeploymentAddress(cluster, deployment).append("web","WEB");
    }

    private static PathAddress getBeanAddress(String cluster, String deployment, String bean) {
        return getDeploymentAddress(cluster, deployment).append("bean", bean) ;
    }

}
