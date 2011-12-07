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

import java.io.DataInput;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.jboss.as.protocol.mgmt.support.RemoteChannelPairSetup;
import org.jboss.as.protocol.mgmt.support.RemotingChannelPairSetup;
import org.jboss.as.protocol.mgmt.support.SimpleHandlers;
import org.jboss.as.protocol.mgmt.support.SimpleHandlers.SimpleClient;
import org.jboss.threads.AsyncFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteChannelManagementTestCase {

    private RemotingChannelPairSetup channels;

    @Before
    public void start() throws Exception {
        channels = new RemoteChannelPairSetup();
        channels.setupRemoting(new SimpleHandlers.OperationHandler());
        channels.startChannels();
    }

    @After
    public void stop() throws Exception {
        channels.stopChannels();
        channels.shutdownRemoting();
    }

    @Test
    public void testSimpleRequest() throws Exception {

        final SimpleClient client = SimpleClient.create(channels);

        SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 600);
        Assert.assertEquals(Integer.valueOf(1200), client.executeForResult(request));
    }

    @Test
    public void testTwoSimpleRequests() throws Exception {
        final SimpleClient client = SimpleClient.create(channels);

        SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 600);
        Assert.assertEquals(Integer.valueOf(1200), client.executeForResult(request));

        request = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 700);
        Assert.assertEquals(Integer.valueOf(1400), client.executeForResult(request));
    }

    @Test
    public void testSeveralConcurrentSimpleRequests() throws Exception {
        final SimpleClient client = SimpleClient.create(channels);

        SimpleHandlers.Request request1 = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 600);
        SimpleHandlers.Request request2 = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 650);
        SimpleHandlers.Request request3 = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 700);

        Future<Integer> future1 = client.execute(request1);
        Future<Integer> future2 = client.execute(request2);
        Future<Integer> future3 = client.execute(request3);
        Assert.assertEquals(Integer.valueOf(1400), future3.get());
        Assert.assertEquals(Integer.valueOf(1300), future2.get());
        Assert.assertEquals(Integer.valueOf(1200), future1.get());
    }

    @Test
    public void testMissingOperationHandler() throws Exception {
        final SimpleClient client = SimpleClient.create(channels);

        //Don't set the operation handler here
        SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.REQUEST_WITH_NO_HANDLER, 600);
        try {
            client.executeForResult(request);
            Assert.fail("Should have failed");
        } catch (ExecutionException expected) {
            Assert.assertTrue(expected.getCause() instanceof IOException);
        }
    }


    @Test
    public void testMissingRequestHandler() throws Exception {
        final SimpleClient client = SimpleClient.create(channels);

        SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.REQUEST_WITH_NO_HANDLER, 600);
        try {
            client.executeForResult(request);
            Assert.fail("Should have failed");
        } catch (ExecutionException expected) {
            Assert.assertTrue(expected.getCause() instanceof IOException);
        }
    }

    @Test
    public void testExceptionInRequestHandlerRead() throws Exception {
        final SimpleClient client = SimpleClient.create(channels);

        SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.REQUEST_WITH_BAD_READ, 600);
        try {
            client.executeForResult(request);
            Assert.fail("Should have failed");
        } catch (ExecutionException expected) {
            Assert.assertTrue(expected.getCause() instanceof IOException);
        }
    }

    @Test
    public void testExceptionInRequestHandlerWrite() throws Exception {
        final SimpleClient client = SimpleClient.create(channels);

        SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.REQUEST_WITH_BAD_WRITE, 600);
        try {
            client.executeForResult(request);
            Assert.fail("Should have failed");
        } catch (ExecutionException expected) {
            Assert.assertTrue(expected.getCause() instanceof IOException);
        }
    }


    @Test
    public void testExceptionInRequestWrite() throws Exception {
        final SimpleClient client = SimpleClient.create(channels);

        SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 600) {

            @Override
            protected void sendRequest(ActiveOperation.ResultHandler<Integer> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
                throw new RuntimeException("Oh no!!!!");
            }

        };
        try {
            client.executeForResult(request);
            Assert.fail("Should have failed");
        } catch (ExecutionException expected) {
            // TODO intermitted failures instanceof RuntimeException
            // Assert.assertTrue(expected.getClass().toString(), expected.getCause() instanceof RuntimeException);
        }
    }

    @Test
    public void testNoResponse() throws Exception {
        // Test request handler waiting for a response from the server, but never getting one...
        // however once the channel gets closed the request should get cancelled
        final SimpleClient client = SimpleClient.create(channels);
        final SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.REQUEST_WITH_NO_RESPONSE, 600);
        final AsyncFuture<Integer> future = client.execute(request);
        try {
            future.get(1, TimeUnit.SECONDS);
            Assert.fail();
        } catch(Exception expected) {
            //
        }
        channels.getClientChannel().close();
        try {
            future.get();
            Assert.fail();
        } catch(CancellationException expected) {
            //
        }
        Assert.assertEquals(future.getStatus(), AsyncFuture.Status.CANCELLED);
    }

    @Test
    public void testCancelAsyncTask() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleClient client = SimpleClient.create(channels);
        final SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 600) {

            @Override
            public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Integer> resultHandler, final ManagementRequestContext<Void> context) throws IOException {
                final int i = input.readInt();
                context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                    @Override
                    public void execute(ManagementRequestContext<Void> context) throws Exception {
                        try {
                            synchronized (this) {
                                wait();
                            }
                            resultHandler.done(i);
                        } catch(InterruptedException e) {
                            latch.countDown();
                        }
                    }
                });
            }
        };
        final AsyncFuture<Integer> future = client.execute(request);
        final AsyncFuture.Status completed = future.await(1, TimeUnit.SECONDS);
        Assert.assertEquals(completed, AsyncFuture.Status.WAITING);
        future.cancel(false);
        latch.await();
        Assert.assertEquals(future.getStatus(), AsyncFuture.Status.CANCELLED);
    }

    @Test
    public void testAwaitCompletion() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleClient client = SimpleClient.create(channels);
        final SimpleHandlers.Request request = new SimpleHandlers.Request(SimpleHandlers.SIMPLE_REQUEST, 600) {
            @Override
            public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Integer> resultHandler, final ManagementRequestContext<Void> context) throws IOException {
                final int i = input.readInt();
                context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                    @Override
                    public void execute(ManagementRequestContext<Void> voidManagementRequestContext) throws Exception {
                        latch.await();
                        resultHandler.done(i);
                    }
                });
            }
        };
        final AsyncFuture<Integer> future = client.execute(request);
        final AsyncFuture.Status completed = future.await(1, TimeUnit.SECONDS);
        Assert.assertEquals(completed, AsyncFuture.Status.WAITING);
        client.shutdown();
        boolean done = client.awaitCompletion(1, TimeUnit.SECONDS);
        Assert.assertFalse(done);
        latch.countDown();
        done = client.awaitCompletion(2, TimeUnit.SECONDS);
        Assert.assertTrue(done);
    }

}
