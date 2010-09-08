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
package org.jboss.test.as.protocol.test.module;

import java.net.URISyntaxException;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.model.Standalone;
import org.jboss.as.server.manager.ServerManagerEnvironment;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.as.server.manager.ServerManagerProtocolUtils;
import org.jboss.test.as.protocol.support.process.MockManagedProcess;
import org.jboss.test.as.protocol.support.process.MockProcessManager;
import org.jboss.test.as.protocol.support.server.MockServerSideMessageHandler;
import org.jboss.test.as.protocol.support.server.manager.ServerManagerStarter;
import org.jboss.test.as.protocol.test.base.ServerManagerTest;

/**
 * Tests that the server manager part works in isolation with
 * the process manager and server processes mocked up
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerManagerTestModule extends AbstractProtocolTestModule implements ServerManagerTest {

    private void setDomainConfigDir(String name) throws URISyntaxException {
        addProperty(ServerManagerEnvironment.DOMAIN_CONFIG_DIR, findDomainConfigsDir(name));
    }

    @Override
    public void testStartServerManagerNoConfig() throws Exception {
        MockProcessManager pm = MockProcessManager.create(2);
        try {
            ServerManagerStarter.createServerManager(pm);
            Assert.fail("Expected failed start");
        } catch (Exception expected) {
        }
        pm.shutdown();
    }

    @Override
    public void testStartStopServerManager() throws Exception {
        MockProcessManager pm = MockProcessManager.create(2);
        setDomainConfigDir("standard");
        ServerManagerStarter.createServerManager(pm);
        pm.waitForServerManager();

        checkProcessManagerConnection(pm, "ServerManager", true);

        pm.waitForAddedProcesses();
        pm.waitForStartedProcesses();
        MockManagedProcess svr1 = pm.getProcess("Server:server-one");
        Assert.assertNotNull(svr1);
        MockManagedProcess svr2 = pm.getProcess("Server:server-two");
        Assert.assertNotNull(svr2);

        svr1.waitForStart();
        svr2.waitForStart();

        Assert.assertTrue(managerAlive(svr1.getSmAddress(), svr1.getSmPort()));

        Standalone cfg1 = readStartCommand(svr1.getMessageHandler());
        Assert.assertEquals("server-one", cfg1.getServerName());
        Standalone cfg2 = readStartCommand(svr2.getMessageHandler());
        Assert.assertEquals("server-two", cfg2.getServerName());

        svr1.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null));
        svr2.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null));

        pm.resetStopLatch(2);
        pm.resetRemoveLatch(2);

        /*On shutdown the proper PM will
         * 1)send SHUTDOWN_SERVERS to SM
         * 2)wait for the SERVERS_SHUTDOWN message from SM
         * 3)send SHUTDOWN to SM */

        //1)send SHUTDOWN_SERVERS to SM
        pm.getServerManager().sendShutdownServers();

        //Check the servers received the SHUTDOWN command from SM
        assertReadCommand(ServerManagerProtocolCommand.STOP_SERVER, svr1.getMessageHandler());
        assertReadCommand(ServerManagerProtocolCommand.STOP_SERVER, svr2.getMessageHandler());

        //Manually send the confirmation from the servers to SM
        svr1.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STOPPED.createCommandBytes(null));
        svr2.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STOPPED.createCommandBytes(null));

        //2)wait for the SERVERS_SHUTDOWN message from SM
        pm.waitForServerShutdown();
        pm.waitForStoppedProcesses();
        pm.waitForRemovedProcesses();

        //3)send SHUTDOWN to SM
        pm.resetStopLatch(1);
        pm.stopProcess("ServerManager");
        pm.waitForStoppedProcesses();

        waitForManagerToStop(svr1.getSmAddress(), svr1.getSmPort(), 5000);
        waitForProcessManagerConnectionToTerminate(pm, "ServerManager", 5000);
    }

    @Override
    public void testServerStartFailedAndGetsRespawned() throws Exception {
        MockProcessManager pm = MockProcessManager.create(2);
        setDomainConfigDir("standard");
        ServerManagerStarter.createServerManager(pm);
        pm.waitForServerManager();

        checkProcessManagerConnection(pm, "ServerManager", true);

        pm.waitForAddedProcesses();
        MockManagedProcess svr1 = pm.getProcess("Server:server-one");
        Assert.assertNotNull(svr1);
        MockManagedProcess svr2 = pm.getProcess("Server:server-two");
        Assert.assertNotNull(svr2);

        svr1.waitForStart();
        svr2.waitForStart();

        Standalone cfg1 = readStartCommand(svr1.getMessageHandler());
        Assert.assertEquals("server-one", cfg1.getServerName());
        Standalone cfg2 = readStartCommand(svr2.getMessageHandler());
        Assert.assertEquals("server-two", cfg2.getServerName());

        Assert.assertTrue(managerAlive(svr1.getSmAddress(), svr1.getSmPort()));

        svr1.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null));

        final int addCount = pm.getAddCount();
        final int removeCount = pm.getRemoveCount();
        int stopCount = pm.getStopCount();
        int startCount = pm.getStartCount();

        for (int i = 0 ; i <= 4 ; i++) {
            pm.resetStopLatch(1);
            pm.resetStartLatch(1);
            svr2.resetStartLatch();
            if (i < 4) {
                svr2.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_START_FAILED.createCommandBytes(null));
                pm.waitForStoppedProcesses();
                pm.waitForStartedProcesses();
                Assert.assertEquals(++stopCount, pm.getStopCount());
                Assert.assertEquals(++startCount, pm.getStartCount());
                svr2 = pm.getProcess("Server:server-two");
                svr2.waitForStart();
                cfg2 = readStartCommand(svr2.getMessageHandler());
                Assert.assertEquals("server-two", cfg2.getServerName());
            } else {
                svr2.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null));
            }
        }
        Assert.assertEquals(addCount, pm.getAddCount());
        Assert.assertEquals(removeCount, pm.getRemoveCount());
    }


    public void testServerStartFailedAndRespawnExpires() throws Exception {
        MockProcessManager pm = MockProcessManager.create(2);
        setDomainConfigDir("standard");
        ServerManagerStarter.createServerManager(pm);
        pm.waitForServerManager();

        checkProcessManagerConnection(pm, "ServerManager", true);

        pm.waitForAddedProcesses();
        MockManagedProcess svr1 = pm.getProcess("Server:server-one");
        Assert.assertNotNull(svr1);
        MockManagedProcess svr2 = pm.getProcess("Server:server-two");
        Assert.assertNotNull(svr2);

        svr1.waitForStart();
        svr2.waitForStart();

        Standalone cfg1 = readStartCommand(svr1.getMessageHandler());
        Assert.assertEquals("server-one", cfg1.getServerName());
        Standalone cfg2 = readStartCommand(svr2.getMessageHandler());
        Assert.assertEquals("server-two", cfg2.getServerName());

        Assert.assertTrue(managerAlive(svr1.getSmAddress(), svr1.getSmPort()));

        svr2.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null));

        final int addCount = pm.getAddCount();
        final int removeCount = pm.getRemoveCount();
        int stopCount = pm.getStopCount();
        int startCount = pm.getStartCount();


        //TODO JBAS-8390 once respawn policies are configurable we can make this a bit less time-consuming
        for (int i = 0 ; i <= 14 ; i++) {
            pm.resetStopLatch(1);
            pm.resetStartLatch(1);
            pm.resetRemoveLatch(1);
            svr1.resetStartLatch();
            svr1.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_START_FAILED.createCommandBytes(null));
            pm.waitForStoppedProcesses();
            Assert.assertEquals(++stopCount, pm.getStopCount());

            if (i < 14) {
                pm.waitForStartedProcesses();
                Assert.assertEquals(++startCount, pm.getStartCount());
                svr1 = pm.getProcess("Server:server-one");
                svr1.waitForStart();
                cfg1 = readStartCommand(svr1.getMessageHandler());
                Assert.assertEquals("server-one", cfg1.getServerName());
                Assert.assertEquals(addCount, pm.getAddCount());
                Assert.assertEquals(removeCount, pm.getRemoveCount());
            }
        }
        pm.waitForRemovedProcesses();
        Assert.assertEquals(removeCount + 1, pm.getRemoveCount());
    }

    @Override
    public void testServerCrashedAfterStartGetsRespawned() throws Exception {
        MockProcessManager pm = MockProcessManager.create(2);
        setDomainConfigDir("standard");
        ServerManagerStarter.createServerManager(pm);
        pm.waitForServerManager();

        checkProcessManagerConnection(pm, "ServerManager", true);

        pm.waitForAddedProcesses();
        MockManagedProcess svr1 = pm.getProcess("Server:server-one");
        Assert.assertNotNull(svr1);
        MockManagedProcess svr2 = pm.getProcess("Server:server-two");
        Assert.assertNotNull(svr2);

        svr1.waitForStart();
        svr2.waitForStart();

        Standalone cfg1 = readStartCommand(svr1.getMessageHandler());
        Assert.assertEquals("server-one", cfg1.getServerName());
        Standalone cfg2 = readStartCommand(svr2.getMessageHandler());
        Assert.assertEquals("server-two", cfg2.getServerName());

        Assert.assertTrue(managerAlive(svr1.getSmAddress(), svr1.getSmPort()));

        svr1.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null));

        final int addCount = pm.getAddCount();
        final int removeCount = pm.getRemoveCount();
        int stopCount = pm.getStopCount();
        int startCount = pm.getStartCount();

        MockManagedProcess serverManager = pm.getServerManager();

        for (int i = 0 ; i <= 4 ; i++) {
            pm.resetStopLatch(1);
            pm.resetStartLatch(1);
            if (i < 4) {
                svr2.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null));

                svr2.stop();
                serverManager.sendDown("Server:server-two");

                pm.waitForStoppedProcesses();
                pm.waitForStartedProcesses();
                Assert.assertEquals(++stopCount, pm.getStopCount());
                Assert.assertEquals(++startCount, pm.getStartCount());
                svr2 = pm.getProcess("Server:server-two");
                svr2.waitForStart();
                cfg2 = readStartCommand(svr2.getMessageHandler());
                Assert.assertEquals("server-two", cfg2.getServerName());
            } else {
                svr2.getCommunicationHandler().sendMessage(ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null));
            }
        }
        Assert.assertEquals(addCount, pm.getAddCount());
        Assert.assertEquals(removeCount, pm.getRemoveCount());
    }

    private ServerManagerProtocolCommand.Command assertReadCommand(ServerManagerProtocolCommand expected, MockServerSideMessageHandler handler) throws Exception {
        byte[] received = handler.awaitAndReadMessage();
        Assert.assertNotNull(received);
        ServerManagerProtocolCommand.Command cmd = ServerManagerProtocolCommand.readCommand(received);
        Assert.assertEquals(expected, cmd.getCommand());
        return cmd;
    }

    private Standalone readStartCommand(MockServerSideMessageHandler handler) throws Exception {
        ServerManagerProtocolCommand.Command cmd = assertReadCommand(ServerManagerProtocolCommand.START_SERVER, handler);
        Standalone cfg = ServerManagerProtocolUtils.unmarshallCommandData(Standalone.class, cmd);
        Assert.assertNotNull(cfg);
        return cfg;
    }

    private void checkProcessManagerConnection(MockProcessManager pm, String name, boolean alive) {
        MockManagedProcess proc = pm.getProcess(name);
        Assert.assertNotNull(proc);
        SocketConnection conn = proc.getPmConnection();
        Assert.assertEquals(alive, conn.isOpen());
    }

    private void waitForProcessManagerConnectionToTerminate(MockProcessManager pm, String name, int timeoutMs) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                checkProcessManagerConnection(pm, name, false);
                return;
            } catch (AssertionFailedError e) {
                Thread.sleep(100);
            }
        }
        checkProcessManagerConnection(pm, name, false);
    }
}
