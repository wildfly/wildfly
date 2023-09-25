/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.PropertyPermission;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Validates failover behavior of a remotely accessed @Stateful EJB.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public abstract class AbstractRemoteStatefulEJBFailoverTestCase extends AbstractClusteringTestCase {
    private static final int COUNT = 20;
    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);

    static Archive<?> createDeployment(String moduleName) {
        return ShrinkWrap.create(JavaArchive.class, moduleName + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatefulIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    private final ExceptionSupplier<EJBDirectory, Exception> directoryProvider;

    protected AbstractRemoteStatefulEJBFailoverTestCase(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) {
        this.directoryProvider = directoryProvider;
    }

    @Test
    public void test() throws Exception {
        try (EJBDirectory directory = this.directoryProvider.get()) {
            Incrementor bean = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);

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

            stop(target);

            result = bean.increment();
            // Bean should failover to other node
            failoverTarget = result.getNode();

            Assert.assertEquals(count++, result.getValue().intValue());
            Assert.assertNotEquals(target, failoverTarget);

            start(target);

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

            bean.remove();
        }
    }
}
