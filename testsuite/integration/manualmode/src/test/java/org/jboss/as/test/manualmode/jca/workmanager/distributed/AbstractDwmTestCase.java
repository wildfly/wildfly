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
package org.jboss.as.test.manualmode.jca.workmanager.distributed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_THREADS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;
import static org.junit.Assert.assertTrue;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.manualmode.jca.workmanager.distributed.ra.DistributedConnection1;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.wildfly.test.api.Authentication;

/**
 * Tests distributed work manager and whether it really distributes work over multiple nodes. Test cases use two servers
 * both with a deployed resource adapter configured to use the DWM.
 *
 * Work is scheduled via a stateless ejb proxy. This allows us to schedule work from within the test, without the need
 * to marshall anything not serializable (such as the resource adapter).
 */
public abstract class AbstractDwmTestCase {

    private static Logger log = Logger.getLogger(AbstractDwmTestCase.class.getCanonicalName());

    private static final String DEFAULT_DWM_NAME = "newdwm";
    private static final String DEFAULT_CONTEXT_NAME = "customContext1";
    private static final ModelNode DEFAULT_DWM_ADDRESS = new ModelNode()
            .add(SUBSYSTEM, "jca")
            .add("distributed-workmanager", DEFAULT_DWM_NAME);
    private static final String DEPLOYMENT_0 =  "deployment-0";
    private static final String DEPLOYMENT_1 =  "deployment-1";
    private static final String CONTAINER_0 = "dwm-container-manual-0";
    private static final String CONTAINER_1 = "dwm-container-manual-1";

    // can be used in extending test cases to control test logic
    protected static final int SRT_MAX_THREADS = 1;
    protected static final int SRT_QUEUE_LENGTH = 1;
    protected static final int WORK_FINISH_MAX_TIMEOUT = 10_000;

    protected enum Policy {
        ALWAYS,
        NEVER,
        WATERMARK
    }

    protected enum Selector {
        FIRST_AVAILABLE,
        MAX_FREE_THREADS,
        PING_TIME
    }

    private String snapshotForServer1;
    private String snapshotForServer2;

    protected DwmAdminObjectEjb server1Proxy;
    protected DwmAdminObjectEjb server2Proxy;

    @ArquillianResource
    protected static ContainerController containerController;

    @ArquillianResource
    protected static Deployer deployer;

    // --------------------------------------------------------------------------------
    // infrastructure preparation
    // --------------------------------------------------------------------------------

