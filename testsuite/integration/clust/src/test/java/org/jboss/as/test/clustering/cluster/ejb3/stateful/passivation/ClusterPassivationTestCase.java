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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.passivation;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.NodeInfoServlet;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.ClusterContext;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

/**
 * Clustering ejb passivation simple test.
 * Part of migration of tests from prior AS testsuites [JBQA-5855].
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusterPassivationTestCase extends ClusterAbstractTestCase {
    private static Logger log = Logger.getLogger(ClusterPassivationTestCase.class);
    public static final String ARCHIVE_NAME = "cluster-passivation-test";
    public static final String ARCHIVE_NAME_HELPER = "cluster-passivation-test-helper";

    private static final String IDLE_TIMEOUT_ATTR = "idle-timeout";
    private static final String PASSIVATE_EVENTS_ON_REPLICATE_ATTR = "passivate-events-on-replicate";
    private static final String HELPER_DEPLOYMENT = "helper-";

    private static EJBDirectory context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        context = new RemoteEJBDirectory(ARCHIVE_NAME);
    }

    // Properties pass amongst tests
    private static ContextSelector<EJBClientContext> previousSelector;
    private static Map<String, String> node2deployment = new HashMap<String, String>();
    private static Map<String, String> node2container = new HashMap<String, String>();
    private static Map<String, String> container2node = new HashMap<String, String>();
    private static Map<String, ManagementClient> node2client = new HashMap<String, ManagementClient>();


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

    @Deployment(name = DEPLOYMENT_1 + HELPER_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> helpDeployment0() {
        Archive<?> archive = createHelperDeployment();
        return archive;
    }

    @Deployment(name = DEPLOYMENT_2 + HELPER_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> helpDeployment1() {
        Archive<?> archive = createHelperDeployment();
        return archive;
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(ClusterPassivationTestCase.class.getPackage());
        war.addClasses(NodeNameGetter.class, NodeInfoServlet.class);
        log.info(war.toString(true));
        return war;
    }

    private static Archive<?> createHelperDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_HELPER + ".jar");
        jar.addClasses(StatefulBeanDeepNested.class, StatefulBeanDeepNestedRemote.class, StatefulBeanNestedParent.class, NodeNameGetter.class);
        log.info(jar.toString(true));
        return jar;
    }

    /**
     * Returning modelnode address for DRM to be able to set cache attributes (client drm call).
     */
    private static ModelNode getAddress() {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "ejb3");
        address.add("cluster-passivation-store", "infinispan");
        address.protect();
        return address;
    }

    /**
     * Setting passivation timeout cache attribute (client drm call).
     */
    private static void setPassivationIdleTimeout(ModelControllerClient client) throws Exception {
        ModelNode address = getAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get("name").set(IDLE_TIMEOUT_ATTR);
        operation.get("value").set(1L);
        // ModelNode result = client.execute(operation);
        ModelNode result = ManagementOperations.executeOperationRaw(client, operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        log.info("modelnode operation " + WRITE_ATTRIBUTE_OPERATION + " " + IDLE_TIMEOUT_ATTR + " =1: " + result);
    }

    /**
     * Setting on cache replicate attribute (client drm call).
     */
    private static void setPassivationOnReplicate(ModelControllerClient client, boolean value) throws Exception {
        ModelNode address = getAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(PASSIVATE_EVENTS_ON_REPLICATE_ATTR);
        operation.get(VALUE).set(value);
        // ModelNode result = client.execute(operation);
        ModelNode result = ManagementOperations.executeOperationRaw(client, operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        log.info("modelnode operation " + WRITE_ATTRIBUTE_OPERATION + " " + PASSIVATE_EVENTS_ON_REPLICATE_ATTR + " ="
                + (value ? "TRUE" : "FALSE") + ": " + result);
    }

    /**
     * Unsetting specific attribute (client drm call).
     */
    private static void unsetPassivationAttributes(ModelControllerClient client, String attrName) throws Exception {
        ModelNode address = getAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(attrName);
        ModelNode result = client.execute(operation);
        Assert.assertEquals("ModelNode operation execute was not successful", SUCCESS, result.get(OUTCOME).asString());
        log.info("unset modelnode operation " + UNDEFINE_ATTRIBUTE_OPERATION + " on " + attrName + ": " + result);
    }

    /**
     * Unsetting all cache attributes - defining their default values.
     */
    private void unsetPassivationAttributes(ModelControllerClient client) throws Exception {
        unsetPassivationAttributes(client, IDLE_TIMEOUT_ATTR);
        unsetPassivationAttributes(client, PASSIVATE_EVENTS_ON_REPLICATE_ATTR);
    }

    /**
     * Sets up the EJB client context to use a selector which processes and sets up EJB receivers based on this testcase
     * specific jboss-ejb-client.properties file
     */
    private void setupEJBClientContextSelector() throws IOException {
        previousSelector = EJBClientContextSelector
                .setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties");
    }

    /**
     * Start servers whether their are not started.
     *
     * @param client1 client for server1
     * @param client2 client for server2
     */
    private void startServers(ManagementClient client1, ManagementClient client2) {
        if (client1 == null || !client1.isServerInRunningState()) {
            log.info("Starting server: " + CONTAINER_1);
            start(CONTAINER_1);
            deploy(DEPLOYMENT_1 + HELPER_DEPLOYMENT);
            deploy(DEPLOYMENT_1);
        }
        if (client2 == null || !client2.isServerInRunningState()) {
            log.info("Starting server: " + CONTAINER_2);
            start(CONTAINER_2);
            deploy(DEPLOYMENT_2 + HELPER_DEPLOYMENT);
            deploy(DEPLOYMENT_2);
        }
    }

    /**
     * Waiting for getting cluster context - it could take some time for client to get info from cluster nodes
     */
    private void waitForClusterContext() throws InterruptedException {
        int counter = 0;
        EJBClientContext ejbClientContext = EJBClientContext.requireCurrent();
        ClusterContext clusterContext = null;
        do {
            clusterContext = ejbClientContext.getClusterContext(CLUSTER_NAME);
            counter--;
            Thread.sleep(CLUSTER_ESTABLISHMENT_WAIT_MS);
        } while (clusterContext == null && counter < CLUSTER_ESTABLISHMENT_LOOP_COUNT);
        Assert.assertNotNull("Cluster context for " + CLUSTER_NAME + " was not taken in "
                + (CLUSTER_ESTABLISHMENT_LOOP_COUNT * CLUSTER_ESTABLISHMENT_WAIT_MS) + " ms", clusterContext);
    }

    // TEST METHODS -------------------------------------------------------------
    @Test
    @InSequence(-2)
    public void arquillianStartServers() {
        startServers(null, null);
    }

    /**
     * Association of node names to deployment,container names and client context
     */
    @Test
    @InSequence(-1)
    public void defineMaps(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {

        String nodeName1 = HttpRequest.get(baseURL1.toString() + NodeInfoServlet.URL , HTTP_REQUEST_WAIT_TIME_S, TimeUnit.SECONDS);
        node2deployment.put(nodeName1, DEPLOYMENT_1);
        node2container.put(nodeName1, CONTAINER_1);
        container2node.put(CONTAINER_1, nodeName1);
        node2client.put(nodeName1, client1);
        log.info("URL1 nodename: " + nodeName1);

        String nodeName2 = HttpRequest.get(baseURL2.toString() + NodeInfoServlet.URL, HTTP_REQUEST_WAIT_TIME_S, TimeUnit.SECONDS);

        node2deployment.put(nodeName2, DEPLOYMENT_2);
        node2container.put(nodeName2, CONTAINER_2);
        container2node.put(CONTAINER_2, nodeName2);
        node2client.put(nodeName2, client2);
        log.info("URL2 nodename: " + nodeName2);
    }

    /**
     * Testing passivation over nodes - switching a node on and off. Testing prepassivate annotated bean function.
     */
    private void runPassivation(boolean isPassivation) throws Exception {
        // Loading context from file to get ejb:// remote context
        setupEJBClientContextSelector(); // setting context from .properties file
        final StatefulBeanRemote statefulBeanRemote = context.lookupStateful(StatefulBean.class, StatefulBeanRemote.class);
        log.info("Passivated (" + (isPassivation ? "TRUE" : "FALSE") + ") by on start: " + statefulBeanRemote.getPassivatedBy());

        // Calling on server one
        int clientNumber = 40;
        String calledNodeFirst = statefulBeanRemote.setNumber(clientNumber);
        statefulBeanRemote.setPassivationNode(calledNodeFirst);
        statefulBeanRemote.incrementNumber(); // 41
        Assert.assertEquals(++clientNumber, statefulBeanRemote.getNumber()); // 41
        // nodeName of nested bean should be the same as the node of parent
        Assert.assertEquals("Nested bean has to be called on the same node as parent one", calledNodeFirst, statefulBeanRemote.getRemoteNestedBeanNodeName());
        log.info("Called node name first: " + calledNodeFirst);
        Thread.sleep(WAIT_FOR_PASSIVATION_MS); // waiting for passivation

        // A small hack - deleting node (by name) from cluster which this client knows
        // It means that the next request (ejb call) will be passed to the server #2
        EJBClientContext.requireCurrent().getClusterContext(CLUSTER_NAME).removeClusterNode(calledNodeFirst);

        if (isPassivation) {
            // this was redefined in @PrePassivate method on first server - checking whether second server knows about it
            Assert.assertEquals("Supposing to get passivation node which was set", calledNodeFirst, statefulBeanRemote.getPassivatedBy());
            // Nested beans have to be passivated as well
            Assert.assertTrue("Passivation of nested bean was not propagated", statefulBeanRemote.getNestedBeanPassivatedCalled() > 0);
            Assert.assertTrue("Activation of nested bean was not propagated", statefulBeanRemote.getNestedBeanActivatedCalled() > 0);
            Assert.assertTrue("Passivation of deep nested bean was not propagated", statefulBeanRemote.getDeepNestedBeanPassivatedCalled() > 0);
            Assert.assertTrue("Activation of deep nested bean was not propagated",statefulBeanRemote.getDeepNestedBeanActivatedCalled() > 0);
            Assert.assertTrue("Passivation of remote bean was not propagated", statefulBeanRemote.getRemoteNestedBeanPassivatedCalled() > 0);
            Assert.assertTrue("Activation of remote bean was not propagated", statefulBeanRemote.getRemoteNestedBeanActivatedCalled() > 0);
            statefulBeanRemote.resetNestedBean();
        } else {
            Assert.assertNull("We suppose that the passivation is not provided.", statefulBeanRemote.getPassivatedBy());
            Assert.assertEquals("No passivation should be done", 0, statefulBeanRemote.getNestedBeanPassivatedCalled());
            Assert.assertEquals("No passivation should be done", 0, statefulBeanRemote.getNestedBeanActivatedCalled());
            Assert.assertEquals("No passivation should be done", 0, statefulBeanRemote.getDeepNestedBeanPassivatedCalled());
            Assert.assertEquals("No passivation should be done", 0, statefulBeanRemote.getDeepNestedBeanActivatedCalled());
            Assert.assertEquals("No passivation should be done", 0, statefulBeanRemote.getRemoteNestedBeanPassivatedCalled());
            Assert.assertEquals("No passivation should be done", 0, statefulBeanRemote.getRemoteNestedBeanActivatedCalled());
        }

        String calledNodeSecond = statefulBeanRemote.incrementNumber(); // 42
        Assert.assertEquals("Nested bean has to be called on the same node as parent one", calledNodeSecond, statefulBeanRemote.getNestedBeanNodeName());
        statefulBeanRemote.setPassivationNode(calledNodeSecond);
        log.info("Called node name second: " + calledNodeSecond);
        Thread.sleep(WAIT_FOR_PASSIVATION_MS); // waiting for passivation

        // Resetting cluster context to know both cluster nodes
        setupEJBClientContextSelector();
        // Waiting for getting cluster context - it could take some time for client to get info from cluster nodes
        waitForClusterContext();

        // Stopping node #2
        deployer.undeploy(node2deployment.get(calledNodeSecond));
        controller.stop(node2container.get(calledNodeSecond));

        // We killed second node and we check the value on first node
        Assert.assertEquals(++clientNumber, statefulBeanRemote.getNumber()); // 42
        // Calling on first server
        String calledNode = statefulBeanRemote.incrementNumber(); // 43
        // Checking called node and set number
        Assert.assertEquals("It can't be node " + calledNodeSecond + " because is switched off", calledNodeFirst, calledNode);
        if (isPassivation) {
            Assert.assertEquals("Supposing to get passivation node which was set", calledNodeSecond, statefulBeanRemote.getPassivatedBy());
            Assert.assertTrue("Passivation of nested bean was not propagated", statefulBeanRemote.getNestedBeanPassivatedCalled() > 0);
            Assert.assertTrue("Activation of nested bean was not propagated", statefulBeanRemote.getNestedBeanActivatedCalled() > 0);
            Assert.assertTrue("Passivation of deep nested bean was not propagated", statefulBeanRemote.getDeepNestedBeanPassivatedCalled() > 0);
            Assert.assertTrue("Activation of deep nested bean was not propagated",statefulBeanRemote.getDeepNestedBeanActivatedCalled() > 0);
            Assert.assertTrue("Passivation of remote bean was not propagated", statefulBeanRemote.getRemoteNestedBeanPassivatedCalled() > 0);
            Assert.assertTrue("Activation of remote bean was not propagated", statefulBeanRemote.getRemoteNestedBeanActivatedCalled() > 0);
            statefulBeanRemote.resetNestedBean();
        } else {
            Assert.assertNull("We suppose that the passivation is not provided.", statefulBeanRemote.getPassivatedBy());
            Assert.assertEquals("No passivation should be done",0, statefulBeanRemote.getNestedBeanPassivatedCalled());
            Assert.assertEquals("No passivation should be done",0, statefulBeanRemote.getNestedBeanActivatedCalled());
            Assert.assertEquals("No passivation should be done",0, statefulBeanRemote.getDeepNestedBeanPassivatedCalled());
            Assert.assertEquals("No passivation should be done",0, statefulBeanRemote.getDeepNestedBeanActivatedCalled());
            Assert.assertEquals("No passivation should be done",0, statefulBeanRemote.getRemoteNestedBeanPassivatedCalled());
            Assert.assertEquals("No passivation should be done",0, statefulBeanRemote.getRemoteNestedBeanActivatedCalled());
        }
        Thread.sleep(WAIT_FOR_PASSIVATION_MS); // waiting for passivation
        Assert.assertEquals(++clientNumber, statefulBeanRemote.getNumber()); // 43
    }

    @Test
    @InSequence(1)
    @Ignore("https://issues.jboss.org/browse/AS7-4266")
    public void testPassivationOverNodesPassivated(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {
        boolean isPassivated = true;
        setPassivationIdleTimeout(client1.getControllerClient());
        setPassivationOnReplicate(client1.getControllerClient(), isPassivated);
        setPassivationIdleTimeout(client2.getControllerClient());
        setPassivationOnReplicate(client2.getControllerClient(), isPassivated);
        runPassivation(isPassivated);
        startServers(client1, client2);
    }

    @Test
    @InSequence(2)
    @Ignore("https://issues.jboss.org/browse/AS7-4246")
    public void testPassivationOverNodesNotPassivated(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {
        boolean isPassivated = false;
        setPassivationIdleTimeout(client1.getControllerClient());
        setPassivationOnReplicate(client1.getControllerClient(), isPassivated);
        setPassivationIdleTimeout(client2.getControllerClient());
        setPassivationOnReplicate(client2.getControllerClient(), isPassivated);
        runPassivation(isPassivated);
        startServers(client1, client2);
    }


    @Test
    @InSequence(3)
    /**
     * Testing behaviour of passivation of bean inherited from another one with more complex data structure.
     */
    public void testMoreDataPassivation(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2)
            throws Exception {
        // Set passivation timeout once again
        boolean isPassivated = true;
        setPassivationIdleTimeout(client1.getControllerClient());
        setPassivationOnReplicate(client1.getControllerClient(), isPassivated);
        setPassivationIdleTimeout(client2.getControllerClient());
        setPassivationOnReplicate(client2.getControllerClient(), isPassivated);

        log.info("Setting context selector...");
        setupEJBClientContextSelector();

        final StatefulBeanChildRemote sb = context.lookupStateful(StatefulBeanChild.class, StatefulBeanChildRemote.class);

        String stringData = "42";
        int intData = 42;
        String calledNodeName = sb.getNodeName();

        // redefine node2client maps
        node2client.put(container2node.get(CONTAINER_1), client1);
        node2client.put(container2node.get(CONTAINER_2), client2);

        // 1. integer defined in sfsb bean
        sb.setInt(intData);
        // 2. DTO defined in sfsb
        sb.setDTOStringData(stringData);
        sb.setDTONumberData(intData);
        // 3. DTO defined in sfsb as transient
        sb.setTransientDTOStringData(stringData);
        sb.setTransientDTONumberData(intData);
        // 4. DTO defined in parent of sfsb
        sb.setParentDTOStringData(stringData);
        sb.setParentDTOTransientStringData(stringData);

        // waiting for passivation
        Thread.sleep(WAIT_FOR_PASSIVATION_MS);

        // Stopping called node
        unsetPassivationAttributes(node2client.get(calledNodeName).getControllerClient());
        undeploy(node2deployment.get(calledNodeName));
        stop(node2container.get(calledNodeName));
        log.info("Node " + calledNodeName + " was stopped.");

        // checking data
        Assert.assertEquals("Int sfsb data wasn't passivated correctly", intData, sb.getInt());
        Assert.assertEquals("String data of dto defined in sfsb wasn't passivated correctly", stringData, sb.getDTOStringData());
        Assert.assertEquals("Int data of dto defined in sfsb wasn't passivated correctly", intData, sb.getDTONumberData());
        Assert.assertNull("String data of transient dto defined in sfsb has to be null after passivation", sb.getTransientDTOStringData());
        Assert.assertEquals("Int data of transient dto has to be 0 after passivation", 0,sb.getTransientDTONumberData());
        Assert.assertEquals("String data of dto defined in parent of sfsb wasn't passivated correctly", stringData, sb.getParentDTOStringData());
        Assert.assertNull("Transient string data of dto defined in parent of sfsb has to be null after passivation", sb.getParentDTOTransientStringData());
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
        if(client1.isServerInRunningState()) {
            unsetPassivationAttributes(client1.getControllerClient());
        }
        if(client2.isServerInRunningState()) {
            unsetPassivationAttributes(client2.getControllerClient());
        }
    }
}
