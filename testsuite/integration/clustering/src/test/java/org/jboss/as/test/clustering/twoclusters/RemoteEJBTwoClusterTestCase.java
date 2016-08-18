package org.jboss.as.test.clustering.twoclusters;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.clustering.cluster.ExtendedClusterAbstractTestCase;
import org.jboss.as.test.clustering.twoclusters.bean.SerialBean;
import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSB;
import org.jboss.as.test.clustering.twoclusters.bean.forwarding.AbstractForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.twoclusters.bean.forwarding.ForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.twoclusters.bean.forwarding.NonTxForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
public class RemoteEJBTwoClusterTestCase extends ExtendedClusterAbstractTestCase {

    private static final Logger logger = Logger.getLogger(RemoteEJBTwoClusterTestCase.class);
    private static final String FORWARDER_MODULE_NAME = "clusterbench-ee6-ejb-forwarder";
    private static final String FORWARDER_WITH_TXN_MODULE_NAME = "clusterbench-ee6-ejb-forwarder-with-txn";
    private static final String RECEIVER_MODULE_NAME = "clusterbench-ee6-ejb";
    // EJBClient configuartion to cluster A
    private static final String FORWARDER_CLIENT_PROPERTIES = "org/jboss/as/test/clustering/twoclusters/forwarder-jboss-ejb-client.properties";

