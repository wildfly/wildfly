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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.jboss.as.protocol.mgmt.support.RemoteChannelPairSetup;
import org.jboss.as.protocol.mgmt.support.RemotingChannelPairSetup;
import org.jboss.as.protocol.mgmt.support.SimpleHandlers;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteChannelManagementTestCase extends AbstractManagementTestCase {

    @Override
    protected RemotingChannelPairSetup createSetup() {
        return new RemoteChannelPairSetup();
    }

    @Test
    public void testBatchIdManagerNoManager() {
        ManagementChannel channel = channels.getServerChannel();
        channels.getClientChannel().startReceiving();
        channel.setOperationHandler(new SimpleHandlers.OperationHandler());

        try {
            channel.createBatchId();
            Assert.fail("Should have given error on no batch manager");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testBatchIdManager() throws Exception {
        final CountDownLatch freedLatch = new CountDownLatch(1);
        final AtomicInteger freedId = new AtomicInteger();

        ManagementChannel channel = channels.getServerChannel();
        channels.getClientChannel().startReceiving();
        channel.setOperationHandler(new SimpleHandlers.OperationHandler());
        channel.setBatchIdManager(new ManagementBatchIdManager() {

            @Override
            public void freeBatchId(int id) {
                freedId.set(id);
                freedLatch.countDown();
            }

            @Override
            public int createBatchId() {
                return 12345;
            }
        });

        try {
            Assert.assertEquals(12345, channels.getClientChannel().createBatchId());
        } finally {
            channels.getClientChannel().freeBatchId(12345);
        }

        freedLatch.await();
        Assert.assertEquals(12345, freedId.get());
    }
}
