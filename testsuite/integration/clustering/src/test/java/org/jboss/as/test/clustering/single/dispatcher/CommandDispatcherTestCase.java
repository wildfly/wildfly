/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.dispatcher;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.junit.Assert.*;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetriever;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetrieverBean;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.legacy.LegacyClusterTopologyRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that a command dispatcher works in a non-clustered environment.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class CommandDispatcherTestCase {
    private static final String MODULE_NAME = CommandDispatcherTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(ClusterTopologyRetrieverBean.class.getPackage());
        war.addPackage(LegacyClusterTopologyRetrieverBean.class.getPackage());
        war.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new RuntimePermission("getClassLoader")), "permissions.xml");
        war.setWebXML(org.jboss.as.test.clustering.cluster.dispatcher.CommandDispatcherTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void test() throws Exception {
        this.test(ClusterTopologyRetrieverBean.class);
    }

    @Test
    public void legacy() throws Exception {
        this.test(LegacyClusterTopologyRetrieverBean.class);
    }

    public void test(Class<? extends ClusterTopologyRetriever> beanClass) throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ClusterTopologyRetriever bean = directory.lookupStateless(beanClass, ClusterTopologyRetriever.class);
            ClusterTopology topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().toString(), topology.getNodes().contains(NODE_1));
            assertTrue(topology.getRemoteNodes().toString() + " should be empty", topology.getRemoteNodes().isEmpty());
        }
    }
}
