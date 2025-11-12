/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.forwarding;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.fail;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.util.PropertyPermission;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.common.CommonStatefulSB;
import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.forwarding.AbstractForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.forwarding.ForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.forwarding.NonTxForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.stateful.RemoteStatefulSB;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.NamingEJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.ejb.client.EJBClientPermission;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Test Jakarta Enterprise Beans Client functionality across two clusters with fail-over.
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
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup(AbstractRemoteEJBForwardingTestCase.ServerSetupTask.class)
public abstract class AbstractRemoteEJBForwardingTestCase extends AbstractClusteringTestCase {

    @BeforeClass
    public static void beforeClass() {
        IntermittentFailure.thisTestIsFailingIntermittently("https://issues.redhat.com/browse/WFLY-10607");
    }

    private static final long FAILURE_FREE_TIME = TimeoutUtil.adjust(5_000);
    private static final long SERVER_DOWN_TIME = TimeoutUtil.adjust(5_000);
    private static final long INVOCATION_WAIT = TimeoutUtil.adjust(1_000);

    private final ExceptionSupplier<EJBDirectory, NamingException> directorySupplier;
    private final String implementationClass;

    private static final Logger logger = Logger.getLogger(AbstractRemoteEJBForwardingTestCase.class);

    AbstractRemoteEJBForwardingTestCase(ExceptionSupplier<EJBDirectory, NamingException> directorySupplier, String implementationClass) {
        super(NODE_1_2_3_4);

        this.directorySupplier = directorySupplier;
        this.implementationClass = implementationClass;
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> deployment3() {
        return createNonForwardingDeployment();
    }

    @Deployment(name = DEPLOYMENT_4, managed = false, testable = false)
    @TargetsContainer(NODE_4)
    public static Archive<?> deployment4() {
        return createNonForwardingDeployment();
    }

    public static Archive<?> createForwardingDeployment(String moduleName, boolean tx) {
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, moduleName + ".jar");
        ejbJar.addClass(CommonStatefulSB.class);
        ejbJar.addClass(RemoteStatefulSB.class);
        // the forwarding classes
        ejbJar.addClass(AbstractForwardingStatefulSBImpl.class);
        if (tx) {
            ejbJar.addClass(ForwardingStatefulSBImpl.class);
        } else {
            ejbJar.addClass(NonTxForwardingStatefulSBImpl.class);
        }
        ejbJar.addClasses(EJBDirectory.class, NamingEJBDirectory.class, RemoteEJBDirectory.class);
        // remote outbound connection configuration
        ejbJar.addAsManifestResource(AbstractRemoteEJBForwardingTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        ejbJar.addAsResource(createPermissionsXmlAsset(
                new EJBClientPermission("changeWeakAffinity"),
                new FilePermission("<<ALL FILES>>", "read"),
                new PropertyPermission("jboss.node.name", "read"),
                new RuntimePermission("getClassLoader"),
                new SocketPermission("localhost", "resolve")
        ), "META-INF/jboss-permissions.xml");
        return ejbJar;
    }

    public static Archive<?> createNonForwardingDeployment() {
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, AbstractForwardingStatefulSBImpl.MODULE_NAME + ".jar");
        ejbJar.addPackage(CommonStatefulSB.class.getPackage());
        ejbJar.addPackage(RemoteStatefulSB.class.getPackage());
        ejbJar.addAsResource(createPermissionsXmlAsset(
                new PropertyPermission("jboss.node.name", "read")
        ), "META-INF/jboss-permissions.xml");
        return ejbJar;
    }

