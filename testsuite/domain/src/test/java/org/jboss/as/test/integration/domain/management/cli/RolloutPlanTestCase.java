/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.domain.management.cli;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.net.SocketPermission;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.RolloutPlanBuilder;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class RolloutPlanTestCase extends AbstractCliTestBase {

    private static File warFile;
    private static final int TEST_PORT = 8081;

    private static final String[] serverGroups = new String[] {"main-server-group", "other-server-group", "test-server-group"};

    @BeforeClass
    public static void before() throws Exception {

        //noinspection resource
        CLITestSuite.createSupport(RolloutPlanTestCase.class.getSimpleName());
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "RolloutPlanTestCase.war");
        war.addClass(RolloutPlanTestServlet.class);
        war.addAsManifestResource(createPermissionsXmlAsset(
                // RolloutPlanTestServlet binds a ServerSocket to the server's address and TEST_PORT
                // But, as described in the SocketPermission javadoc, a 'listen' SocketPermission should always
                // be for 'localhost', so that's what we grant.
                new SocketPermission("localhost:" + TEST_PORT, "listen"),           // main-one
                new SocketPermission("localhost:" + (TEST_PORT + 350), "listen")),  // main-three
                "permissions.xml");
        String tempDir = System.getProperty("java.io.tmpdir");
        warFile = new File(tempDir + File.separator + "RolloutPlanTestCase.war");
        new ZipExporterImpl(war).exportTo(warFile, true);

        AbstractCliTestBase.initCLI(DomainTestSupport.primaryAddress);

        // add another server group to default profile
        cli.sendLine("/server-group=test-server-group:add(profile=default,socket-binding-group=standard-sockets)");
        cli.sendLine("/server-group=test-server-group/jvm=default:add");
        // add a server to the group
        cli.sendLine("/host=primary/server-config=test-one:add(group=test-server-group,socket-binding-port-offset=700");
        cli.sendLine("/host=primary/server-config=test-one/interface=public:add(inet-address=" +
                CLITestSuite.hostAddresses.get("primary") + ")");
        CLITestSuite.addServer("test-one", "primary", "test-server-group","default", 700, true);

        // start main-two
        cli.sendLine("/host=primary/server-config=main-two:start(blocking=true)");
        CLIOpResult res = cli.readAllAsOpResult();
        Assert.assertTrue(res.isIsOutcomeSuccess());
        waitUntilState("main-two", "STARTED");

        // start test-one
        cli.sendLine("/host=primary/server-config=test-one:start(blocking=true)");
        res = cli.readAllAsOpResult();
        Assert.assertTrue(res.isIsOutcomeSuccess());
        waitUntilState("test-one", "STARTED");
    }

    @AfterClass
    public static void after() throws Exception {

        if (warFile.exists()){
            //noinspection ResultOfMethodCallIgnored
            warFile.delete();
        }

        // stop test-one
        cli.sendLine("/host=primary/server-config=test-one:stop(blocking=true)");
        CLIOpResult res = cli.readAllAsOpResult();
        Assert.assertTrue(res.isIsOutcomeSuccess());
        waitUntilState("test-one", "STOPPED");

        // stop main-two
        cli.sendLine("/host=primary/server-config=main-two:stop(blocking=true)");
        res = cli.readAllAsOpResult();
        Assert.assertTrue(res.isIsOutcomeSuccess());
        waitUntilState("main-two", "DISABLED");

        AbstractCliTestBase.closeCLI();

        CLITestSuite.stopSupport();
    }

    @After
    public void afterTest() {

        // undeploy helper servlets
        cli.sendLine("undeploy RolloutPlanTestCase.war --all-relevant-server-groups", true);

        // remove socket binding
        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test-binding:remove(){allow-resource-service-restart=true}", true);
    }

    @Test
    public void testInSeriesRolloutPlan() throws Exception {

        // create rollout plans

        // 1st plan
        RolloutPlanBuilder planBuilder = new RolloutPlanBuilder();
        planBuilder.addGroup(serverGroups[0], new RolloutPlanBuilder.RolloutPolicy(true, null, null));
        planBuilder.addGroup(serverGroups[1], new RolloutPlanBuilder.RolloutPolicy(true, null, null));
        planBuilder.addGroup(serverGroups[2], new RolloutPlanBuilder.RolloutPolicy(true, null, null));
        String rolloutPlan = planBuilder.buildAsString();
        cli.sendLine("rollout-plan add --name=testPlan --content=" + rolloutPlan);

        // 2nd with reversed order
        planBuilder = new RolloutPlanBuilder();
        planBuilder.addGroup(serverGroups[2], new RolloutPlanBuilder.RolloutPolicy(true, null, null));
        planBuilder.addGroup(serverGroups[1], new RolloutPlanBuilder.RolloutPolicy(true, null, null));
        planBuilder.addGroup(serverGroups[0], new RolloutPlanBuilder.RolloutPolicy(true, null, null));
        rolloutPlan = planBuilder.buildAsString();
        cli.sendLine("rollout-plan add --name=testPlan2 --content=" + rolloutPlan);

        // check they are listed
        cli.sendLine("cd /management-client-content=rollout-plans/rollout-plan");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        Assert.assertTrue(ls.contains("testPlan"));
        Assert.assertTrue(ls.contains("testPlan2"));

        // deploy using 1st prepared rollout plan
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --all-server-groups --headers={rollout id=testPlan}");

        // check that the apps were deployed in correct order
        // get application deployment times from servers
        long mainOneTime = Long.parseLong(checkURL("main-one"));
        long mainTwoTime = Long.parseLong(checkURL("main-two"));
        long mainThreeTime = Long.parseLong(checkURL("main-three"));
        long otherTwoTime = Long.parseLong(checkURL("other-two"));
        long testOneTime = Long.parseLong(checkURL("test-one"));

        Assert.assertTrue(mainOneTime < otherTwoTime);
        Assert.assertTrue(mainTwoTime < otherTwoTime);
        Assert.assertTrue(mainThreeTime < otherTwoTime);
        Assert.assertTrue(otherTwoTime < testOneTime);

        // undeploy apps
        cli.sendLine("undeploy RolloutPlanTestCase.war --all-relevant-server-groups");

        // deploy using 2nd plan
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --all-server-groups --headers={rollout id=testPlan2}");

        // check that the apps were deployed in reversed order
        mainOneTime = Long.parseLong(checkURL("main-one"));
        mainTwoTime = Long.parseLong(checkURL("main-two"));
        mainThreeTime = Long.parseLong(checkURL("main-three"));
        otherTwoTime = Long.parseLong(checkURL("other-two"));
        testOneTime = Long.parseLong(checkURL("test-one"));

        Assert.assertTrue(mainOneTime > otherTwoTime);
        Assert.assertTrue(mainTwoTime > otherTwoTime);
        Assert.assertTrue(mainThreeTime > otherTwoTime);
        Assert.assertTrue(otherTwoTime > testOneTime);

        // remove rollout plans
        cli.sendLine("rollout-plan remove --name=testPlan");
        cli.sendLine("rollout-plan remove --name=testPlan2");

        // check plans are no more listed
        cli.sendLine("cd /management-client-content=rollout-plans");
        cli.sendLine("ls");
        ls = cli.readOutput();
        Assert.assertFalse(ls.contains("testPlan"));
        Assert.assertFalse(ls.contains("testPlan2"));


    }
    /**
     * Tests rollout plan with non-zero maxFailedServers attribute.
     */
    @Test
    public void testMaxFailServersRolloutPlan() throws Exception {

        // deploy helper servlets
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --all-server-groups");

        checkURL("main-one", "/RolloutPlanTestCase/RolloutServlet");
        checkURL("main-two", "/RolloutPlanTestCase/RolloutServlet");
        checkURL("main-three", "/RolloutPlanTestCase/RolloutServlet");
        checkURL("test-one", "/RolloutPlanTestCase/RolloutServlet");

        // prepare socket binding
        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test-binding:add(interface=public,port=" + TEST_PORT + ")");


        // create plan with max fail server set to 1
        RolloutPlanBuilder planBuilder = new RolloutPlanBuilder();
        planBuilder.addGroup(serverGroups[0], new RolloutPlanBuilder.RolloutPolicy(true, null, 1));
        planBuilder.addGroup(serverGroups[1], new RolloutPlanBuilder.RolloutPolicy(true, null, 1));
        planBuilder.addGroup(serverGroups[2], new RolloutPlanBuilder.RolloutPolicy(true, null, 1));
        String rolloutPlan = planBuilder.buildAsString();
        cli.sendLine("rollout-plan add --name=maxFailOnePlan --content=" + rolloutPlan);

        // 1st scenario - main-one should fail, but the whole operation should succeed

        // let the helper server bind to test port to prevent successful subsequent add connector operation on main-one
        checkURL("main-one", "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" + TEST_PORT);
        CLIOpResult ret = testAddConnector("maxFailOnePlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertTrue(getServerStatus("main-two", ret));
        Assert.assertTrue(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));
        ret = testRemoveConnector("maxFailOnePlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertTrue(getServerStatus("main-two", ret));
        Assert.assertTrue(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));

        // 2nd scenario - main-one and main-three failures -> main-two should be rolled back but the operation succeed
        checkURL("main-three", "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" +
                (TEST_PORT + CLITestSuite.portOffsets.get("main-three")));
        ret = testAddConnector("maxFailOnePlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertFalse(getServerStatus("main-two", ret));
        Assert.assertFalse(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));

        testCleanupConnector("maxFailOnePlan");

        // remove rollout plan
        cli.sendLine("rollout-plan remove --name=maxFailOnePlan");
    }

    /**
     * Tests rollout plan with non-zero maxFailurePercentage attribute.
     */
    @Test
    public void testMaxFailServersPercentageRolloutPlan() throws Exception {

        // deploy helper servlets
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --all-server-groups");

        // prepare socket binding
        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test-binding:add(interface=public,port=" + TEST_PORT + ")");


        // create plan with max fail server percentage set to 40%
        RolloutPlanBuilder planBuilder = new RolloutPlanBuilder();
        planBuilder.addGroup(serverGroups[0], new RolloutPlanBuilder.RolloutPolicy(true, 40, 0));
        planBuilder.addGroup(serverGroups[1], new RolloutPlanBuilder.RolloutPolicy(true, 40, 0));
        planBuilder.addGroup(serverGroups[2], new RolloutPlanBuilder.RolloutPolicy(true, 40, 0));
        String rolloutPlan = planBuilder.buildAsString();
        cli.sendLine("rollout-plan add --name=maxFailPercPlan --content=" + rolloutPlan);

        // 1st scenario - server-one should fail, but the whole operation should succeed
        checkURL("main-one", "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" + TEST_PORT);
        CLIOpResult ret = testAddConnector("maxFailPercPlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertTrue(getServerStatus("main-two", ret));
        Assert.assertTrue(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));
        ret = testRemoveConnector("maxFailPercPlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertTrue(getServerStatus("main-two", ret));
        Assert.assertTrue(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));

        // 2nd scenario - main-one and main-three should fail -> main-two should be rolled back but the operation succeed
        checkURL("main-three", "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" +
                (TEST_PORT + CLITestSuite.portOffsets.get("main-three")));
        ret = testAddConnector("maxFailPercPlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertFalse(getServerStatus("main-two", ret));
        Assert.assertFalse(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));

        testCleanupConnector("maxFailPercPlan");

        // remove rollout plan
        cli.sendLine("rollout-plan remove --name=maxFailPercPlan");
    }

    /**
     * Tests rollout plan with RollbackAcrossGroups set to true.
     */
    @Test
    public void testRollbackAcrossGroupsRolloutPlan() throws Exception {
        // deploy helper servlets
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --all-server-groups");

        checkURL("main-one", "/RolloutPlanTestCase/RolloutServlet");
        checkURL("main-two", "/RolloutPlanTestCase/RolloutServlet");
        checkURL("main-three", "/RolloutPlanTestCase/RolloutServlet");
        checkURL("test-one", "/RolloutPlanTestCase/RolloutServlet");

        // prepare socket binding
        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test-binding:add(interface=public,port=" + TEST_PORT + ")");


        // create plan with max fail server set to 1
        RolloutPlanBuilder planBuilder = new RolloutPlanBuilder();
        planBuilder.addGroup(serverGroups[0], new RolloutPlanBuilder.RolloutPolicy(true, null, 1));
        planBuilder.addGroup(serverGroups[1], new RolloutPlanBuilder.RolloutPolicy(true, null, 1));
        planBuilder.addGroup(serverGroups[2], new RolloutPlanBuilder.RolloutPolicy(true, null, 1));
        planBuilder.setRollBackAcrossGroups(true);
        String rolloutPlan = planBuilder.buildAsString();
        cli.sendLine("rollout-plan add --name=groupsRollbackPlan --content=" + rolloutPlan);

        // let the main-one ane main-three fail, main two rollback and then test-one rollback

        // let the helper server bind to test port to prevent successful subsequent add connector operation on main-one
        checkURL("main-one", "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" + TEST_PORT);
        checkURL("main-three", "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" +
                (TEST_PORT + CLITestSuite.portOffsets.get("main-three")));
        CLIOpResult ret = testAddConnector("groupsRollbackPlan");
        Assert.assertFalse(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertFalse(getServerStatus("main-two", ret));
        Assert.assertFalse(getServerStatus("main-three", ret));
        Assert.assertFalse(getServerStatus("test-one", ret));

        // remove rollout plan
        cli.sendLine("rollout-plan remove --name=groupsRollbackPlan");
    }

    private CLIOpResult testAddConnector(String rolloutPlanId) throws Exception {
        cli.sendLine("/profile=default/subsystem=undertow/server=default-server/http-listener="+rolloutPlanId+":add" +
                "(socket-binding=test-binding)"
                + "{rollout id=" + rolloutPlanId + "}", true);
        return cli.readAllAsOpResult();
    }

    private CLIOpResult testRemoveConnector(String rolloutPlanId) throws Exception {
        cli.sendLine("/profile=default/subsystem=undertow/server=default-server/http-listener="+rolloutPlanId+":remove" +
                "{rollout id=" + rolloutPlanId + "; allow-resource-service-restart=true}");
        return cli.readAllAsOpResult();
    }

    private void testCleanupConnector(String rolloutPlanId) throws Exception {
        CLIOpResult ret = testRemoveConnector(rolloutPlanId);
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertTrue(getServerStatus("test-one", ret));
        boolean gotNoResponse = false;
        for (String server : new String[]{"main-one", "main-two", "main-three"}) {
            try {
                Assert.assertFalse(getServerStatus(server, ret));
            } catch (NoResponseException e) {
                if (gotNoResponse) {
                    throw e;
                }
                gotNoResponse = true;
            }
        }
        Assert.assertTrue("received no response from one server", gotNoResponse);

    }


    @SuppressWarnings({"rawtypes", "RedundantExplicitVariableType"})
    private boolean getServerStatus(String serverName, CLIOpResult result) throws Exception {
        Map  groups = (Map) result.getServerGroups();
        for (Object group : groups.values()) {
            Map hosts = (Map)((Map)group).get("host");
            if (hosts != null) {
                for (Object value : hosts.values()) {
                    Map serverResults = (Map)value;
                    Map serverResult = (Map)serverResults.get(serverName);
                    if (serverResult != null) {
                        Map serverResponse  = (Map)serverResult.get("response");
                        String serverOutcome = (String) serverResponse.get("outcome");
                        return "success".equals(serverOutcome);
                    }
                }
            }
        }
        throw new NoResponseException(serverName);
    }


    private static String checkURL(String server) throws Exception {
        return checkURL(server, "/RolloutPlanTestCase/RolloutServlet");
    }
    private static String checkURL(String server, String path) throws Exception {
        String address = CLITestSuite.hostAddresses.get(getServerHost(server));
        Integer portOffset = CLITestSuite.portOffsets.get(server);

        URL url = new URL("http", address, 8080 + portOffset, path);
        String response;
        try {
            response = HttpRequest.get(url.toString(), 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new Exception("Http request failed.", e);
        }
        return response;

    }

    private static String getServerHost(String server) {
        for(Entry<String, String[]> hostEntry : CLITestSuite.hostServers.entrySet()) {
            for (String hostServer : hostEntry.getValue()) if (hostServer.equals(server)) return hostEntry.getKey();
        }
        return null;
    }

    private static void waitUntilState(final String serverName, final String state) throws TimeoutException {
        final String serverHost = CLITestSuite.getServerHost(serverName);
        RetryTaskExecutor<Void> taskExecutor = new RetryTaskExecutor<>();
        taskExecutor.retryTask(new Callable<>() {
            public Void call() throws Exception {
                cli.sendLine("/host=" + serverHost + "/server-config=" + serverName + ":read-attribute(name=status)");
                CLIOpResult res = cli.readAllAsOpResult();
                if (! res.getResult().equals(state)) throw new Exception("Server not in state.");
                return null;
            }
        });

    }

    private static class NoResponseException extends Exception {
        private static final long serialVersionUID = 1L;

        private NoResponseException(String serverName) {
            super("Status of the server " + serverName + " not found in operation result.");
        }
    }

}
