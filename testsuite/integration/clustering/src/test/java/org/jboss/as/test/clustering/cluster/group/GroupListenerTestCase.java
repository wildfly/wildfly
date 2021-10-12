/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.group;

import static org.junit.Assert.*;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.group.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.group.bean.ClusterTopologyRetriever;
import org.jboss.as.test.clustering.cluster.group.bean.ClusterTopologyRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for the listener facility of a {@link Group}.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class GroupListenerTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = GroupListenerTestCase.class.getSimpleName();
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
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(ClusterTopologyRetriever.class.getPackage());
        war.setWebXML(GroupListenerTestCase.class.getPackage(), "web.xml");
        war.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new RuntimePermission("getClassLoader")), "permissions.xml");
        return war;
    }

    @Test
    public void test() throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ClusterTopologyRetriever bean = directory.lookupStateless(ClusterTopologyRetrieverBean.class, ClusterTopologyRetriever.class);

            ClusterTopology topology = bean.getClusterTopology();
            assertEquals(topology.getCurrentMembers().toString(), 2, topology.getCurrentMembers().size());
            assertTrue(topology.getCurrentMembers().toString(), topology.getCurrentMembers().contains(NODE_1));
            assertTrue(topology.getCurrentMembers().toString(), topology.getCurrentMembers().contains(NODE_2));

            stop(NODE_2);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(topology.getCurrentMembers().toString(), 1, topology.getCurrentMembers().size());
            assertTrue(topology.getCurrentMembers().toString(), topology.getCurrentMembers().contains(NODE_1));
            assertEquals(topology.getPreviousMembers().toString(), 2, topology.getPreviousMembers().size());
            assertTrue(topology.getPreviousMembers().toString(), topology.getPreviousMembers().contains(NODE_1));
            assertTrue(topology.getPreviousMembers().toString(), topology.getPreviousMembers().contains(NODE_2));

            start(NODE_2);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(topology.getCurrentMembers().toString(), 2, topology.getCurrentMembers().size());
            assertTrue(topology.getCurrentMembers().toString(), topology.getCurrentMembers().contains(NODE_1));
            assertTrue(topology.getCurrentMembers().toString(), topology.getCurrentMembers().contains(NODE_2));
            if (topology.getTargetMember().equals(NODE_1)) {
                assertEquals(topology.getPreviousMembers().toString(), 1, topology.getPreviousMembers().size());
                assertTrue(topology.getPreviousMembers().toString(), topology.getPreviousMembers().contains(NODE_1));
            } else {
                // Since node 2 was just started, its previous membership will be empty
                assertEquals(topology.getPreviousMembers().toString(), 0, topology.getPreviousMembers().size());
            }

            stop(NODE_1);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(topology.getCurrentMembers().toString(), 1, topology.getCurrentMembers().size());
            assertTrue(topology.getCurrentMembers().toString(), topology.getCurrentMembers().contains(NODE_2));
            assertEquals(topology.getPreviousMembers().toString(), 2, topology.getPreviousMembers().size());
            assertTrue(topology.getPreviousMembers().toString(), topology.getPreviousMembers().contains(NODE_1));
            assertTrue(topology.getPreviousMembers().toString(), topology.getPreviousMembers().contains(NODE_2));

            start(NODE_1);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(topology.getCurrentMembers().toString(), 2, topology.getCurrentMembers().size());
            assertTrue(topology.getCurrentMembers().toString(), topology.getCurrentMembers().contains(NODE_1));
            assertTrue(topology.getCurrentMembers().toString(), topology.getCurrentMembers().contains(NODE_2));
            if (topology.getTargetMember().equals(NODE_2)) {
                assertEquals(topology.getPreviousMembers().toString(), 1, topology.getPreviousMembers().size());
                assertTrue(topology.getPreviousMembers().toString(), topology.getPreviousMembers().contains(NODE_2));
            } else {
                // Since node 1 was just started, its previous membership will be empty
                assertEquals(topology.getPreviousMembers().toString(), 0, topology.getPreviousMembers().size());
            }
        }
    }
}
