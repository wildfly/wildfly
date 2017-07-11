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

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.legacy.JBossEJBProperties;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates failover behavior of a remotely accessed @Stateful EJB.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteStatefulEJBFailoverTestCase extends ClusterAbstractTestCase {
    private static final String MODULE_NAME = "remote-stateful-ejb-failover-test";
    private static final String CLIENT_PROPERTIES = "org/jboss/as/test/clustering/cluster/ejb/remote/jboss-ejb-client.properties";

    private static final int COUNT = 20;
    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatefulIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    @Test
    public void test() throws Exception {
        JBossEJBProperties.fromClassPath(this.getClass().getClassLoader(), CLIENT_PROPERTIES).runCallable(() -> {
            try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
                Incrementor bean = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);
                EJBClient.setStrongAffinity(bean, new ClusterAffinity("ejb"));

                Result<Integer> result = bean.increment();
                String target = result.getNode();
                int count = 1;

                Assert.assertEquals(count++, result.getValue().intValue());

                // Bean should retain weak affinity for this node
                for (int i = 0; i < COUNT; ++i) {
                    result = bean.increment();
                    Assert.assertEquals(count++, result.getValue().intValue());
                    Assert.assertEquals(String.valueOf(i), target, result.getNode());
                }

                undeploy(this.findDeployment(target));

                Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

                result = bean.increment();
                // Bean should failover to other node
                String failoverTarget = result.getNode();

                Assert.assertEquals(count++, result.getValue().intValue());
                Assert.assertNotEquals(target, failoverTarget);

                deploy(this.findDeployment(target));

                // Allow sufficient time for client to receive new topology
                Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

                result = bean.increment();
                String failbackTarget = result.getNode();
                Assert.assertEquals(count++, result.getValue().intValue());
                // Bean should retain weak affinity for this node
                Assert.assertEquals(failoverTarget, failbackTarget);

                result = bean.increment();
                // Bean may have acquired new weak affinity
                target = result.getNode();
                Assert.assertEquals(count++, result.getValue().intValue());

                // Bean should retain weak affinity for this node
                for (int i = 0; i < COUNT; ++i) {
                    result = bean.increment();
                    Assert.assertEquals(count++, result.getValue().intValue());
                    Assert.assertEquals(String.valueOf(i), target, result.getNode());
                }

                stop(this.findContainer(target));

                result = bean.increment();
                // Bean should failover to other node
                failoverTarget = result.getNode();

                Assert.assertEquals(count++, result.getValue().intValue());
                Assert.assertNotEquals(target, failoverTarget);

                start(this.findContainer(target));

                // Allow sufficient time for client to receive new topology
                Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

                result = bean.increment();
                failbackTarget = result.getNode();
                Assert.assertEquals(count++, result.getValue().intValue());
                // Bean should retain weak affinity for this node
                Assert.assertEquals(failoverTarget, failbackTarget);

                result = bean.increment();
                // Bean may have acquired new weak affinity
                target = result.getNode();
                Assert.assertEquals(count++, result.getValue().intValue());

                // Bean should retain weak affinity for this node
                for (int i = 0; i < COUNT; ++i) {
                    result = bean.increment();
                    Assert.assertEquals(count++, result.getValue().intValue());
                    Assert.assertEquals(String.valueOf(i), target, result.getNode());
                }
            }
            return null;
        });
    }
}
