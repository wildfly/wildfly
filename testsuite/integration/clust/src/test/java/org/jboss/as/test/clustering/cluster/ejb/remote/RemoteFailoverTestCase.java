/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.SecureStatelessIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.SlowToDestroyStatefulIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessDDIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates @Stateful vs @Stateless failover behavior of a remotely accessed @Clustered session beans.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteFailoverTestCase extends ClusterAbstractTestCase {
    private static final Logger log = Logger.getLogger(RemoteFailoverTestCase.class);
    private static final String MODULE_NAME = "remote-failover-test";
    private static final String CLIENT_PROPERTIES = "org/jboss/as/test/clustering/cluster/ejb/remote/jboss-ejb-client.properties";
    private static final String SECURE_CLIENT_PROPERTIES = "org/jboss/as/test/clustering/cluster/ejb/remote/jboss-ejb-client-secure.properties";

    private static final int COUNT = 10;

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
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(Incrementor.class.getPackage());
        jar.addClasses(EJBDirectory.class, RemoteEJBDirectory.class);
        jar.addAsManifestResource(Incrementor.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testStatelessFailoverOnStop() throws Exception {
        EJBClientContextSelector.setup(CLIENT_PROPERTIES);
        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            Incrementor bean = context.lookupStateless(StatelessIncrementorBean.class, Incrementor.class);

            List<String> results = new ArrayList<>(COUNT);
            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.add(result.getNode());
            }

            for (String node: NODES) {
                int frequency = Collections.frequency(results, node);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }

            stop(CONTAINER_1);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
            }

            Assert.assertEquals(0, Collections.frequency(results, NODE_1));
            Assert.assertEquals(COUNT, Collections.frequency(results, NODE_2));

            start(CONTAINER_1);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
            }

            for (String node: NODES) {
                int frequency = Collections.frequency(results, node);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }

            stop(CONTAINER_2);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
            }

            Assert.assertEquals(COUNT, Collections.frequency(results, NODE_1));
            Assert.assertEquals(0, Collections.frequency(results, NODE_2));

            start(CONTAINER_2);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
            }

            for (String node: NODES) {
                int frequency = Collections.frequency(results, node);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    @Test
    public void testStatelessFailoverOnUndeploy() throws Exception {
        this.testStatelessFailoverOnUndeploy(CLIENT_PROPERTIES, StatelessIncrementorBean.class);
    }

    @Test
    public void testStatelessDDFailoverOnUndeploy() throws Exception {
        this.testStatelessFailoverOnUndeploy(CLIENT_PROPERTIES, StatelessDDIncrementorBean.class);
    }

    @Test
    public void testSecureStatelessFailoverOnUndeploy() throws Exception {
        this.testStatelessFailoverOnUndeploy(SECURE_CLIENT_PROPERTIES, SecureStatelessIncrementorBean.class);
    }

    private void testStatelessFailoverOnUndeploy(String properties, Class<? extends Incrementor> beanClass) throws Exception {
        EJBClientContextSelector.setup(properties);
        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            Incrementor bean = context.lookupStateless(beanClass, Incrementor.class);

            List<String> results = new ArrayList<>(COUNT);
            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.add(result.getNode());
            }

            for (String node: NODES) {
                int frequency = Collections.frequency(results, node);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }

            undeploy(DEPLOYMENT_1);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
            }

            Assert.assertEquals(0, Collections.frequency(results, NODE_1));
            Assert.assertEquals(COUNT, Collections.frequency(results, NODE_2));

            deploy(DEPLOYMENT_1);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
            }

            for (String node: NODES) {
                int frequency = Collections.frequency(results, node);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }

            undeploy(DEPLOYMENT_2);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
            }

            Assert.assertEquals(COUNT, Collections.frequency(results, NODE_1));
            Assert.assertEquals(0, Collections.frequency(results, NODE_2));

            deploy(DEPLOYMENT_2);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
            }

            for (String node: NODES) {
                int frequency = Collections.frequency(results, node);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    @Test
    public void testStatefulFailoverOnStop() throws Exception {
        EJBClientContextSelector.setup(CLIENT_PROPERTIES);
        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            Incrementor bean = context.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);

            Result<Integer> result = bean.increment();
            String target = result.getNode();

            Assert.assertEquals(1, result.getValue().intValue());

            // Bean should have weak affinity for this node
            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals(i + 2, result.getValue().intValue());
                Assert.assertEquals(target, result.getNode());
            }

            stop(this.findContainer(target));

            result = bean.increment();
            // Bean should failover to other node
            String failoverTarget = result.getNode();

            Assert.assertEquals(COUNT + 2, result.getValue().intValue());
            Assert.assertNotEquals(target, failoverTarget);

            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals(COUNT + i + 3, result.getValue().intValue());
                Assert.assertEquals(failoverTarget, result.getNode());
            }

            start(this.findContainer(target));

            result = bean.increment();
            // Bean should retain weak affinity to failover node
            String failbackTarget = result.getNode();

            Assert.assertEquals((COUNT * 2) + 3, result.getValue().intValue());
            Assert.assertEquals(failoverTarget, failbackTarget);

            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals((COUNT * 2) + i + 4, result.getValue().intValue());
                Assert.assertEquals(failbackTarget, result.getNode());
            }

            stop(this.findContainer(failbackTarget));

            result = bean.increment();
            failoverTarget = result.getNode();

            Assert.assertEquals((COUNT * 3) + 4, result.getValue().intValue());
            Assert.assertNotEquals(failbackTarget, failoverTarget);

            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals((COUNT * 3) + i + 5, result.getValue().intValue());
                Assert.assertEquals(failoverTarget, result.getNode());
            }

            start(this.findContainer(failbackTarget));

            result = bean.increment();
            failbackTarget = result.getNode();

            Assert.assertEquals((COUNT * 4) + 5, result.getValue().intValue());
            Assert.assertEquals(failoverTarget, failbackTarget);

            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals((COUNT * 4) + i + 6, result.getValue().intValue());
                Assert.assertEquals(failbackTarget, result.getNode());
            }
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    @Test
    public void testStatefulFailoverOnUndeploy() throws Exception {
        EJBClientContextSelector.setup(CLIENT_PROPERTIES);
        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            Incrementor bean = context.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);

            Result<Integer> result = bean.increment();
            String target = result.getNode();

            Assert.assertEquals(1, result.getValue().intValue());

            // Bean should have weak affinity for this node
            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals(i + 2, result.getValue().intValue());
                Assert.assertEquals(target, result.getNode());
            }

            undeploy(this.findDeployment(target));

            result = bean.increment();
            // Bean should failover to other node
            String failoverTarget = result.getNode();

            Assert.assertEquals(COUNT + 2, result.getValue().intValue());
            Assert.assertNotEquals(target, failoverTarget);

            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals(COUNT + i + 3, result.getValue().intValue());
                Assert.assertEquals(failoverTarget, result.getNode());
            }

            deploy(this.findDeployment(target));

            result = bean.increment();
            // Bean should retain weak affinity to failover node
            String failbackTarget = result.getNode();

            Assert.assertEquals((COUNT * 2) + 3, result.getValue().intValue());
            Assert.assertEquals(failoverTarget, failbackTarget);

            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals((COUNT * 2) + i + 4, result.getValue().intValue());
                Assert.assertEquals(failbackTarget, result.getNode());
            }

            undeploy(this.findDeployment(failbackTarget));

            result = bean.increment();
            failoverTarget = result.getNode();

            Assert.assertEquals((COUNT * 3) + 4, result.getValue().intValue());
            Assert.assertNotEquals(failbackTarget, failoverTarget);

            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals((COUNT * 3) + i + 5, result.getValue().intValue());
                Assert.assertEquals(failoverTarget, result.getNode());
            }

            deploy(this.findDeployment(failbackTarget));

            result = bean.increment();
            failbackTarget = result.getNode();

            Assert.assertEquals((COUNT * 4) + 5, result.getValue().intValue());
            Assert.assertEquals(failoverTarget, failbackTarget);

            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals((COUNT * 4) + i + 6, result.getValue().intValue());
                Assert.assertEquals(failbackTarget, result.getNode());
            }
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    @Test
    public void testConcurrentUndeploy() throws Exception {
        EJBClientContextSelector.setup(CLIENT_PROPERTIES);
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            Incrementor bean = directory.lookupStateful(SlowToDestroyStatefulIncrementorBean.class, Incrementor.class);
            String target = bean.increment().getNode();
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            try {
                CountDownLatch latch = new CountDownLatch(1);
                Future<?> future = executor.schedule(new IncrementTask(bean, 1, latch), 0, TimeUnit.MILLISECONDS);
                latch.await();

                undeploy(this.findDeployment(target));

                future.cancel(false);
                future.get();

                deploy(this.findDeployment(target));

                latch = new CountDownLatch(1);
                future = executor.schedule(new LookupTask(directory, SlowToDestroyStatefulIncrementorBean.class, latch), 0, TimeUnit.MILLISECONDS);
                latch.await();

                undeploy(this.findDeployment(target));

                future.cancel(false);
                future.get();

                deploy(this.findDeployment(target));
            } finally {
                executor.shutdownNow();
            }
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    @Test
    public void testConcurrentStop() throws Exception {
        EJBClientContextSelector.setup(CLIENT_PROPERTIES);
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            Incrementor bean = directory.lookupStateful(SlowToDestroyStatefulIncrementorBean.class, Incrementor.class);
            String target = bean.increment().getNode();
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            try {
                CountDownLatch latch = new CountDownLatch(1);
                Future<?> future = executor.schedule(new IncrementTask(bean, 1, latch), 0, TimeUnit.MILLISECONDS);
                latch.await();

                stop(this.findContainer(target));

                future.cancel(false);
                future.get();

                start(this.findContainer(target));

                latch = new CountDownLatch(1);
                future = executor.schedule(new LookupTask(directory, SlowToDestroyStatefulIncrementorBean.class, latch), 0, TimeUnit.MILLISECONDS);
                latch.await();

                stop(this.findContainer(target));

                future.cancel(false);
                future.get();

                start(this.findContainer(target));
            } catch (CancellationException e) {
                // Expected
            } finally {
                executor.shutdownNow();
            }
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    private class IncrementTask implements Runnable {
        private final Incrementor bean;
        private volatile int value;
        private final CountDownLatch latch;

        IncrementTask(Incrementor bean, int initialValue, CountDownLatch latch) {
            this.bean = bean;
            this.value = initialValue;
            this.latch = latch;
        }

        @Override
        public void run() {
            int value = this.bean.increment().getValue();
            Assert.assertEquals(this.value + 1, value);
            this.value = value;
            this.latch.countDown();
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
                this.latch.countDown();
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
