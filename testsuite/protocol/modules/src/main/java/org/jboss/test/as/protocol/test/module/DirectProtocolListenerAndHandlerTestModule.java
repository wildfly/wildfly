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

import java.net.InetAddress;
import java.util.Arrays;

import junit.framework.Assert;

import org.jboss.as.process.SystemExiter;
import org.jboss.as.server.DirectServerSideCommunicationHandler;
import org.jboss.as.server.manager.DirectServerManagerCommunicationHandler;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.test.as.protocol.support.process.NoopExiter;
import org.jboss.test.as.protocol.support.server.TestServerSideMessageHandler;
import org.jboss.test.as.protocol.support.server.manager.TestDirectServerManagerCommunicationListener;
import org.jboss.test.as.protocol.support.server.manager.TestServerManagerMessageHandler;
import org.jboss.test.as.protocol.support.server.manager.MockServerManagerProcess;
import org.jboss.test.as.protocol.test.base.DirectProtocolListenerAndHandlerTest;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DirectProtocolListenerAndHandlerTestModule implements DirectProtocolListenerAndHandlerTest{

    static {
        SystemExiter.initialize(new NoopExiter());
    }

    @Override
    public void testDirectProtocolListenerAndHandler() throws Exception{
        MockServerManagerProcess serverManager = MockServerManagerProcess.create();

        TestServerManagerMessageHandler managerMessageHandler = new TestServerManagerMessageHandler();
        TestDirectServerManagerCommunicationListener managerListener = TestDirectServerManagerCommunicationListener.create(serverManager, InetAddress.getLocalHost(), 0, 10, managerMessageHandler);

        TestServerSideMessageHandler serverMessageHandler = new TestServerSideMessageHandler();
        DirectServerSideCommunicationHandler serverHandler = DirectServerSideCommunicationHandler.create("Test", InetAddress.getLocalHost(), managerListener.getSmPort(), serverMessageHandler);

        byte[] sent = ServerManagerProtocolCommand.SERVER_AVAILABLE.createCommandBytes(null);
        serverHandler.sendMessage(sent);
        compare(sent, managerMessageHandler.awaitAndReadMessage().getMessage());

        DirectServerManagerCommunicationHandler managerHandler = managerListener.getManagerHandler("Test");
        sent = ServerManagerProtocolCommand.START_SERVER.createCommandBytes("ABCD".getBytes());
        managerHandler.sendMessage(sent);
        compare(sent, serverMessageHandler.awaitAndReadMessage());

        sent = ServerManagerProtocolCommand.SERVER_STARTED.createCommandBytes(null);
        serverHandler.sendMessage(sent);
        compare(sent, managerMessageHandler.awaitAndReadMessage().getMessage());

        sent = ServerManagerProtocolCommand.STOP_SERVER.createCommandBytes(null);
        managerHandler.sendMessage(sent);
        compare(sent, serverMessageHandler.awaitAndReadMessage());

        sent = ServerManagerProtocolCommand.SERVER_STOPPED.createCommandBytes(null);
        serverHandler.sendMessage(sent);
        compare(sent, managerMessageHandler.awaitAndReadMessage().getMessage());

        serverHandler.shutdown();

        waitForClose(serverHandler, managerHandler, 5000);
    }

    private void waitForClose(DirectServerSideCommunicationHandler serverHandler, DirectServerManagerCommunicationHandler managerHandler, int timeoutMs)
            throws InterruptedException {
        //Wait for close
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (managerHandler.isClosed() && serverHandler.isClosed())
                break;
            Thread.sleep(100);
        }
        Assert.assertTrue(managerHandler.isClosed());
        Assert.assertTrue(serverHandler.isClosed());
    }

    private void compare(byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            Assert.fail("Expected " + Arrays.toString(expected) + " actual " + Arrays.toString(actual));
        }
    }

    @Override
    public void afterTest() throws Exception {
    }

    @Override
    public void beforeTest() throws Exception {
    }
}
