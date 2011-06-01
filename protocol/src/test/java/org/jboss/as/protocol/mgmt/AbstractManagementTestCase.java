/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.protocol.mgmt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.as.protocol.mgmt.support.ConcurrentRequestOperationHandler;
import org.jboss.as.protocol.mgmt.support.RemotingChannelPairSetup;
import org.jboss.as.protocol.mgmt.support.SimpleHandlers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractManagementTestCase {

    RemotingChannelPairSetup channels;
    @Before
    public void start() throws Exception {
        channels = createSetup();
        channels.setupRemoting();
        channels.startChannels();
    }

    @After
    public void stop() throws Exception {
        channels.stopChannels();
        channels.shutdownRemoting();
    }

    @Test
    public void testSimpleRequest() throws Exception {
        ProtocolChannel channel = channels.getServerChannel();
        channels.getClientChannel().startReceiving();
        channel.getReceiver(ManagementChannelReceiver.class).setOperationHandler(new SimpleHandlers.OperationHandler());
        SimpleHandlers.Request request = new SimpleHandlers.Request(600);
        Assert.assertEquals(Integer.valueOf(1200), request.executeForResult(channels.getExecutorService(), ManagementClientChannelStrategy.create(channels.getClientChannel())));
    }

    @Test
    public void testTwoSimpleRequests() throws Exception {
        ProtocolChannel channel = channels.getServerChannel();
        channels.getClientChannel().startReceiving();
        channel.getReceiver(ManagementChannelReceiver.class).setOperationHandler(new SimpleHandlers.OperationHandler());
        SimpleHandlers.Request request = new SimpleHandlers.Request(600);
        Assert.assertEquals(Integer.valueOf(1200), request.executeForResult(channels.getExecutorService(), ManagementClientChannelStrategy.create(channels.getClientChannel())));

        request = new SimpleHandlers.Request(700);
        Assert.assertEquals(Integer.valueOf(1400), request.executeForResult(channels.getExecutorService(), ManagementClientChannelStrategy.create(channels.getClientChannel())));
    }

    @Test
    public void testSeveralConcurrentSimpleRequests() throws Exception {
        ProtocolChannel channel = channels.getServerChannel();
        channels.getClientChannel().startReceiving();
        channel.getReceiver(ManagementChannelReceiver.class).setOperationHandler(new ConcurrentRequestOperationHandler(3));
        System.out.println("-----");

        SimpleHandlers.Request request1 = new SimpleHandlers.Request(600);
        SimpleHandlers.Request request2 = new SimpleHandlers.Request(650);
        SimpleHandlers.Request request3 = new SimpleHandlers.Request(700);

        ManagementClientChannelStrategy strategy =  ManagementClientChannelStrategy.create(channels.getClientChannel());

        ExecutorService executorService = Executors.newCachedThreadPool();
        Future<Integer> future1 = request1.execute(executorService, strategy);
        Future<Integer> future2 = request2.execute(executorService, strategy);
        Future<Integer> future3 = request3.execute(executorService, strategy);
        Assert.assertEquals(Integer.valueOf(1400), future3.get());
        Assert.assertEquals(Integer.valueOf(1300), future2.get());
        Assert.assertEquals(Integer.valueOf(1200), future1.get());
    }

    protected abstract RemotingChannelPairSetup createSetup();
}