    /**
     * Tests that Jakarta Enterprise Beans Client invocations on stateful session beans can still successfully be processed
     * as long as one node in each cluster is available.
     */
    @Test
    public void test() throws Exception {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        try (EJBDirectory directory = directorySupplier.get()) {
            // get the correct forwarder deployment on cluster A
            RemoteStatefulSB bean = directory.lookupStateful(implementationClass, RemoteStatefulSB.class);

            // Allow enough time for the client to receive full topology
            logger.debug("Waiting for clusters to form.");
            Thread.sleep(FAILURE_FREE_TIME);

            int newSerialValue = bean.getSerialAndIncrement();
            logger.debugf("First invocation: serial = %d", newSerialValue);

            ClientInvocationTask client = new ClientInvocationTask(bean, newSerialValue);

            // set up the client invocations
            executor.scheduleWithFixedDelay(client, 0, INVOCATION_WAIT, TimeUnit.MILLISECONDS);

            // a few seconds of non-failure behavior
            Thread.sleep(FAILURE_FREE_TIME);
            client.assertNoExceptions("at the beginning of the test");

            logger.debugf("------ Shutdown clusterA-node0 -----");
            stop(NODE_1);
            Thread.sleep(SERVER_DOWN_TIME);
            client.assertNoExceptions("after clusterA-node0 was shut down");

            logger.debug("------ Startup clusterA-node0 -----");
            start(NODE_1);
            Thread.sleep(FAILURE_FREE_TIME);
            client.assertNoExceptions("after clusterA-node0 was brought up");

            logger.debug("----- Shutdown clusterA-node1 -----");
            stop(NODE_2);
            Thread.sleep(SERVER_DOWN_TIME);

            logger.debug("------ Startup clusterA-node1 -----");
            start(NODE_2);
            Thread.sleep(FAILURE_FREE_TIME);
            client.assertNoExceptions("after clusterA-node1 was brought back up");

            logger.debug("----- Shutdown clusterB-node0 -----");
            stop(NODE_3);
            Thread.sleep(SERVER_DOWN_TIME);
            client.assertNoExceptions("after clusterB-node0 was shut down");

            logger.debug("------ Startup clusterB-node0 -----");
            start(NODE_3);
            Thread.sleep(FAILURE_FREE_TIME);
            client.assertNoExceptions("after clusterB-node0 was brought back up");

            logger.debug("----- Shutdown clusterB-node1 -----");
            stop(NODE_4);
            Thread.sleep(SERVER_DOWN_TIME);

            logger.debug("------ Startup clusterB-node1 -----");
            start(NODE_4);
            Thread.sleep(FAILURE_FREE_TIME);

            // final assert
            client.assertNoExceptions("after clusterB-node1 was brought back up");
        } finally {
            executor.shutdownNow();
        }
    }

    private static class ClientInvocationTask implements Runnable {
        private final RemoteStatefulSB bean;
        private int expectedSerial;
        private volatile Exception firstException;
        private int invocationCount;

        ClientInvocationTask(RemoteStatefulSB bean, int serial) {
            this.bean = bean;
            this.expectedSerial = serial;
        }

        /**
         * Asserts that there were no exception during the last test period.
         */
        void assertNoExceptions(String when) {
            if (firstException != null) {
                logger.error(firstException);
                fail("Client threw an exception " + when + ": " + firstException);
            }
        }

        @Override
        public void run() {
            invocationCount++;

            try {
                int serial = this.bean.getSerialAndIncrement();
                logger.debugf("Jakarta Enterprise Beans client invocation #%d on bean, received serial #%d.", this.invocationCount, serial);

                if (serial != ++expectedSerial) {
                    logger.warnf("Expected (%d) and received serial (%d) numbers do not match! Resetting.", expectedSerial, serial);
                    expectedSerial = serial;
                }
            } catch (Exception clientException) {
                logger.warnf("Jakarta Enterprise Beans client invocation #%d on bean, exception occurred %s", this.invocationCount, clientException);
                if (this.firstException == null) {
                    this.firstException = clientException;
                }
            }
        }
    }

    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=jgroups/channel=ee:write-attribute(name=cluster, value=ejb-forwarder)")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=binding-remote-ejb-connection:add(host=%s, port=8280)", TESTSUITE_NODE3)
                            // n.b. user/password is configured statically via testsuite/shared/src/main/resources/application-users.properties file.
                            .add("/subsystem=elytron/authentication-configuration=remote-ejb-configuration:add(authentication-name=remoteejbuser, security-domain=ApplicationDomain, realm=ApplicationRealm, forwarding-mode=authorization, credential-reference={clear-text=rem@teejbpasswd1})")
                            .add("/subsystem=elytron/authentication-context=remote-ejb-context:add(match-rules=[{authentication-configuration=remote-ejb-configuration, match-protocol=http-remoting}])")
                            .add("/subsystem=remoting/remote-outbound-connection=remote-ejb-connection:add(outbound-socket-binding-ref=binding-remote-ejb-connection, authentication-context=remote-ejb-context)")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=remoting/remote-outbound-connection=remote-ejb-connection:remove")
                            .add("/subsystem=elytron/authentication-configuration=remote-ejb-configuration:remove")
                            .add("/subsystem=elytron/authentication-context=remote-ejb-context:remove")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=binding-remote-ejb-connection:remove")
                            .add("/subsystem=jgroups/channel=ee:write-attribute(name=cluster, value=ejb)")
                            .endBatch()
                            .build())
                    .build());
        }
    }

}
