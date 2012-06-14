/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.domain.management.cli;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.RolloutPlanBuilder;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class RolloutPlanTestCase extends AbstractCliTestBase {

    private static WebArchive war;
    private static File warFile;
    private static final int TEST_PORT = 8081;
    
    private static final String[] serverGroups = new String[] {"main-server-group", "other-server-group", "test-server-group"};

    @BeforeClass
    public static void before() throws Exception {
        war = ShrinkWrap.create(WebArchive.class, "RolloutPlanTestCase.war");
        war.addClass(RolloutPlanTestServlet.class);
        String tempDir = System.getProperty("java.io.tmpdir");
        warFile = new File(tempDir + File.separator + "RolloutPlanTestCase.war");
        new ZipExporterImpl(war).exportTo(warFile, true);
        war = ShrinkWrap.create(WebArchive.class, "RolloutPlanTestCase.war");

        AbstractCliTestBase.initCLI(DomainTestSupport.masterAddress);

        // add another server group to default profile
        cli.sendLine("/server-group=test-server-group:add(profile=default,socket-binding-group=standard-sockets)");
        // add a server to the group
        cli.sendLine("/host=master/server-config=test-one:add(group=test-server-group,socket-binding-port-offset=700");
        cli.sendLine("/host=master/server-config=test-one/interface=public:add(inet-address=" +
                CLITestSuite.hostAddresses.get("master") + ")");
        CLITestSuite.addServer("test-one", "master", "test-server-group","default", 700, true);

        // start main-two
        cli.sendLine("/host=master/server-config=main-two:start");
        CLIOpResult res = cli.readAllAsOpResult();
        Assert.assertTrue(res.isIsOutcomeSuccess());
        waitUntilState("main-two", "STARTED");

        // start test-one
        cli.sendLine("/host=master/server-config=test-one:start");
        res = cli.readAllAsOpResult();
        Assert.assertTrue(res.isIsOutcomeSuccess());
        waitUntilState("test-one", "STARTED");
    }

    @AfterClass
    public static void after() throws Exception {

        // stop test-one
        cli.sendLine("/host=master/server-config=test-one:stop");
        CLIOpResult res = cli.readAllAsOpResult();
        Assert.assertTrue(res.isIsOutcomeSuccess());
        waitUntilState("test-one", "STOPPED");

        // stop main-two
        cli.sendLine("/host=master/server-config=main-two:stop");
        res = cli.readAllAsOpResult();
        Assert.assertTrue(res.isIsOutcomeSuccess());
        waitUntilState("main-two", "DISABLED");

        AbstractCliTestBase.closeCLI();
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
        long mainOneTime = Long.valueOf(checkURL("main-one", false));
        long mainTwoTime = Long.valueOf(checkURL("main-two", false));
        long mainThreeTime = Long.valueOf(checkURL("main-three", false));
        long otherTwoTime = Long.valueOf(checkURL("other-two", false));
        long testOneTime = Long.valueOf(checkURL("test-one", false));

        Assert.assertTrue(mainOneTime < otherTwoTime);
        Assert.assertTrue(mainTwoTime < otherTwoTime);
        Assert.assertTrue(mainThreeTime < otherTwoTime);
        Assert.assertTrue(otherTwoTime < testOneTime);

        // undeploy apps
        cli.sendLine("undeploy RolloutPlanTestCase.war --all-relevant-server-groups");

        // deploy using 2nd plan
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --all-server-groups --headers={rollout id=testPlan2}");

        // check that the apps were deployed in reversed order
        mainOneTime = Long.valueOf(checkURL("main-one", false));
        mainTwoTime = Long.valueOf(checkURL("main-two", false));
        mainThreeTime = Long.valueOf(checkURL("main-three", false));
        otherTwoTime = Long.valueOf(checkURL("other-two", false));
        testOneTime = Long.valueOf(checkURL("test-one", false));

        Assert.assertTrue(mainOneTime > otherTwoTime);
        Assert.assertTrue(mainTwoTime > otherTwoTime);
        Assert.assertTrue(mainThreeTime > otherTwoTime);
        Assert.assertTrue(otherTwoTime > testOneTime);

        // undeploy apps
        cli.sendLine("undeploy RolloutPlanTestCase.war --all-relevant-server-groups");

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

        checkURL("main-one", false, "/RolloutPlanTestCase/RolloutServlet");
        checkURL("main-two", false, "/RolloutPlanTestCase/RolloutServlet");
        checkURL("main-three", false, "/RolloutPlanTestCase/RolloutServlet");
        checkURL("test-one", false, "/RolloutPlanTestCase/RolloutServlet");

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
        checkURL("main-one", false, "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" + TEST_PORT);
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
        checkURL("main-three", false, "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" +
                String.valueOf(TEST_PORT + CLITestSuite.portOffsets.get("main-three")));
        ret = testAddConnector("maxFailOnePlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertFalse(getServerStatus("main-two", ret));
        Assert.assertFalse(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));
        ret = testRemoveConnector("maxFailOnePlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-two", ret));
        Assert.assertFalse(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));

        // undeploy helper servlets
        cli.sendLine("undeploy RolloutPlanTestCase.war --all-relevant-server-groups");

        // remove socket binding
        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test-binding:remove");

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
        checkURL("main-one", false, "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" + TEST_PORT);
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
        checkURL("main-three", false, "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" +
                String.valueOf(TEST_PORT + CLITestSuite.portOffsets.get("main-three")));
        ret = testAddConnector("maxFailPercPlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertFalse(getServerStatus("main-two", ret));
        Assert.assertFalse(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));
        ret = testRemoveConnector("maxFailOnePlan");
        Assert.assertTrue(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-two", ret));
        Assert.assertFalse(getServerStatus("main-three", ret));
        Assert.assertTrue(getServerStatus("test-one", ret));

        // undeploy helper servlets
        cli.sendLine("undeploy RolloutPlanTestCase.war --all-relevant-server-groups");

        // remove socket binding
        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test-binding:remove");
    }

    /**
     * Tests rollout plan with RollbackAcrossGroups set to true.
     */
    @Test
    public void testRollbackAcrossGroupsRolloutPlan() throws Exception {
        // deploy helper servlets
        cli.sendLine("deploy " + warFile.getAbsolutePath() + " --all-server-groups");

        checkURL("main-one", false, "/RolloutPlanTestCase/RolloutServlet");
        checkURL("main-two", false, "/RolloutPlanTestCase/RolloutServlet");
        checkURL("main-three", false, "/RolloutPlanTestCase/RolloutServlet");
        checkURL("test-one", false, "/RolloutPlanTestCase/RolloutServlet");

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
        checkURL("main-one", false, "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" + TEST_PORT);
        checkURL("main-three", false, "/RolloutPlanTestCase/RolloutServlet?operation=bind&bindPort=" +
                String.valueOf(TEST_PORT + CLITestSuite.portOffsets.get("main-three")));
        CLIOpResult ret = testAddConnector("groupsRollbackPlan");
        Assert.assertFalse(ret.isIsOutcomeSuccess());
        Assert.assertFalse(getServerStatus("main-one", ret));
        Assert.assertFalse(getServerStatus("main-two", ret));
        Assert.assertFalse(getServerStatus("main-three", ret));
        Assert.assertFalse(getServerStatus("test-one", ret));

        // undeploy helper servlets
        cli.sendLine("undeploy RolloutPlanTestCase.war --all-relevant-server-groups");

        // remove socket binding
        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test-binding:remove");
    }

    private CLIOpResult testAddConnector(String rolloutPlanId) throws Exception {
        cli.sendLine("/profile=default/subsystem=web/connector=test-http:add" +
                "(socket-binding=test-binding, scheme=http, protocol=\"HTTP/1.1\", enabled=true)"
                + "{rollout id=" + rolloutPlanId + "}", true);
        return cli.readAllAsOpResult();
    }

    private CLIOpResult testRemoveConnector(String rolloutPlanId) throws Exception {
        cli.sendLine("/profile=default/subsystem=web/connector=test-http:remove" +
                "{rollout id=" + rolloutPlanId + "}");
        return cli.readAllAsOpResult();
    }


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
        throw new Exception("Status of the server " + serverName + " not found in operation result.");
    }


    private static String checkURL(String server, boolean shouldFail) throws Exception {
        return checkURL(server, shouldFail, "/RolloutPlanTestCase/RolloutServlet");
    }
    private static String checkURL(String server, boolean shouldFail, String path) throws Exception {
        String address = CLITestSuite.hostAddresses.get(getServerHost(server));
        Integer portOffset = CLITestSuite.portOffsets.get(server);

        URL url = new URL("http", address, 8080 + portOffset, path);
        boolean failed = false;
        String response = null;
        try {
            response = HttpRequest.get(url.toString(), 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            failed = true;
            if (!shouldFail) throw new Exception("Http request failed.", e);
        }
        if (shouldFail) Assert.assertTrue(failed);
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
        RetryTaskExecutor taskExecutor = new RetryTaskExecutor();
        taskExecutor.retryTask(new Callable() {
            public Object call() throws Exception {
                cli.sendLine("/host=" + serverHost + "/server-config=" + serverName + ":read-attribute(name=status)");
                CLIOpResult res = cli.readAllAsOpResult();
                if (! res.getResult().equals(state)) throw new Exception("Server not in state.");
                return null;
            }
        });

    }

}
