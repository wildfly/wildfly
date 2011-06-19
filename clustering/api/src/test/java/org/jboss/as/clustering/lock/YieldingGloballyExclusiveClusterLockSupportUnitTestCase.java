/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.clustering.lock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Vector;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupRpcDispatcher;
import org.jboss.as.clustering.lock.AbstractClusterLockSupport.RpcTarget;

/**
 * Unit test of ExclusiveClusterLockManager
 *
 * @author Brian Stansberry
 *
 */
public class YieldingGloballyExclusiveClusterLockSupportUnitTestCase extends ClusteredLockManagerTestBase<YieldingGloballyExclusiveClusterLockSupport> {

    @Override
    protected YieldingGloballyExclusiveClusterLockSupport createClusteredLockManager(String serviceHAName, GroupRpcDispatcher rpcDispatcher, GroupMembershipNotifier notifier, LocalLockHandler handler) {
        return new YieldingGloballyExclusiveClusterLockSupport(serviceHAName, rpcDispatcher, notifier, handler);
    }

    public void testBasicRemoteLock() throws Exception {
        TesteeSet<YieldingGloballyExclusiveClusterLockSupport> testeeSet = getTesteeSet(node1, 1, 2);
        YieldingGloballyExclusiveClusterLockSupport testee = testeeSet.impl;
        LocalLockHandler handler = testee.getLocalHandler();
        RpcTarget target = testeeSet.target;

        ClusterNode caller = testee.getCurrentView().get(0);
        assertFalse(node1.equals(caller));

        RemoteLockResponse rsp = target.remoteLock("test", caller, 1000);

        verify(handler).lockFromCluster("test", caller, 1000);
        
        assertEquals(RemoteLockResponse.Flag.OK, rsp.flag);
        assertNull(rsp.holder);

        // Do it again; should still work
        rsp = target.remoteLock("test", caller, 1000);

        verify(handler).lockFromCluster("test", caller, 1000);
        
        assertEquals(RemoteLockResponse.Flag.OK, rsp.flag);
        assertNull(rsp.holder);
    }

    public void testContestedRemoteLock() throws Exception {
        TesteeSet<YieldingGloballyExclusiveClusterLockSupport> testeeSet = getTesteeSet(node1, 1, 3);
        YieldingGloballyExclusiveClusterLockSupport testee = testeeSet.impl;
        LocalLockHandler handler = testee.getLocalHandler();
        RpcTarget target = testeeSet.target;

        ClusterNode caller1 = testee.getCurrentView().get(0);
        assertFalse(node1.equals(caller1));

        ClusterNode caller2 = testee.getCurrentView().get(2);
        assertFalse(node1.equals(caller2));

        RemoteLockResponse rsp = target.remoteLock("test", caller1, 1000);

        verify(handler).lockFromCluster("test", caller1, 1000);

        assertEquals(RemoteLockResponse.Flag.OK, rsp.flag);
        assertNull(rsp.holder);

        // A call from a different caller should still work as
        // w/ supportLockOnly==false we only reject if WE hold the lock
        rsp = target.remoteLock("test", caller2, 1000);

        verify(handler).lockFromCluster("test", caller2, 1000);

        assertEquals(RemoteLockResponse.Flag.OK, rsp.flag);
        assertNull(rsp.holder);
    }

    /**
     * Test that if a member holds a lock but is then removed from the view, another remote member can obtain the lock.
     *
     * @throws Exception
     */
    public void testDeadMemberCleanupAllowsRemoteLock() throws Exception {
        TesteeSet<YieldingGloballyExclusiveClusterLockSupport> testeeSet = getTesteeSet(node1, 1, 3);
        YieldingGloballyExclusiveClusterLockSupport testee = testeeSet.impl;
        LocalLockHandler handler = testee.getLocalHandler();
        RpcTarget target = testeeSet.target;

        List<ClusterNode> members = testee.getCurrentView();
        ClusterNode caller1 = members.get(0);
        assertFalse(node1.equals(caller1));

        ClusterNode caller2 = members.get(2);
        assertFalse(node1.equals(caller2));

        RemoteLockResponse rsp = target.remoteLock("test", caller1, 1000);

        verify(handler).lockFromCluster("test", caller1, 1000);

        assertEquals(RemoteLockResponse.Flag.OK, rsp.flag);
        assertNull(rsp.holder);

        // Change the view
        Vector<ClusterNode> dead = new Vector<ClusterNode>();
        dead.add(caller1);

        Vector<ClusterNode> all = new Vector<ClusterNode>(members);
        all.remove(caller1);

        testee.membershipChanged(dead, new Vector<ClusterNode>(), all);

        // A call from a different caller should work
        rsp = target.remoteLock("test", caller2, 1000);

        verify(handler).lockFromCluster("test", caller2, 1000);

        assertEquals(RemoteLockResponse.Flag.OK, rsp.flag);
        assertNull(rsp.holder);
    }
}
