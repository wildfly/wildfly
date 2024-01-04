/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared.CounterRemote;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that invocations on a clustered stateful session Enterprise Beans 2 bean from a remote Jakarta Enterprise Beans client, failover to
 * other node(s) in cases like a node going down.
 * This test is taken from test of ejb3 beans.
 *
 * @author Jaikiran Pai
 * @author Radoslav Husar
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class RemoteEJBClientStatefulBeanFailoverTestCase extends RemoteEJBClientStatefulFailoverTestBase {

    @Deployment(name = DEPLOYMENT_HELPER_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1Singleton() {
        return createDeploymentSingleton();
    }

    @Deployment(name = DEPLOYMENT_HELPER_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2Singleton() {
        return createDeploymentSingleton();
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(CounterRemote.class.getPackage());
        jar.addClass(CounterBean.class);
        jar.addClass(NodeNameGetter.class);
        jar.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\nDependencies: deployment." + MODULE_NAME_SINGLE + ".jar\n"), "MANIFEST.MF");
        jar.addAsResource(createPermissionsXmlAsset(
                new PropertyPermission("jboss.node.name", "read")),
                "META-INF/jboss-permissions.xml");
        return jar;
    }

    @Override
    @Test
    public void testFailoverFromRemoteClientWhenOneNodeGoesDown() throws Exception {
        failoverFromRemoteClient(false);
    }

    @Override
    @Test
    public void testFailoverFromRemoteClientWhenOneNodeUndeploys() throws Exception {
        failoverFromRemoteClient(true);
    }
}
