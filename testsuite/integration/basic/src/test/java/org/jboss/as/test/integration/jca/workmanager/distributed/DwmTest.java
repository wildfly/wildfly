/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.workmanager.distributed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_THREADS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.jca.rar.distributed.DistributedConnection1;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 * Tests distributed work manager and whether it really distributes work over multiple nodes. Test cases use two servers
 * both with a deployed resource adapter configured to use the DWM.
 *
 * Work is scheduled via a stateless ejb proxy. This allows us to schedule work from within the test, without the need
 * to marshall anything not serializable (such as the resource adapter).
 */
@ServerSetup(DwmTest.DwmServerSetupTask.class)
@RunWith(Arquillian.class)
@RunAsClient
public class DwmTest {

    private static Logger log = Logger.getLogger(DwmTest.class.getCanonicalName());

    private static final String DEFAULT_DWM_NAME = "newdwm";
    private static final String DEFAULT_CONTEXT_NAME = "customContext1";
    private static final ModelNode DEFAULT_DWM_ADDRESS = new ModelNode()
            .add(SUBSYSTEM, "jca")
            .add("distributed-workmanager", DEFAULT_DWM_NAME);
    private static final String DEPLOYMENT_0 =  "deployment-0";
    private static final String DEPLOYMENT_1 =  "deployment-1";
    private static final String CONTAINER_0 = "container-0";
    private static final String CONTAINER_1 = "container-1";
    private static final int SRT_MAX_THREADS = 10;
    private static final int SRT_QUEUE_LENGTH = 10;

    private enum Policy {
        ALWAYS,
        NEVER,
        WATERMARK
    }

    private enum Selector {
        FIRST_AVAILABLE,
        MAX_FREE_THREADS,
        PING_TIME
    }

    private static ModelControllerClient client1;
    private static ModelControllerClient client2;

    private DwmAdminObjectEjb server1Proxy;
    private DwmAdminObjectEjb server2Proxy;

    static {
        client1 = createClient1();
        try {
            client2 = createClient2();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    private static ModelControllerClient createClient2() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort() + 100,
                Authentication.getCallbackHandler());
    }

    @Deployment(name = DEPLOYMENT_0)
    @TargetsContainer(CONTAINER_0)
    public static Archive<?> deploy0 () {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_1)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deploy1 () {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        JavaArchive jar = createLibJar();
        ResourceAdapterArchive rar = createResourceAdapterArchive();
        JavaArchive ejbJar = createEjbJar();

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "dwmtest.ear");
        ear.addAsLibrary(jar).addAsModule(rar).addAsModule(ejbJar);
        ear.addAsManifestResource(DwmTest.class.getPackage(), "application.xml", "application.xml");