    private static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    private static ModelControllerClient createClient2() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddressNode1()),
                TestSuiteEnvironment.getServerPort() + 100,
                Authentication.getCallbackHandler());
    }

    private static String takeSnapshot(ModelControllerClient client) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("take-snapshot");
        ModelNode result = execute(client, operation);
        log.info("Snapshot of current configuration taken: " + result.asString());
        return result.asString();
    }

    private static ModelNode execute(ModelControllerClient client, ModelNode operation) throws Exception {
        ModelNode response = client.execute(operation);
        boolean success = SUCCESS.equals(response.get(OUTCOME).asString());
        if (success) {
            return response.get(RESULT);
        }
        throw new Exception("Operation failed");
    }

    private void restoreSnapshot(String snapshot) {
        File snapshotFile = new File(snapshot);
        File configurationDir = snapshotFile.getParentFile().getParentFile().getParentFile();
        File standaloneConfiguration = new File(configurationDir, "standalone-ha.xml");
        if (standaloneConfiguration.exists()) {
            if (!standaloneConfiguration.delete()) {
                log.warn("Could not delete file " + standaloneConfiguration.getAbsolutePath());
            }
        }
        if (!snapshotFile.renameTo(standaloneConfiguration)) {
            log.warn("File " + snapshotFile.getAbsolutePath() + " could not be renamed to " + standaloneConfiguration.getAbsolutePath());
        }
    }

    private void executeReloadAndWaitForCompletionOfServer1(ModelControllerClient initialClient, boolean adminOnly) throws Exception {
        executeReloadAndWaitForCompletion(initialClient, adminOnly);
    }

    private void executeReloadAndWaitForCompletionOfServer2(ModelControllerClient initialClient, boolean adminOnly) throws Exception {
        executeReloadAndWaitForCompletion(initialClient, ServerReload.TIMEOUT,
                adminOnly,
                TestSuiteEnvironment.getServerAddressNode1(),
                TestSuiteEnvironment.getServerPort() + 100);
    }

    // --------------------------------------------------------------------------------
    // server set up and tear down
    // --------------------------------------------------------------------------------

    @Before
    public void setUp() throws Exception {

        // start server1 and reload it in admin-only
        containerController.start(CONTAINER_0);
        ModelControllerClient client1 = createClient1();
        snapshotForServer1 = takeSnapshot(client1);
        executeReloadAndWaitForCompletionOfServer1(client1, true);

        // start server2 and reload it in admin-only
        containerController.start(CONTAINER_1);
        ModelControllerClient client2 = createClient2();
        snapshotForServer2 = takeSnapshot(client2);
        executeReloadAndWaitForCompletionOfServer2(client2, true);

        // setup both servers
        try {
            setUpServer(client1, CONTAINER_0);
            setUpServer(client2, CONTAINER_1);
        } catch (Exception e) {
            client1.close();
            client2.close();
            tearDown();
            throw e;
        }

        // reload servers in normal mode
        executeReloadAndWaitForCompletionOfServer1(client1, false);
        executeReloadAndWaitForCompletionOfServer2(client2, false);

        // both servers are started and configured
        assertTrue(containerController.isStarted(CONTAINER_0));
        assertTrue(containerController.isStarted(CONTAINER_1));
        client1.close();
        client2.close();

        // now deploy the ejbs
        deployer.deploy(DEPLOYMENT_0);
        deployer.deploy(DEPLOYMENT_1);

        // set up ejb proxies
        try {
            server1Proxy = lookupAdminObject(TestSuiteEnvironment.getServerAddress(), "8080");
            server2Proxy = lookupAdminObject(TestSuiteEnvironment.getServerAddressNode1(), "8180");
            Assert.assertNotNull(server1Proxy);
            Assert.assertNotNull(server2Proxy);
        } catch (Throwable e) {
            tearDown();
            throw e;
        }
    }

    @After
    public void tearDown() {
        server1Proxy = null;
        server2Proxy = null;

        deployer.undeploy(DEPLOYMENT_0);
        deployer.undeploy(DEPLOYMENT_1);

        if (containerController.isStarted(CONTAINER_0)) {
            containerController.stop(CONTAINER_0);
        }
        restoreSnapshot(snapshotForServer1);
        if (containerController.isStarted(CONTAINER_1)) {
            containerController.stop(CONTAINER_1);
        }
        restoreSnapshot(snapshotForServer2);
    }

    // --------------------------------------------------------------------------------
    // set up helper methods
    // --------------------------------------------------------------------------------

    private ModelNode addBasicDwm() {
        ModelNode setUpDwm = new ModelNode();

        setUpDwm.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS);
        setUpDwm.get(OP).set(ADD);
        setUpDwm.get(NAME).set(DEFAULT_DWM_NAME);

        return setUpDwm;
    }

    private ModelNode setUpShortRunningThreads(int maxThreads, int queueLength) {
        ModelNode setUpSrt = new ModelNode();

        // the thread pool name must be the same as the DWM it belongs to
        setUpSrt.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS.clone().add("short-running-threads", DEFAULT_DWM_NAME));
        setUpSrt.get(OP).set(ADD);
        setUpSrt.get(MAX_THREADS).set(maxThreads);
        setUpSrt.get("queue-length").set(queueLength);

        return setUpSrt;
    }

    private ModelNode setUpCustomContext() {
        ModelNode setUpCustomContext = new ModelNode();

        setUpCustomContext.get(OP_ADDR).set(new ModelNode()
                .add(SUBSYSTEM, "jca")
                .add("bootstrap-context", DEFAULT_CONTEXT_NAME));
        setUpCustomContext.get(OP).set(ADD);
        setUpCustomContext.get(NAME).set(DEFAULT_CONTEXT_NAME);
        setUpCustomContext.get("workmanager").set(DEFAULT_DWM_NAME);

        return setUpCustomContext;
    }

    private ModelNode setUpPolicy(Policy policy) {
        ModelNode setUpPolicy = new ModelNode();

        setUpPolicy.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS);
        setUpPolicy.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        setUpPolicy.get(NAME).set("policy");
        setUpPolicy.get(VALUE).set(policy.toString());

        return setUpPolicy;
    }

    private ModelNode setUpWatermarkPolicyOption(int waterMarkPolicyOption) {
        ModelNode setUpWatermarkPolicyOption = new ModelNode();

        setUpWatermarkPolicyOption.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS);
        setUpWatermarkPolicyOption.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        setUpWatermarkPolicyOption.get(NAME).set("policy-options");
        setUpWatermarkPolicyOption.get(VALUE).set(new ModelNode().add("watermark", waterMarkPolicyOption));

        return setUpWatermarkPolicyOption;
    }

    private ModelNode setUpSelector(Selector selector) {
        ModelNode setUpSelector = new ModelNode();

        setUpSelector.get(OP_ADDR).set(DEFAULT_DWM_ADDRESS);
        setUpSelector.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        setUpSelector.get(NAME).set("selector");
        setUpSelector.get(VALUE).set(selector.toString());

        return setUpSelector;
    }

    @SuppressWarnings("unused")
    private void setUpServer(ModelControllerClient client ,String containerId) throws IOException {
        ModelControllerClient mcc = CONTAINER_0.equals(containerId) ? createClient1() : createClient2();

        log.info("Setting up Policy/Selector: " + getPolicy() + "/" + getSelector() + " on server " + containerId);
        ModelNode addBasicDwm = addBasicDwm();
        ModelNode setUpPolicy = setUpPolicy(getPolicy());
        ModelNode setUpPolicyOptions = setUpWatermarkPolicyOption(getWatermarkPolicyOption());
        ModelNode setUpSelector = setUpSelector(getSelector());
        ModelNode setUpShortRunningThreads = setUpShortRunningThreads(getSrtMaxThreads(), getSrtQueueLength());

        List<ModelNode> operationList = new ArrayList<>(Arrays.asList(addBasicDwm, setUpPolicy, setUpSelector, setUpShortRunningThreads));
        if (getPolicy().equals(Policy.WATERMARK)) {
            operationList.add(setUpPolicyOptions);
        }
        ModelNode compositeOp = ModelUtil.createCompositeNode(operationList.toArray(new ModelNode[1]));
        ModelNode result = mcc.execute(compositeOp);
        log.info("Setting up DWM on server " + containerId + ": " + result);

        result = mcc.execute(setUpCustomContext());
        log.info("Setting up CustomContext on server " + containerId + ": " + result);

        mcc.close();
    }

    // --------------------------------------------------------------------------------
    // abstract and other to-be-overwritten methods
    // --------------------------------------------------------------------------------

    protected abstract Policy getPolicy();
    protected abstract Selector getSelector();
    protected int getWatermarkPolicyOption() {
        return 0;
    }
    protected int getSrtMaxThreads() {
        return SRT_MAX_THREADS;
    }
    protected int getSrtQueueLength() {
        return SRT_QUEUE_LENGTH;
    }

    // --------------------------------------------------------------------------------
    // deployment setup
    // --------------------------------------------------------------------------------

    @Deployment(name = DEPLOYMENT_0, managed = false, testable = false)
    @TargetsContainer(CONTAINER_0)
    public static Archive<?> deploy0 () {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
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
        ear.addAsManifestResource(AbstractDwmTestCase.class.getPackage(), "application.xml", "application.xml");

        return ear;
    }

    private static JavaArchive createLibJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        jar.addClass(LongWork.class).addClass(ShortWork.class)
                .addPackage(DistributedConnection1.class.getPackage());
        jar.addAsManifestResource(new StringAsset("Dependencies: javax.inject.api,org.jboss.as.connector,"
                + "org.jboss.as.controller,org.jboss.dmr,org.jboss.as.cli,org.jboss.staxmapper,"
                + "org.jboss.ironjacamar.impl\n"), "MANIFEST.MF");
        return jar;
    }

    private static ResourceAdapterArchive createResourceAdapterArchive() {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "dwm.rar");
        rar.addAsManifestResource(AbstractDwmTestCase.class.getPackage(), "ra-distributed.xml", "ra.xml")
                .addAsManifestResource(AbstractDwmTestCase.class.getPackage(), "ironjacamar-distributed-1.xml",
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
        ejbJar.addAsManifestResource(AbstractDwmTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.ironjacamar.api"), "MANIFEST.MF");
        return ejbJar;
    }

    // --------------------------------------------------------------------------------
    // test related abstractions
    // --------------------------------------------------------------------------------

    private DwmAdminObjectEjb lookupAdminObject(String address, String port) throws NamingException {
        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        properties.put(Context.PROVIDER_URL, String.format("%s%s:%s", "http-remoting://", address, port));
        Context context = new InitialContext(properties);

        String ejbExportedName = String.format("%s/%s/%s!%s", "dwm-ejb-application", "dwm-ejb-module",
                DwmAdminObjectEjbImpl.class.getSimpleName(), DwmAdminObjectEjb.class.getCanonicalName());
        return (DwmAdminObjectEjb) context.lookup(ejbExportedName);
    }

    private enum WorkScheduleType {
        DO_WORK,
        START_WORK,
        SCHEDULE_WORK
    }

    protected boolean waitForDoWork(DwmAdminObjectEjb proxy, int expectedWorkAmount, int timeout) throws InterruptedException {
        return waitForWork(proxy, expectedWorkAmount, timeout, WorkScheduleType.DO_WORK);
    }

    protected boolean waitForStartWork(DwmAdminObjectEjb proxy, int expectedWorkAmount, int timeout) throws InterruptedException {
        return waitForWork(proxy, expectedWorkAmount, timeout, WorkScheduleType.START_WORK);
    }

    protected boolean waitForScheduleWork(DwmAdminObjectEjb proxy, int expectedWorkAmount, int timeout) throws InterruptedException {
        return waitForWork(proxy, expectedWorkAmount, timeout, WorkScheduleType.SCHEDULE_WORK);
    }

    private boolean waitForWork(DwmAdminObjectEjb proxy, int expectedWorkAmount, int timeout, WorkScheduleType wst) throws InterruptedException {
        long finish = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() <= finish) {
            if (getCurrentWorkAmount(proxy, wst) == expectedWorkAmount) {
                return true;
            }
            Thread.sleep(100L);
        }

        return getCurrentWorkAmount(proxy, wst) == expectedWorkAmount;
    }

    private int getCurrentWorkAmount(DwmAdminObjectEjb proxy, WorkScheduleType wst) {
        switch (wst) {
            case DO_WORK:
                return proxy.getDoWorkAccepted();
            case START_WORK:
                return proxy.getStartWorkAccepted();
            case SCHEDULE_WORK:
                return proxy.getScheduleWorkAccepted();
            default:
                throw new IllegalStateException("Bad work schedule type - test error");
        }
    }
}
