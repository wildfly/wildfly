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
package org.jboss.as.test.clustering.cluster.management;

import java.net.URL;
import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.JndiServlet;
import org.jboss.as.test.integration.management.base.ArquillianResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.ModelUtil;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CacheTestCase extends AbstractMgmtTestBase {
        
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;    
    
    private ManagementClient managementClient;
    
    private static final String TEST_CONTAINER = "test-container";
    private static final String TEST_CACHE = "test-local-cache";
         
    @Deployment(name = DEPLOYMENT_1, managed = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "CacheTestCase.war");
        war.addClass(CacheTestCase.class);
        war.addClass(JndiServlet.class);
        return war;
    }
    

    @Test
    @InSequence(-2)
    public void testStartServer() throws Exception {
        NodeUtil.start(controller, deployer, CONTAINER_1, DEPLOYMENT_1);                
    }

    @Test
    @InSequence(-1)
    public void testStartCacheContainer(@ArquillianResource ManagementClient managementClient) throws Exception {
        this.managementClient = managementClient;
        ModelNode[] steps = new ModelNode[2];
        steps[0] = createOpNode(
                "subsystem=infinispan/cache-container=" + TEST_CONTAINER, ModelDescriptionConstants.ADD);       
        steps[1] = createOpNode(
                "subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/transport=TRANSPORT",
                ModelDescriptionConstants.ADD);       
        executeOperation(ModelUtil.createCompositeNode(steps));                        
        
    }
    
    @Test
    @InSequence(1)
    public void testStopContainers(@ArquillianResource ManagementClient managementClient) throws Exception {
        this.managementClient = managementClient;
        
        ModelNode op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER, ModelDescriptionConstants.REMOVE);       
        executeOperation(op);                     
        
        NodeUtil.stop(controller, deployer, CONTAINER_1, DEPLOYMENT_1);                
    }    
    @Test    
    public void testLocalCache(@ArquillianResource ManagementClient managementClient, @ArquillianResource URL url) throws Exception {        
        this.managementClient = managementClient;
        // add local cache
        ModelNode  op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/local-cache=" + TEST_CACHE,
                ModelDescriptionConstants.ADD);       
        op.get("start").set("EAGER");
        op.get("jndi-name").set("java:jboss/caches/TestCache");
        executeOperation(op);                     

        // check that it is available through JNDI        
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/caches/TestCache");
        Assert.assertEquals("org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager$DelegatingCache", jndiClass);
        
        // remove local cache
        op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/local-cache=" + TEST_CACHE,
                ModelDescriptionConstants.REMOVE);       
        executeOperation(op);                             

        // check that it is unregistered
        jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/caches/TestCache");
        Assert.assertEquals(JndiServlet.NOT_FOUND, jndiClass);        
    }

    
    @Test
    public void testDistributedCache(@ArquillianResource ManagementClient managementClient,  @ArquillianResource URL url) throws Exception {  
        this.managementClient = managementClient;
        // add local cache
        ModelNode  op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/distributed-cache=" + TEST_CACHE,
                ModelDescriptionConstants.ADD);       
        op.get("start").set("EAGER");
        op.get("mode").set("ASYNC");
        op.get("jndi-name").set("java:jboss/caches/TestCache");
        executeOperation(op);                     
        
        
        // check that it is available through JNDI        
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/caches/TestCache");
        Assert.assertEquals("org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager$DelegatingCache", jndiClass);        
        
        // remove local cache
        op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/distributed-cache=" + TEST_CACHE,
                ModelDescriptionConstants.REMOVE);       
        executeOperation(op);                             
        
        // check that it is unregistered
        jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/caches/TestCache");
        Assert.assertEquals(JndiServlet.NOT_FOUND, jndiClass);        
    }

    @Test
    public void testReplicatedCache(@ArquillianResource ManagementClient managementClient,  @ArquillianResource URL url) throws Exception {  
        this.managementClient = managementClient;
        // add local cache
        ModelNode  op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/replicated-cache=" + TEST_CACHE,
                ModelDescriptionConstants.ADD);       
        op.get("start").set("EAGER");
        op.get("mode").set("ASYNC");
        op.get("jndi-name").set("java:jboss/caches/TestCache");
        executeOperation(op);                     
        
        // check that it is available through JNDI        
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/caches/TestCache");
        Assert.assertEquals("org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager$DelegatingCache", jndiClass);
        
        // remove local cache
        op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/replicated-cache=" + TEST_CACHE,
                ModelDescriptionConstants.REMOVE);       
        executeOperation(op);                             

        // check that it is unregistered
        jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/caches/TestCache");
        Assert.assertEquals(JndiServlet.NOT_FOUND, jndiClass);        
    }

    @Test
    public void testInvalidationCache(@ArquillianResource ManagementClient managementClient,  @ArquillianResource URL url) throws Exception {    
        this.managementClient = managementClient;
        // add local cache
        ModelNode  op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/invalidation-cache=" + TEST_CACHE,
                ModelDescriptionConstants.ADD);       
        op.get("start").set("EAGER");
        op.get("mode").set("ASYNC");
        op.get("jndi-name").set("java:jboss/caches/TestCache");
        executeOperation(op);                     
        
        // check that it is available through JNDI        
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/caches/TestCache");
        Assert.assertEquals("org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager$DelegatingCache", jndiClass);
        
        // remove local cache
        op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER + "/invalidation-cache=" + TEST_CACHE,
                ModelDescriptionConstants.REMOVE);       
        executeOperation(op);                             

        // check that it is unregistered
        jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/caches/TestCache");
        Assert.assertEquals(JndiServlet.NOT_FOUND, jndiClass);        
    }

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
    }
}