    private static long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);
    private static long FAILURE_FREE_TIME = TimeoutUtil.adjust(5000);
    // 200 ms keeps the test stable
    private static long INVOCATION_WAIT = TimeoutUtil.adjust(200);
    private static long SERVER_DOWN_TIME = TimeoutUtil.adjust(5000);
    // allowed percentage of exceptions (exceptions / invocations)
    private static double EXCEPTION_PERCENTAGE = 0.1;

    // EJB deployment lookup helpers
    private static EJBDirectory beanDirectory;
    private static EJBDirectory txnBeanDirectory;

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return getNonTxForwardingDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return getNonTxForwardingDeployment();
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

    @Deployment(name = "deployment-4", managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0_txn() {
        return getTxForwardingDeployment();
    }

    @Deployment(name = "deployment-5", managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1_txn() {
        return getTxForwardingDeployment();
    }

    private static Archive<?> getTxForwardingDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, FORWARDER_WITH_TXN_MODULE_NAME + ".jar");
        ejbJar.addPackage(CommonStatefulSB.class.getPackage());
        ejbJar.addPackage(RemoteStatefulSB.class.getPackage());
        ejbJar.addClass(SerialBean.class.getName());
        // the forwarding classes
        ejbJar.addClass(AbstractForwardingStatefulSBImpl.class.getName());
        ejbJar.addPackage(ForwardingStatefulSBImpl.class.getPackage());
        // remote outbound connection configuration
        ejbJar.addAsManifestResource(RemoteEJBTwoClusterTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        log.info(ejbJar.toString(true));
        return ejbJar;
    }

    private static Archive<?> getNonTxForwardingDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, FORWARDER_MODULE_NAME + ".jar");
        ejbJar.addPackage(CommonStatefulSB.class.getPackage());
        ejbJar.addPackage(RemoteStatefulSB.class.getPackage());
        ejbJar.addClass(SerialBean.class.getName());
        // the forwarding classes
        ejbJar.addClass(AbstractForwardingStatefulSBImpl.class.getName());
        ejbJar.addClass(NonTxForwardingStatefulSBImpl.class.getName());
        // remote outbound connection configuration
        ejbJar.addAsManifestResource(RemoteEJBTwoClusterTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        log.info(ejbJar.toString(true));
        return ejbJar;
    }

    private static Archive<?> getNonForwardingDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, RECEIVER_MODULE_NAME + ".jar");
        ejbJar.addPackage(CommonStatefulSB.class.getPackage());
        ejbJar.addPackage(RemoteStatefulSB.class.getPackage());
        ejbJar.addClass(SerialBean.class.getName());
        log.info(ejbJar.toString(true));
        return ejbJar;
    }

    /*
     * In the test case framework:
     * - containers are deployed by the framework before any test runs
     * - containers are undeployed by the test case itself
     * - deployments are deployed by the test case itself
     * - deployments are undeployed by the test case itself
     */

    @BeforeClass
    public static void beforeTest() throws NamingException {
        beanDirectory = new RemoteEJBDirectory(FORWARDER_MODULE_NAME);
        txnBeanDirectory = new RemoteEJBDirectory(FORWARDER_WITH_TXN_MODULE_NAME);
    }

    @AfterClass
    public static void destroy() throws NamingException {
        beanDirectory.close();
        txnBeanDirectory.close();
    }

    @After
    public void afterTest() throws Exception {
    }

    /*
     * Tests concurrent fail-over without a managed transaction context on the forwarder.
     */
    @Test
    @InSequence(1)
    public void testConcurrentFailoverOverWithoutTransactions() throws Exception {

        testConcurrentFailoverOverWithTwoClusters(false);
    }

    /*
     * Tests concurrent fail-over with a managed transaction context on the forwarder.
     */
    @Test
    @InSequence(2)
    public void testConcurrentFailoverOverWithTransactions() throws Exception {
        // some additional transaction-oriented deployments for containers 1 and 2
        this.deploy("deployment-4", "deployment-5");

        testConcurrentFailoverOverWithTwoClusters(true);

        // additional un-deployments for containers 1 and 2
        this.undeploy("deployment-4", "deployment-5");
    }

    /*
     * Tests that EJBClient invocations on stateful session beans can still successfully be processed
     * as long as one node in each cluster is available.
     */
    public void testConcurrentFailoverOverWithTwoClusters(boolean useTransactions) throws Exception {
        ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(FORWARDER_CLIENT_PROPERTIES);

        try {
            try {
                // get the correct forwarder deployment on cluster A
                RemoteStatefulSB bean = null;
                if (useTransactions)
                    bean = txnBeanDirectory.lookupStateful(ForwardingStatefulSBImpl.class, RemoteStatefulSB.class);
                else
                    bean = beanDirectory.lookupStateful(NonTxForwardingStatefulSBImpl.class, RemoteStatefulSB.class);

                AtomicInteger count = new AtomicInteger();

                // Allow sufficient time for client to receive full topology
                logger.info("Waiting for clusters to form:");
                Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

                int newSerialValue = bean.getSerialAndIncrement();
                int newCountValue = count.getAndIncrement();
                logger.info("First invocation: count = " + newCountValue + ", serial = " + newSerialValue);

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

                    logger.info("------ Shutdown clusterA-node0 -----");
                    // stop cluster A node 0
                    stop(CONTAINER_1);
                    // Let the server stay down for a while
                    Thread.sleep(SERVER_DOWN_TIME);
                    logger.info("------ Startup clusterA-node0 -----");
                    start(CONTAINER_1);

                    // a few seconds of non-failure behaviour
                    Thread.sleep(FAILURE_FREE_TIME);

                    logger.info("----- Shutdown clusterA-node1 -----");
                    // stop cluster A node 1
                    stop(CONTAINER_2);
                    // Let the server stay down for a while
                    Thread.sleep(SERVER_DOWN_TIME);
                    logger.info("------ Startup clusterA-node1 -----");
                    start(CONTAINER_2);

                    // a few seconds of non-failure behaviour
                    Thread.sleep(FAILURE_FREE_TIME);

                    logger.info("----- Shutdown clusterB-node0 -----");
                    // stop cluster B node 0
                    stop(CONTAINER_3);
                    // Let the server stay down for a while
                    Thread.sleep(SERVER_DOWN_TIME);
                    logger.info("------ Startup clusterB-node0 -----");
                    start(CONTAINER_3);

                    // a few seconds of non-failure behaviour
                    Thread.sleep(FAILURE_FREE_TIME);

                    logger.info("----- Shutdown clusterB-node1 -----");
                    // stop cluster B node 1
                    stop(CONTAINER_4);
                    // Let the server stay down for a while
                    Thread.sleep(SERVER_DOWN_TIME);
                    logger.info("------ Startup clusterB-node1 -----");
                    start(CONTAINER_4);

                    // a few seconds of non-failure behaviour
                    Thread.sleep(FAILURE_FREE_TIME);

                    // cancel the executor and wait for it to complete
                    future.cancel(false);
                    try {
                        future.get();
                    } catch (CancellationException e) {
                        logger.info("Could not cancel future: " + e.toString());
                    }

                    // test is completed, report results
                    double invocations = client.getInvocationCount();
                    double exceptions = client.getExceptionCount();
                    logger.info("Total invocations = " + invocations + ", total exceptions = " + exceptions);
                    Assert.assertTrue("Too many exceptions! percentage = " + 100 * (exceptions/invocations), (exceptions/invocations) < EXCEPTION_PERCENTAGE);

                } catch (Exception e) {
                    Assert.fail("Exception occurred on client: " + e.getMessage() + ", test did not complete successfully (inner)");

                } finally {
                    logger.info("Shutting down executor");
                    executor.shutdownNow();
                }

            } catch (Exception e) {
                Assert.fail("Exception occurred on client: " + e.getMessage() + ", test did not complete successfully (outer)");

            }
        } finally {
            // reset the EJBClient context to the original instance
            EJBClientContext.setSelector(selector);
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
                logger.info("CLIENT: start invocation (" + this.invocationCount + ")");
                int value = this.bean.getSerialAndIncrement();

                // check to see if the previous invocation was exceptional
                if (this.lastWasException) {
                    // reset the value of the counter
                    this.count.set(value+1);
                    this.lastWasException = false;
                    logger.info("CLIENT: made invocation (" + this.invocationCount + ") on bean, resetting count = " + (value+1));
                } else {
                    int count = this.count.getAndIncrement();
                    logger.info("CLIENT: made invocation (" + this.invocationCount + ") on bean, count = " + count + ", value = " + value);
                }
            } catch (Exception e) {
                // log the occurrence of the exception
                logger.info("CLIENT: Exception invoking (" + this.invocationCount + ") on bean from client: " + e.getMessage());
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