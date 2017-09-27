/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.clustering.twoclusters;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.ExtendedClusterAbstractTestCase;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.twoclusters.bean.SerialBean;
import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSB;
import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Test EJBClient functionality across two clusters with fail-over.
 * <p/>
 * A client makes an invocation on one clustered app (on cluster A) which in turn
 * forwards the invocation on a second clustered app (on cluster B).
 * <p/>
 * cluster A = {node0, node1}
 * cluster B = {node2, node3}
 * <p/>
 * Under constant client load, we stop and then restart individual servers.
 * <p/>
 * We expect that client invocations will not be affected.
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractRemoteEJBTwoClusterTestCase extends ExtendedClusterAbstractTestCase {

    private static final Logger logger = Logger.getLogger(AbstractRemoteEJBTwoClusterTestCase.class);
    private static final String RECEIVER_MODULE_NAME = "clusterbench-ee6-ejb";

    private static long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);
    private static long FAILURE_FREE_TIME = TimeoutUtil.adjust(5000);
    // 200 ms keeps the test stable
    private static long INVOCATION_WAIT = TimeoutUtil.adjust(200);
    private static long SERVER_DOWN_TIME = TimeoutUtil.adjust(5000);
    // allowed percentage of exceptions (exceptions / invocations)
    private static double EXCEPTION_PERCENTAGE = 0.1;

    private final ExceptionSupplier<EJBDirectory, NamingException> directorySupplier;
    private final Supplier<Class> implementationSupplier;

    AbstractRemoteEJBTwoClusterTestCase(ExceptionSupplier<EJBDirectory, NamingException> directorySupplier, Supplier<Class> implementationSupplier) {
        this.directorySupplier = directorySupplier;
        this.implementationSupplier = implementationSupplier;
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(CONTAINER_3)
    public static Archive<?> deployment2() {
        return getNonForwardingDeployment();
    }

    @Deployment(name = DEPLOYMENT_4, managed = false, testable = false)
    @TargetsContainer(CONTAINER_4)
    public static Archive<?> deployment3() {
        return getNonForwardingDeployment();
    }

    private static Archive<?> getNonForwardingDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, RECEIVER_MODULE_NAME + ".jar");
        ejbJar.addPackage(CommonStatefulSB.class.getPackage());
        ejbJar.addPackage(RemoteStatefulSB.class.getPackage());
        ejbJar.addClass(SerialBean.class.getName());
        return ejbJar;
    }

    /**
     * Tests that EJBClient invocations on stateful session beans can still successfully be processed
     * as long as one node in each cluster is available.
     */
    @Test
    public void test() throws Exception {
        try (EJBDirectory directory = directorySupplier.get()) {
            // get the correct forwarder deployment on cluster A
            RemoteStatefulSB bean = directory.lookupStateful(implementationSupplier.get().getSimpleName(), RemoteStatefulSB.class);

            AtomicInteger count = new AtomicInteger();

            // Allow sufficient time for client to receive full topology
            logger.trace("Waiting for clusters to form:");
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            int newSerialValue = bean.getSerialAndIncrement();
            int newCountValue = count.getAndIncrement();
            logger.trace("First invocation: count = " + newCountValue + ", serial = " + newSerialValue);

            //
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            CountDownLatch latch = new CountDownLatch(1);
            ClientInvocationTask client = new ClientInvocationTask(bean, latch, count);

            try {
                // set up the client invocations
                Future<?> future = executor.scheduleWithFixedDelay(client, 0, INVOCATION_WAIT, TimeUnit.MILLISECONDS);
                latch.await();

                // a few seconds of non-failure behaviour
                Thread.sleep(FAILURE_FREE_TIME);

                logger.debug("------ Shutdown clusterA-node0 -----");
                // stop cluster A node 0
                stop(GRACEFUL_SHUTDOWN_TIMEOUT, CONTAINER_1);
                // Let the server stay down for a while
                Thread.sleep(SERVER_DOWN_TIME);
                logger.debug("------ Startup clusterA-node0 -----");
                start(CONTAINER_1);

                // a few seconds of non-failure behaviour
                Thread.sleep(FAILURE_FREE_TIME);

                logger.debug("----- Shutdown clusterA-node1 -----");
                // stop cluster A node 1
                stop(GRACEFUL_SHUTDOWN_TIMEOUT, CONTAINER_2);
                // Let the server stay down for a while
                Thread.sleep(SERVER_DOWN_TIME);
                logger.trace("------ Startup clusterA-node1 -----");
                start(CONTAINER_2);

                // a few seconds of non-failure behaviour
                Thread.sleep(FAILURE_FREE_TIME);

                logger.debug("----- Shutdown clusterB-node0 -----");
                // stop cluster B node 0
                stop(GRACEFUL_SHUTDOWN_TIMEOUT, CONTAINER_3);
                // Let the server stay down for a while
                Thread.sleep(SERVER_DOWN_TIME);
                logger.trace("------ Startup clusterB-node0 -----");
                start(CONTAINER_3);

                // a few seconds of non-failure behaviour
                Thread.sleep(FAILURE_FREE_TIME);

                logger.debug("----- Shutdown clusterB-node1 -----");
                // stop cluster B node 1
                stop(GRACEFUL_SHUTDOWN_TIMEOUT, CONTAINER_4);
                // Let the server stay down for a while
                Thread.sleep(SERVER_DOWN_TIME);
                logger.debug("------ Startup clusterB-node1 -----");
                start(CONTAINER_4);

                // a few seconds of non-failure behaviour
                Thread.sleep(FAILURE_FREE_TIME);

                // cancel the executor and wait for it to complete
                future.cancel(false);
                try {
                    future.get();
                } catch (CancellationException e) {
                    logger.debug("Could not cancel future: " + e.toString());
                }

                // test is completed, report results
                double invocations = client.getInvocationCount();
                double exceptions = client.getExceptionCount();
                logger.debug("Total invocations = " + invocations + ", total exceptions = " + exceptions);
                Assert.assertTrue("Too many exceptions! percentage = " + 100 * (exceptions / invocations), (exceptions / invocations) < EXCEPTION_PERCENTAGE);

            } catch (Exception e) {
                Assert.fail("Exception occurred on client: " + e.getMessage() + ", test did not complete successfully (inner)");

            } finally {
                logger.debug("Shutting down executor");
                executor.shutdownNow();
            }

        } catch (Exception e) {
            Assert.fail("Exception occurred on client: " + e.getMessage() + ", test did not complete successfully (outer)");
        }
    }

    private class ClientInvocationTask implements Runnable {
        private final RemoteStatefulSB bean;
        private final CountDownLatch latch;
        private final AtomicInteger count;
        // count of exceptional responses
        private int invocationCount;
        private int exceptionCount;
        // true of the last invocation resulted in an exception
        private boolean lastWasException;
        private boolean firstTime;

        ClientInvocationTask(RemoteStatefulSB bean, CountDownLatch latch, AtomicInteger count) {
            this.bean = bean;
            this.latch = latch;
            this.count = count;
            this.invocationCount = 0;
            this.exceptionCount = 0;

            this.lastWasException = false;
            this.firstTime = true;
        }

        public int getExceptionCount() {
            return exceptionCount;
        }

        public int getInvocationCount() {
            return invocationCount;
        }

        @Override
        public void run() {
            try {
                // make an invocation on the remote SFSB
                this.invocationCount++;
                logger.trace("CLIENT: start invocation (" + this.invocationCount + ")");
                int value = this.bean.getSerialAndIncrement();

                // check to see if the previous invocation was exceptional
                if (this.lastWasException) {
                    // reset the value of the counter
                    this.count.set(value + 1);
                    this.lastWasException = false;
                    logger.trace("CLIENT: made invocation (" + this.invocationCount + ") on bean, resetting count = " + (value + 1));
                } else {
                    int count = this.count.getAndIncrement();
                    logger.trace("CLIENT: made invocation (" + this.invocationCount + ") on bean, count = " + count + ", value = " + value);
                }
            } catch (Exception e) {
                // log the occurrence of the exception
                logger.debug("CLIENT: Exception invoking (" + this.invocationCount + ") on bean from client: " + e.getMessage());
                this.exceptionCount++;
                this.lastWasException = true;
            } finally {
                if (firstTime) {
                    this.firstTime = false;
                    this.latch.countDown();
                }
            }
        }
    }
}