        return ear;
    }

    private static JavaArchive createLibJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        jar.addClass(DwmTest.class)
                .addClass(LongWork.class).addClass(ShortWork.class)
                .addPackage(DistributedConnection1.class.getPackage());
        jar.addAsManifestResource(new StringAsset("Dependencies: javax.inject.api,org.jboss.as.connector,"
                + "org.jboss.as.controller,org.jboss.dmr,org.jboss.as.cli,org.jboss.staxmapper,"
                + "org.jboss.ironjacamar.impl\n"), "MANIFEST.MF");
        return jar;
    }

    private static ResourceAdapterArchive createResourceAdapterArchive() {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "dwm.rar");
        rar.addAsManifestResource(DwmTest.class.getPackage(), "ra-distributed.xml", "ra.xml")
                .addAsManifestResource(DwmTest.class.getPackage(), "ironjacamar-distributed-1.xml",
                        "ironjacamar.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,org.jboss.as.connector \n"),
                        "MANIFEST.MF");
        return rar;
    }

    private static JavaArchive createEjbJar() {
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClass(DwmAdminObjectEjb.class).addClass(DwmAdminObjectEjbImpl.class);
        ejbJar.addAsManifestResource(DwmTest.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.ironjacamar.api"), "MANIFEST.MF");
        return ejbJar;
    }

    static class DwmServerSetupTask implements ServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            log.info("Setting up " + containerId);
            setUpServer(containerId);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            log.info("Tearing down " + containerId);
            tearDownServer(containerId);
        }

        private static void setUpServer(String containerId) throws Exception {
            ModelControllerClient mcc = CONTAINER_0.equals(containerId) ? client1 : client2;
            int serverPort = CONTAINER_0.equals(containerId) ? TestSuiteEnvironment.getServerPort() : TestSuiteEnvironment.getServerPort() + 100;

            ModelNode addBasicDwm = addBasicDwm();
            ModelNode setUpPolicy = setUpPolicy(Policy.ALWAYS);
            ModelNode setUpSelector = setUpSelector(Selector.MAX_FREE_THREADS);
            ModelNode setUpShortRunningThreads = setUpShortRunningThreads(SRT_MAX_THREADS, SRT_QUEUE_LENGTH);

            ModelNode compositeOp = ModelUtil.createCompositeNode(
                    new ModelNode[] {addBasicDwm, setUpPolicy, setUpSelector, setUpShortRunningThreads});
            ModelNode result = mcc.execute(compositeOp);
            log.info("Setting up Dwm: " + result);

            result = mcc.execute(setUpCustomContext());
            log.info("Setting up CustomContext: " + result);
        }

        private static void tearDownServer(String containerId) throws Exception {
            ModelControllerClient mcc = CONTAINER_0.equals(containerId) ? client1 : client2;
            int serverPort = CONTAINER_0.equals(containerId) ? TestSuiteEnvironment.getServerPort() : TestSuiteEnvironment.getServerPort() + 100;

            ModelNode removeDwm = new ModelNode();
            removeDwm.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS);
            removeDwm.get(OP).set(REMOVE);

            ModelNode removeContext = new ModelNode();
            removeContext.get(OP_ADDR).set((new ModelNode())
                    .add(SUBSYSTEM, "jca")
                    .add("bootstrap-context", DEFAULT_CONTEXT_NAME));
            removeContext.get(OP).set(REMOVE);

            ModelNode compositeOp = ModelUtil.createCompositeNode(
                    new ModelNode[] { removeDwm, removeContext });
            mcc.execute(compositeOp);
            ServerReload.executeReloadAndWaitForCompletion(mcc, 60000, false,
                    TestSuiteEnvironment.getServerAddress(), serverPort);
        }

        private static ModelNode addBasicDwm() {
            ModelNode setUpDwm = new ModelNode();

            setUpDwm.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS);
            setUpDwm.get(OP).set(ADD);
            setUpDwm.get(NAME).set(DEFAULT_DWM_NAME);

            return setUpDwm;
        }

        private static ModelNode setUpShortRunningThreads(int maxThreads, int queueLength) {
            ModelNode setUpSrt = new ModelNode();

            // the thread pool name must be the same as the DWM it belongs to
            setUpSrt.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS.clone().add("short-running-threads", DEFAULT_DWM_NAME));
            setUpSrt.get(OP).set(ADD);
            setUpSrt.get(MAX_THREADS).set(maxThreads);
            setUpSrt.get("queue-length").set(queueLength);

            return setUpSrt;
        }

        private static ModelNode setUpCustomContext() {
            ModelNode setUpCustomContext = new ModelNode();

            setUpCustomContext.get(OP_ADDR).set(new ModelNode()
                    .add(SUBSYSTEM, "jca")
                    .add("bootstrap-context", DEFAULT_CONTEXT_NAME));
            setUpCustomContext.get(OP).set(ADD);
            setUpCustomContext.get(NAME).set(DEFAULT_CONTEXT_NAME);
            setUpCustomContext.get("workmanager").set(DEFAULT_DWM_NAME);

            return setUpCustomContext;
        }
    }

    @Before
    public void setUpAdminObjects() throws NamingException {
        server1Proxy = lookupAdminObject(TestSuiteEnvironment.getServerAddress(), "8080");
        server2Proxy = lookupAdminObject(TestSuiteEnvironment.getServerAddress(), "8180");
        Assert.assertNotNull(server1Proxy);
        Assert.assertNotNull(server2Proxy);
    }

    /**
     * Executes a long work instances on a single node and verifies that it took enough time and executed on the
     * expected node.
     */
    @Test
    public void testDoWork() throws IOException, NamingException, WorkException, InterruptedException {
        log.info("Running testDoWork()");
        preparePolicyAndSelector(client1, Policy.NEVER, null);

        long startTime = System.currentTimeMillis();
        int doWorkAccepted = server1Proxy.getDoWorkAccepted();

        server1Proxy.doWork(new LongWork().setName("testDoWork-work1"));

        Assert.assertTrue("Expected time >=" + (startTime + LongWork.WORK_TIMEOUT) + ", actual: " + System.currentTimeMillis(),
                startTime + LongWork.WORK_TIMEOUT <= System.currentTimeMillis());

        logWorkStats();

        Assert.assertTrue("Expected doWorkAccepted = " + (doWorkAccepted + 1) + " but was: " + server1Proxy.getDoWorkAccepted(),
                server1Proxy.getDoWorkAccepted() == doWorkAccepted + 1);
    }

    /**
     * Submits a few (less than our max threads) long work instances and verifies that
     * {@link org.jboss.jca.core.api.workmanager.DistributedWorkManager#startWork(Work)} returns sooner than the time
     * needed for the work items to actually finish.
     */
    @Test
    public void testStartWork() throws IOException, NamingException, WorkException, InterruptedException {
        log.info("Running testStartWork()");
        preparePolicyAndSelector(client1, Policy.NEVER, null);

        long startTime = System.currentTimeMillis();
        int startWorkAccepted = server1Proxy.getStartWorkAccepted();

        server1Proxy.startWork(new LongWork().setName("testStartWork-work1"));

        Assert.assertTrue("Expected time <" + (startTime + LongWork.WORK_TIMEOUT) + ", actual: " + System.currentTimeMillis(),
                startTime + LongWork.WORK_TIMEOUT > System.currentTimeMillis());

        Thread.sleep(LongWork.WORK_TIMEOUT); // wait for the started work to finish, so it doesn't mess up our statistics for other tests
        logWorkStats();

        Assert.assertTrue("Expected startWorkAccepted = " + (startWorkAccepted + 1) + " but was: " + server1Proxy.getStartWorkAccepted(),
                server1Proxy.getStartWorkAccepted() == startWorkAccepted + 1);
    }

    /**
     * Submits more instances of long running work than we have threads in our thread pool and expects that
     * {@link org.jboss.jca.core.api.workmanager.DistributedWorkManager#scheduleWork(Work)} returns sooner than the time
     * needed for at least one of the work items to actually finish.
     *
     * This requires that all the work items have been processed and are now waiting for execution, but some may not
     * have been started yet.
     */
    @Test
    public void testScheduleWork() throws IOException, NamingException, WorkException, InterruptedException {
        log.info("Running testScheduleWork()");
        preparePolicyAndSelector(client1, Policy.NEVER, null);

        long startTime = System.currentTimeMillis();
        int schedulWorkAccepted = server1Proxy.getScheduleWorkAccepted();

        for (int i = 0; i < SRT_MAX_THREADS + 1; i++) {
            server1Proxy.scheduleWork(new LongWork().setName("testScheduleWork-work" + (i + 1)));
        }

        Assert.assertTrue("Expected time <" + (startTime + LongWork.WORK_TIMEOUT) + ", actual: " + System.currentTimeMillis(),
                startTime + LongWork.WORK_TIMEOUT > System.currentTimeMillis());

        Thread.sleep(LongWork.WORK_TIMEOUT * 2); // wait for the scheduled work to finish, so it doesn't mess up our statistics for other tests
        logWorkStats();

        Assert.assertTrue("Expected scheduleWorkAccepted = " + (schedulWorkAccepted + SRT_MAX_THREADS + 1) + " but was: " + server1Proxy.getScheduleWorkAccepted(),
                server1Proxy.getScheduleWorkAccepted() == schedulWorkAccepted + SRT_MAX_THREADS + 1);
    }

    /**
     * Does a few instances of short work with {@code policy = ALWAYS} and expects that they will be executed on a
     * remote node.
     */
    @Test
    public void testAlwaysPolicy() throws IOException, NamingException, WorkException {
        log.info("Running testAlwaysPolicy()");
        preparePolicyAndSelector(client1, Policy.ALWAYS, Selector.PING_TIME);
        preparePolicyAndSelector(client2, Policy.ALWAYS, Selector.PING_TIME);

        int doWorkAccepted = server2Proxy.getDoWorkAccepted();

        server1Proxy.doWork(new ShortWork().setName("testAlwaysPolicy-work1"));
        server1Proxy.doWork(new ShortWork().setName("testAlwaysPolicy-work2"));

        logWorkStats();

        Assert.assertTrue("Expected doWorkAccepted = " + (doWorkAccepted + 2) + ", actual: " + server2Proxy.getDoWorkAccepted(),
                server2Proxy.getDoWorkAccepted() == doWorkAccepted + 2);
    }

    /**
     * Runs two short running work instances and verifies that both run on the same node (be it local or remote).
     */
    @Test
    public void testWatermarkPolicyFirstAvailable() throws IOException, NamingException, WorkException {
        log.info("Running testWatermarkPolicyFirstAvailable()");
        preparePolicyAndSelector(client1, Policy.WATERMARK, Selector.FIRST_AVAILABLE);
        preparePolicyAndSelector(client2, Policy.WATERMARK, Selector.FIRST_AVAILABLE);

        int doWorkAccepted1 = server1Proxy.getDoWorkAccepted();
        int doWorkAccepted2 = server2Proxy.getDoWorkAccepted();

        server1Proxy.doWork(new ShortWork().setName("testWatermarkPolicyFirstAvailable-work1"));
        server1Proxy.doWork(new ShortWork().setName("testWatermarkPolicyFirstAvailable-work2"));

        logWorkStats();

        Assert.assertTrue("Expected both work instances to be executed on the same node",
                doWorkAccepted1 + 2 == server1Proxy.getDoWorkAccepted() ||
                doWorkAccepted2 + 2 == server2Proxy.getDoWorkAccepted());
    }

    /**
     * Runs two long running work instances and verifies that both run on different nodes.
     */
    @Test
    public void testWatermarkPolicyMaxFreeThreads() throws IOException, NamingException, WorkException, InterruptedException {
        log.info("Running testWatermarkPolicyMaxFreeThreads()");
        preparePolicyAndSelector(client1, Policy.WATERMARK, Selector.MAX_FREE_THREADS);
        preparePolicyAndSelector(client2, Policy.WATERMARK, Selector.MAX_FREE_THREADS);

        int startWorkAccepted1 = server1Proxy.getStartWorkAccepted();
        int startWorkAccepted2 = server2Proxy.getStartWorkAccepted();

        server1Proxy.startWork(new LongWork().setName("testWatermarkPolicyMaxFreeThreads-work1"));
        server1Proxy.startWork(new LongWork().setName("testWatermarkPolicyMaxFreeThreads-work2"));

        Thread.sleep(LongWork.WORK_TIMEOUT); // wait for the started work to finish, so it doesn't mess up our statistics for other tests
        logWorkStats();

        Assert.assertTrue("Expected both work instances to be executed on different nodes (expected/actual): "
                        + (startWorkAccepted1 + 1) + ":" + (startWorkAccepted2 + 1) + "/"
                        + server1Proxy.getStartWorkAccepted() + ":" + server2Proxy.getStartWorkAccepted(),
                startWorkAccepted1 + 1 == server1Proxy.getStartWorkAccepted() &&
                        startWorkAccepted2 + 1 == server2Proxy.getStartWorkAccepted());
    }

    /**
     * Executes two long running work instances and verifies that both run on the local node, because it has the best
     * ping time.
     *
     * Not sure if we can expect server one to have the best ping time since both servers run on the same node. If this
     * case creates problems, let's @Ignore it.
     */
    @Test
    public void testWatermarkPolicyPingTime() throws IOException, NamingException, WorkException {
        log.info("Running testWatermarkPolicyPingTime()");
        preparePolicyAndSelector(client1, Policy.WATERMARK, Selector.PING_TIME);
        preparePolicyAndSelector(client2, Policy.WATERMARK, Selector.PING_TIME);

        int startWorkAccepted1 = server1Proxy.getStartWorkAccepted();
        int startWorkAccepted2 = server2Proxy.getStartWorkAccepted();

        server1Proxy.startWork(new LongWork().setName("testWatermarkPolicyPingTime-work1"));
        server1Proxy.startWork(new LongWork().setName("testWatermarkPolicyPingTime-work2"));

        logWorkStats();

        Assert.assertTrue("Expected both work instances to be executed on the node where they're scheduled",
                startWorkAccepted1 + 2 == server1Proxy.getStartWorkAccepted() &&
                        startWorkAccepted2 == server2Proxy.getStartWorkAccepted());
    }

    @Test
    @InSequence(1)
    public void logFinalStats() {
        log.info("Running logFinalStats()");
        logWorkStats();
    }

    private static void preparePolicyAndSelector(ModelControllerClient mcc, Policy policy, Selector selector) throws IOException {
        if (policy != null) {
            ModelNode setUpPolicy = setUpPolicy(policy);
            ModelNode result = mcc.execute(setUpPolicy);
            Assert.assertTrue("Expected outcome is success, but was " + result.get("outcome"),
                    result.get("outcome").asString().equals("success"));
        }

        if (selector != null) {
            ModelNode setUpSelector = setUpSelector(selector);
            ModelNode result = mcc.execute(setUpSelector);
            Assert.assertTrue("Expected outcome is success, but was " + result.get("outcome"),
                    result.get("outcome").asString().equals("success"));
        }
    }

    private static ModelNode setUpPolicy(Policy policy) {
        ModelNode setUpPolicy = new ModelNode();

        setUpPolicy.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS);
        setUpPolicy.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        setUpPolicy.get(NAME).set("policy");
        setUpPolicy.get(VALUE).set(policy.toString());

        return setUpPolicy;
    }

    private static ModelNode setUpSelector(Selector selector) {
        ModelNode setUpSelector = new ModelNode();

        setUpSelector.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS);
        setUpSelector.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        setUpSelector.get(NAME).set("selector");
        setUpSelector.get(VALUE).set(selector.toString());

        return setUpSelector;
    }

    private DwmAdminObjectEjb lookupAdminObject(String address, String port) throws NamingException {
        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        properties.put(Context.PROVIDER_URL, String.format("%s%s:%s", "http-remoting://", address, port));
        Context context = new InitialContext(properties);

        String ejbExportedName = String.format("%s/%s/%s!%s", "dwm-ejb-application", "dwm-ejb-module",
                DwmAdminObjectEjbImpl.class.getSimpleName(), DwmAdminObjectEjb.class.getCanonicalName());
        return (DwmAdminObjectEjb) context.lookup(ejbExportedName);
    }

    private void logWorkStats() {
        log.info("doWorkAccepted (server 1): " + server1Proxy.getDoWorkAccepted());
        log.info("doWorkAccepted (server 2): " + server2Proxy.getDoWorkAccepted());
        log.info("doWorkRejected (server 1): " + server1Proxy.getDoWorkRejected());
        log.info("doWorkRejected (server 2): " + server2Proxy.getDoWorkRejected());

        log.info("startWorkAccepted (server 1): " + server1Proxy.getStartWorkAccepted());
        log.info("startWorkAccepted (server 2): " + server2Proxy.getStartWorkAccepted());
        log.info("startWorkRejected (server 1): " + server1Proxy.getStartWorkRejected());
        log.info("startWorkRejected (server 2): " + server2Proxy.getStartWorkRejected());

        log.info("scheduleWorkAccepted (server 1): " + server1Proxy.getScheduleWorkAccepted());
        log.info("scheduleWorkAccepted (server 2): " + server2Proxy.getScheduleWorkAccepted());
        log.info("scheduleWorkRejected (server 1): " + server1Proxy.getScheduleWorkRejected());
        log.info("scheduleWorkRejected (server 2): " + server2Proxy.getScheduleWorkRejected());
    }

    private ModelNode readAttribute(ModelControllerClient mcc, ModelNode address, String attributeName) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attributeName);
        op.get(OP_ADDR).set(address);
        return mcc.execute(op);
    }
}
