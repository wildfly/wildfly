package org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusteringTestConstants;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that if a EJB client invokes on a EJB method when the server is in the process of undeploying the deployment, then the EJB client is returned an appropriate "bean isn't available"
 * message, so that the EJB client can retry the invocation on a different eligible server node.
 *
 * @author: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SlowUndeploymentRemoteFailoverTestCase {

    private static final Logger logger = Logger.getLogger(SlowUndeploymentRemoteFailoverTestCase.class);

    private static final String MODULE_NAME = "slow-undeployment-failover-testcase";

    private static Context jndiContext;

    @ArquillianResource
    protected ContainerController controller;
    @ArquillianResource
    protected Deployer deployer;

    private ContextSelector<EJBClientContext> previousSelector;
    private InvocationCountTrackingClientInterceptor clientInterceptor;

    private Collection<String> undeployedDeployments = new HashSet<String>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Properties props = new Properties();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiContext = new InitialContext(props);

    }

    @Deployment(name = ClusteringTestConstants.DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(ClusteringTestConstants.CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = ClusteringTestConstants.DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(ClusteringTestConstants.CONTAINER_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addClasses(NodeNameRetriever.class, TimeoutUtil.class, SlowUndeployingClusteredSFSB.class, SlowUndeployingClusteredSLSB.class);
        return ejbJar;
    }

    @Before
    public void before() throws Exception {
        // start containers
        controller.start(ClusteringTestConstants.CONTAINER_1);
        controller.start(ClusteringTestConstants.CONTAINER_2);
        // Also deploy
        deployer.deploy(ClusteringTestConstants.DEPLOYMENT_1);
        deployer.deploy(ClusteringTestConstants.DEPLOYMENT_2);
        undeployedDeployments.clear();

        // setup selector
        previousSelector = EJBClientContextSelector.setup("cluster/ejb3/stateful/failover/slow-undeployment-failover-jboss-ejb-client.properties");
        clientInterceptor = new InvocationCountTrackingClientInterceptor();
        EJBClientContext.getCurrent().registerInterceptor(99999, clientInterceptor);
    }

    @After
    public void after() throws Exception {
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }
        if (!undeployedDeployments.contains(ClusteringTestConstants.DEPLOYMENT_1)) {
            try {
                deployer.undeploy(ClusteringTestConstants.DEPLOYMENT_1);
            } catch (Throwable t) {
                logger.info("Failed to undeploy " + ClusteringTestConstants.DEPLOYMENT_1);
            }
        }
        if (!undeployedDeployments.contains(ClusteringTestConstants.DEPLOYMENT_2)) {
            try {
                deployer.undeploy(ClusteringTestConstants.DEPLOYMENT_2);
            } catch (Throwable t) {
                logger.info("Failed to undeploy " + ClusteringTestConstants.DEPLOYMENT_2);
            }
        }
        // stop the containers
        controller.stop(ClusteringTestConstants.CONTAINER_2);
        controller.stop(ClusteringTestConstants.CONTAINER_1);
    }

    /**
     * Two servers (Server A and Server B) in a cluster. Both servers host the same deployment "foo" which contains a SLSB.
     * <p/>
     * Tests that when a SLSB is invoked on a server (for example, Server A) and the deployment "foo" that's hosting the SLSB, is being currently undeployed, then the server sends back an appropriate message which then triggers the
     * client to retry the invocation on the other eligible node (Server B)
     *
     * @throws Exception
     */
    @Test
    @Ignore("https://issues.jboss.org/browse/WFLY-1306")
    public void testSLSBInvocationRetryWhenOriginalInvocationHappensDuringEJBContainerShutdown() throws Exception {
        final NodeNameRetriever bean = (NodeNameRetriever) jndiContext.lookup("ejb:/" + MODULE_NAME + "/" + "" + "/" + SlowUndeployingClusteredSLSB.class.getSimpleName() + "!" + NodeNameRetriever.class.getName());
        doTest(bean);
    }

    /**
     * Two servers (Server A and Server B) in a cluster. Both servers host the same deployment "foo" which contains a SFSB.
     * <p/>
     * Tests that when a SFSB is invoked on a server (for example, Server A) and the deployment "foo" that's hosting the SFSB, is being currently undeployed, then the server sends back an appropriate message which then triggers the
     * client to retry the invocation on the other eligible node (Server B)
     *
     * @throws Exception
     */
    @Test
    @Ignore("https://issues.jboss.org/browse/WFLY-1306")
    public void testSFSBInvocationRetryWhenOriginalInvocationHappensDuringEJBContainerShutdown() throws Exception {
        final NodeNameRetriever bean = (NodeNameRetriever) jndiContext.lookup("ejb:/" + MODULE_NAME + "/" + "" + "/" + SlowUndeployingClusteredSFSB.class.getSimpleName() + "!" + NodeNameRetriever.class.getName() + "?stateful");
        doTest(bean);
    }

    /**
     * Two servers (Server A and Server B) in a cluster. Both servers host the same deployment "foo" which contains a SFSB.
     * <p/>
     * Tests that when a session creation request for SFSB is received on a server (for example, Server A) and the deployment "foo" that's hosting the SFSB, is being currently undeployed, then the client retries the session
     * creation request on the other eligible node (Server B)
     *
     * @throws Exception
     */
    @Test
    @Ignore("https://issues.jboss.org/browse/WFLY-1306")
    public void testSFSBSessionCreationDuringEJBContainerShutdown() throws Exception {
        final StopSignal stopSignal = new StopSignal();
        final Callable<Void> task = new StatefulSessionCreatingTask(stopSignal);
        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        try {
            // trigger the session creation
            final Future<Void> futureResult = executorService.submit(task);
            final long waitTimeBeforeUndeployment = TimeoutUtil.adjust(250);
            logger.info("Sleeping for " + waitTimeBeforeUndeployment + " milli. sec to let the EJB sessions be created a few times, before undeploying it");
            Thread.sleep(waitTimeBeforeUndeployment);
            // now undeploy from one of the nodes
            undeploy(ClusteringTestConstants.DEPLOYMENT_1);
            // now wait for a few more moments to let the session creation continue
            final long waitTimeAfterUndeployment = TimeoutUtil.adjust(500);
            logger.info("Sleeping for " + waitTimeAfterUndeployment + " milli. sec to let the EJB sessions be created, after the deployment has been undeployed from one of the nodes");
            Thread.sleep(waitTimeAfterUndeployment);
            // time to stop the invocations
            stopSignal.stop = true;
            // get the result
            futureResult.get();
        } finally {
            executorService.shutdown();
        }
    }

    private void doTest(final NodeNameRetriever bean) throws Exception {
        final StopSignal stopSignal = new StopSignal();
        final Callable<Integer> task = new EJBInvokingTask(bean, stopSignal);
        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        try {
            final Future<Integer> futureResult = executorService.submit(task);
            final long initialFewInvocationsWaitTime = TimeoutUtil.adjust(500);
            logger.info("Sleeping for " + initialFewInvocationsWaitTime + " milli. sec to let the EJB be invoked a few times");
            Thread.sleep(initialFewInvocationsWaitTime);
            // stop the invocations
            stopSignal.stop = true;
            final int numberOfExplicitClientTriggeredInvocations = futureResult.get();
            logger.info("Explicit client triggered invocations on the bean was " + numberOfExplicitClientTriggeredInvocations + " times and client interceptor was invoked " + clientInterceptor.invocationCount + " times");
            Assert.assertTrue("No invocations were done on the bean", numberOfExplicitClientTriggeredInvocations > 0);
            Assert.assertEquals("Unexpected number of invocations on the client interceptor", numberOfExplicitClientTriggeredInvocations, clientInterceptor.invocationCount);

            // now that we have ensured that invocations are working fine on the bean and the client interceptor, let's re-trigger the invocations
            // and after a while undeploy the deployment from one of the nodes and expect a retry.
            // let's first reset the counters and other flags
            clientInterceptor.invocationCount = 0;
            stopSignal.stop = false;
            final Future<Integer> futureResultExpectingRetry = executorService.submit(task);
            final long waitTimeBeforeUndeployment = TimeoutUtil.adjust(500);
            logger.info("Sleeping for " + waitTimeBeforeUndeployment + " milli. sec to let the EJB be invoked a few times, before undeploying it");
            Thread.sleep(waitTimeBeforeUndeployment);
            // now undeploy from one of the nodes
            undeploy(ClusteringTestConstants.DEPLOYMENT_1);
            // now wait for a few more moments to let the retry happen and EJB invocations continue for a while
            final long waitTimeAfterUndeployment = TimeoutUtil.adjust(1000);
            logger.info("Sleeping for " + waitTimeAfterUndeployment + " milli. sec to let the EJB be invoked a few times, after the deployment has been undeployed from one of the nodes");
            Thread.sleep(waitTimeAfterUndeployment);
            // time to stop the invocations
            stopSignal.stop = true;
            // get the result
            final int explicitClientTriggeredInvocationsOnBeanExpectingFailover = futureResultExpectingRetry.get();
            logger.info("Explicit client triggered invocations on the bean was " + explicitClientTriggeredInvocationsOnBeanExpectingFailover + " times and client interceptor was invoked " + clientInterceptor.invocationCount + " times");
            Assert.assertTrue("No invocations were done on the bean", explicitClientTriggeredInvocationsOnBeanExpectingFailover > 0);
            final int NUM_RETRIES_EXPECTED = 1;
            Assert.assertEquals("Client interceptor invocation count = " + clientInterceptor.invocationCount + " was expected to be " + NUM_RETRIES_EXPECTED + " greater than the explicit client triggered invocations on the bean, " +
                    "since we are expecting " + NUM_RETRIES_EXPECTED + " implicit retry/retries during failover", explicitClientTriggeredInvocationsOnBeanExpectingFailover + NUM_RETRIES_EXPECTED, clientInterceptor.invocationCount);

        } finally {
            executorService.shutdown();
        }

    }

    private void undeploy(final String deploymentName) {
        deployer.undeploy(deploymentName);
        logger.info("Undeployed " + deploymentName);
        undeployedDeployments.add(deploymentName);
    }

    private class EJBInvokingTask implements Callable<Integer> {

        private final StopSignal stopSignalled;
        private final NodeNameRetriever bean;

        EJBInvokingTask(final NodeNameRetriever bean, final StopSignal stopSignalled) {
            this.bean = bean;
            this.stopSignalled = stopSignalled;
        }

        @Override
        public Integer call() throws Exception {
            if (stopSignalled.stop) {
                return 0;
            }
            int invocationCount = 0;
            // keep invoking on the bean until a stop is requested by the testcase
            while (!stopSignalled.stop) {
                invocationCount++;
                bean.getNodeName();
            }
            return invocationCount;
        }
    }

    private class StatefulSessionCreatingTask implements Callable<Void> {

        private final StopSignal stopSignalled;

        StatefulSessionCreatingTask(final StopSignal stopSignalled) {
            this.stopSignalled = stopSignalled;
        }

        @Override
        public Void call() throws Exception {
            if (stopSignalled.stop) {
                return null;
            }
            // keep creating EJB session until a stop is requested by the testcase
            while (!stopSignalled.stop) {
                final NodeNameRetriever bean = (NodeNameRetriever) jndiContext.lookup("ejb:/" + MODULE_NAME + "/" + "" + "/" + SlowUndeployingClusteredSFSB.class.getSimpleName() + "!" + NodeNameRetriever.class.getName() + "?stateful");
            }
            return null;
        }
    }

    private class StopSignal {
        volatile boolean stop = false;
    }

    // A EJB client interceptor which keeps track of the number of times its handleInvocation() method has been invoked
    private class InvocationCountTrackingClientInterceptor implements EJBClientInterceptor {

        volatile int invocationCount;

        @Override
        public void handleInvocation(EJBClientInvocationContext context) throws Exception {
            invocationCount += 1;
            context.sendRequest();
        }

        @Override
        public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
            return context.getResult();
        }
    }

}
