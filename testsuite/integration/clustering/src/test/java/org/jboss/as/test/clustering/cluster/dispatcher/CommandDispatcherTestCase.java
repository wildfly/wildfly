/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.dispatcher;

import static org.junit.Assert.*;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetriever;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CommandDispatcherTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = CommandDispatcherTestCase.class.getSimpleName();
    private static final long VIEW_CHANGE_WAIT = TimeoutUtil.adjust(2000);

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(ClusterTopologyRetriever.class.getPackage());
        return ejbJar;
    }

    @Test
    public void test() throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ClusterTopologyRetriever bean = directory.lookupStateless(ClusterTopologyRetrieverBean.class, ClusterTopologyRetriever.class);

            ClusterTopology topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().toString(), topology.getNodes().contains(NODE_1));
            assertTrue(topology.getNodes().toString(), topology.getNodes().contains(NODE_2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));

            undeploy(DEPLOYMENT_2);

            topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(NODE_1));
            assertEquals(NODE_1, topology.getLocalNode());
            assertTrue(topology.getRemoteNodes().toString(), topology.getRemoteNodes().isEmpty());

            deploy(DEPLOYMENT_2);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(NODE_1));
            assertTrue(topology.getNodes().contains(NODE_2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));

            stop(NODE_1);

            topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(NODE_2));
            assertEquals(NODE_2, topology.getLocalNode());
            assertTrue(topology.getRemoteNodes().toString(), topology.getRemoteNodes().isEmpty());

            start(NODE_1);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(topology.getNodes().toString(), 2, topology.getNodes().size());
            assertTrue(topology.getNodes().toString() + " should contain " + NODE_1, topology.getNodes().contains(NODE_1));
            assertTrue(topology.getNodes().toString() + " should contain " + NODE_2, topology.getNodes().contains(NODE_2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));
        }
    }
}
