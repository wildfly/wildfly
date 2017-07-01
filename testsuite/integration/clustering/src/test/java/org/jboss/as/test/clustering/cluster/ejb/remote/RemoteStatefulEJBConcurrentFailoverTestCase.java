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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.SlowToDestroyStatefulIncrementorBean;
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
 * Validates mid-invocation failover behavior of a remotely accessed @Stateful EJB.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteStatefulEJBConcurrentFailoverTestCase extends ClusterAbstractTestCase {
    private static final String MODULE_NAME = "remote-stateful-ejb-concurrent-failover-test";
    private static final String CLIENT_PROPERTIES = "org/jboss/as/test/clustering/cluster/ejb/remote/jboss-ejb-client.properties";

    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);
    private static final long INVOCATION_WAIT = TimeoutUtil.adjust(10);

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
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, SlowToDestroyStatefulIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    @Test
    public void test() throws Exception {
        this.test(new GracefulRestartLifecycle());
    }

    public void test(Lifecycle lifecycle) throws Exception {
        JBossEJBProperties.fromClassPath(this.getClass().getClassLoader(), CLIENT_PROPERTIES).runCallable(() -> {
            try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
                Incrementor bean = directory.lookupStateful(SlowToDestroyStatefulIncrementorBean.class, Incrementor.class);
                EJBClient.setStrongAffinity(bean, new ClusterAffinity("ejb"));

                AtomicInteger count = new AtomicInteger();

                // Allow sufficient time for client to receive full topology
                Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

                String target = bean.increment().getNode();
                count.incrementAndGet();
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                try {
                    CountDownLatch latch = new CountDownLatch(1);
                    Future<?> future = executor.scheduleWithFixedDelay(new IncrementTask(bean, count, latch), 0, INVOCATION_WAIT, TimeUnit.MILLISECONDS);
                    latch.await();

                    lifecycle.stop(target);

                    future.cancel(false);
                    try {
                        future.get();
                    } catch (CancellationException e) {
                        // Ignore
                    }

                    lifecycle.start(target);

                    latch = new CountDownLatch(1);
                    future = executor.scheduleWithFixedDelay(new LookupTask(directory, SlowToDestroyStatefulIncrementorBean.class, latch), 0, INVOCATION_WAIT, TimeUnit.MILLISECONDS);
                    latch.await();

                    lifecycle.stop(target);

                    future.cancel(false);
                    try {
                        future.get();
                    } catch (CancellationException e) {
                        // Ignore
                    }

                    lifecycle.start(target);
                } finally {
                    executor.shutdownNow();
                }
            }
            return null;
        });
    }

    private class IncrementTask implements Runnable {
        private final Incrementor bean;
        private final CountDownLatch latch;
        private final AtomicInteger value;

        IncrementTask(Incrementor bean, AtomicInteger value, CountDownLatch latch) {
            this.bean = bean;
            this.value = value;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                int value = this.bean.increment().getValue();
                Assert.assertEquals(this.value.incrementAndGet(), value);
            } finally {
                this.latch.countDown();
            }
        }
    }

    private class LookupTask implements Runnable {
        private final EJBDirectory directory;
        private final Class<? extends Incrementor> beanClass;
        private final CountDownLatch latch;

        LookupTask(EJBDirectory directory, Class<? extends Incrementor> beanClass, CountDownLatch latch) {
            this.directory = directory;
            this.beanClass = beanClass;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                this.directory.lookupStateful(this.beanClass, Incrementor.class);
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            } finally {
                this.latch.countDown();
            }
        }
    }
}
