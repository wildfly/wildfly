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
package org.jboss.as.clustering.lock;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupRpcDispatcher;
import org.jboss.as.clustering.MockClusterNode;
import org.jboss.as.clustering.ResponseFilter;
import org.jboss.as.clustering.lock.AbstractClusterLockSupport.RpcTarget;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager.LockResult;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.clustering.lock.LockParamsMatcher.eqLockParams;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of SharedLocalYieldingClusterLockManager.
 *
 *
 * @author Brian Stansberry
 *
 * @version $Revision$
 */
@Ignore("Fails intermittently")
public class SharedLocalYieldingClusterLockManagerUnitTestCase {
    private static final ResponseFilter NULL_FILTER = null;

    private ClusterNode node1 = new MockClusterNode(1);
    private ClusterNode node2 = new MockClusterNode(2);
    private ClusterNode node3 = new MockClusterNode(3);

    @Test
    public void testBasicLock() throws Exception {
        TesteeSet ts = getTesteeSet(node1, 0, 3);

        List<RemoteLockResponse> rspList = getOKResponses(2);
        when(ts.rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(ts.rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        assertEquals(LockResult.ACQUIRED_FROM_CLUSTER, ts.testee.lock("test", 1000, false));

        assertEquals(LockResult.ALREADY_HELD, ts.testee.lock("test", 1000, false));
        ts.testee.unlock("test", false);
        ts.testee.unlock("test", false);

        // Even though we fully released, we can still reacquire without asking cluster
        assertEquals(LockResult.ALREADY_HELD, ts.testee.lock("test", 1000, false));
    }

    @Test
    public void testNewLock() throws Exception {
        TesteeSet ts = getTesteeSet(node1, 0, 3);
        assertEquals(LockResult.NEW_LOCK, ts.testee.lock("test", 1000, true));
        assertEquals(LockResult.ALREADY_HELD, ts.testee.lock("test", 1000, true));
        ts.testee.unlock("test", false);
        ts.testee.unlock("test", false);

        // Even though we fully released, we can still reacquire without asking cluster
        assertEquals(LockResult.ALREADY_HELD, ts.testee.lock("test", 1000, true));
    }

    @Test
    public void testUnlockAndRemove() throws Exception {
        TesteeSet ts = getTesteeSet(node1, 0, 3);
        assertEquals(LockResult.NEW_LOCK, ts.testee.lock("test", 1000, true));
        ts.testee.unlock("test", true);

        assertEquals(LockResult.NEW_LOCK, ts.testee.lock("test", 1000, true));
        assertEquals(LockResult.ALREADY_HELD, ts.testee.lock("test", 1000, true));
        ts.testee.unlock("test", false);
        ts.testee.unlock("test", true);

        assertEquals(LockResult.NEW_LOCK, ts.testee.lock("test", 1000, true));
        assertEquals(LockResult.ALREADY_HELD, ts.testee.lock("test", 1000, true));
        ts.testee.unlock("test", true);
        assertEquals(LockResult.ALREADY_HELD, ts.testee.lock("test", 1000, true));
        ts.testee.unlock("test", false);
        ts.testee.unlock("test", false);

        assertEquals(LockResult.NEW_LOCK, ts.testee.lock("test", 1000, true));
        ts.testee.unlock("test", true);

        when(ts.rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        List<RemoteLockResponse> rspList = getOKResponses(2);
        when(ts.rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        assertEquals(LockResult.ACQUIRED_FROM_CLUSTER, ts.testee.lock("test", 1000, false));
    }

    @Test
    public void testRejectRemoteCaller() throws Exception {
        TesteeSet ts = getTesteeSet(node1, 0, 3);
        assertEquals(LockResult.NEW_LOCK, ts.testee.lock("test", 1000, true));
        RemoteLockResponse rsp = ts.target.remoteLock("test", node2, 100);
        assertNotNull(rsp);
        assertEquals(RemoteLockResponse.Flag.FAIL, rsp.flag);
        assertEquals(node1, rsp.holder);
    }

    @Test
    public void testRemoteCallerTakesUnknownLock() throws Exception {
        TesteeSet ts = getTesteeSet(node1, 0, 3);
        RemoteLockResponse rsp = ts.target.remoteLock("test", node2, 100);
        assertNotNull(rsp);
        assertEquals(RemoteLockResponse.Flag.OK, rsp.flag);
        assertNull(rsp.holder);
    }

    @Test
    public void testRemoteCallerTakesKnownLock() throws Exception {
        remoteCallerTakesTest(getTesteeSet(node1, 0, 3), false);
    }

    @Test
    public void testRemoteCallerTakesRemovedLock() throws Exception {
        remoteCallerTakesTest(getTesteeSet(node1, 0, 3), true);
    }

    private void remoteCallerTakesTest(TesteeSet ts, boolean localRemoves) throws Exception {
        assertEquals(LockResult.NEW_LOCK, ts.testee.lock("test", 1000, true));

        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);

        RemoteLocker locker = new RemoteLocker(ts, node2, readyLatch, endLatch);
        Executors.newSingleThreadExecutor().execute(locker);

        boolean gotLatch = readyLatch.await(5, TimeUnit.SECONDS);
        assertTrue("timed out waiting for ready latch",gotLatch);
        Thread.sleep(500);
        ts.testee.unlock("test", localRemoves);
        gotLatch = endLatch.await(5, TimeUnit.SECONDS);
        assertTrue("timed out waiting for end latch",gotLatch);
        if (locker.exception != null) {
            throw locker.exception;
        }

        assertNotNull(locker.result);
        assertEquals(RemoteLockResponse.Flag.OK, locker.result.flag);
    }

    @Test
    public void testLocalCallTakesLockBack() throws Exception {
        TesteeSet ts = getTesteeSet(node1, 0, 3);
        remoteCallerTakesTest(ts, false);

        List<RemoteLockResponse> rspList = getOKResponses(2);
        when(ts.rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(ts.rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        assertEquals(LockResult.ACQUIRED_FROM_CLUSTER, ts.testee.lock("test", 1000, false));
    }

    @Test
    public void testConcurrentLocalLocks() throws Exception {
        concurrentLocalLocksTest(false, -1);
    }

    @Test
    public void testConcurrentLocalNewLocks() throws Exception {
        concurrentLocalLocksTest(true, -1);
    }

    @Test
    public void testConcurrentLocalLocksSomeNew() throws Exception {
        concurrentLocalLocksTest(true, 0);
        concurrentLocalLocksTest(true, 1);
        concurrentLocalLocksTest(true, 2);
        concurrentLocalLocksTest(true, 3);
    }

    private void concurrentLocalLocksTest(boolean newLock, int newLockPos) throws Exception {
        TesteeSet ts = getTesteeSet(node1, 0, 3);

        if (!newLock || newLockPos >= 0) {
            List<RemoteLockResponse> rspList = getOKResponses(2);
            when(ts.rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
            when(ts.rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                            aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);
        }

        Locker[] lockers = new Locker[4];
        CountDownLatch readyLatch = new CountDownLatch(lockers.length);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(lockers.length);
        ExecutorService executor = Executors.newFixedThreadPool(lockers.length);

        for (int i = 0; i < lockers.length; i++) {
            boolean newOne = newLock && (i == newLockPos || newLockPos < 0);
            lockers[i] = new Locker(ts, newOne, readyLatch, startLatch, endLatch);
            executor.submit(lockers[i]);
        }

        boolean gotLatch = readyLatch.await(5, TimeUnit.SECONDS);
        assertTrue("timed out waiting for ready latch",gotLatch);
        startLatch.countDown();
        gotLatch = endLatch.await(5, TimeUnit.SECONDS);
        assertTrue("timed out waiting for ready latch",gotLatch);

        boolean sawNewLockResult = false;
        int lockPosition = -1;  // use in failure message
        for (Locker locker : lockers) {
            lockPosition++;
            String details = "failure on lock#" + lockPosition +" newLock is #"+newLockPos;
            if (locker.exception != null) {
                throw (Exception) locker.exception;
            }

            LockResult newLockResult = locker.newLock ? LockResult.NEW_LOCK : LockResult.ACQUIRED_FROM_CLUSTER;

            if (sawNewLockResult) {
                if (locker.newLock) {
                    assertTrue("locker result ("+locker.result+") is not ALREADY_HELD or NEW_LOCK, " + details,
                        locker.result == LockResult.ALREADY_HELD || locker.result == LockResult.NEW_LOCK);
                } else {
                    assertTrue("locker result ("+locker.result+") is not ALREADY_HELD or ACQUIRED_FROM_CLUSTER, " + details,
                        locker.result == LockResult.ALREADY_HELD || locker.result == LockResult.ACQUIRED_FROM_CLUSTER);
                }
            } else if (locker.result != LockResult.ALREADY_HELD) {
                assertEquals("expected lock to be " + newLockResult + " but was " + locker.result +", " +details,
                    newLockResult, locker.result);
                sawNewLockResult = true;
            }
        }

        assertTrue("Saw a new lock result", sawNewLockResult);
    }

    @Test
    public void testRejectionFromCluster() throws Exception {
        TesteeSet ts = getTesteeSet(node1, 0, 3);

        List<RemoteLockResponse> rspList = getRejectionResponses(node3, 2);
        when(ts.rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(ts.rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);
        // expect(ts.partition.callMethodOnCluster(eq("test"),
        // eq("releaseRemoteLock"),
        // eqLockParams(node1, 200000),
        // aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES),
        // eq(RemoteLockResponse.class),
        // eq(true),
        // eq(NULL_FILTER),
        // anyInt(),
        // eq(false))).andReturn(rspList);

        try {
            ts.testee.lock("test", 1000, false);
            fail("Did not throw TimeoutException");
        } catch (TimeoutException ok) {
        }
    }

    protected TesteeSet getTesteeSet(ClusterNode node, int viewPos, int viewSize) throws Exception {
        GroupRpcDispatcher rpcDispatcher = mock(GroupRpcDispatcher.class);
        GroupMembershipNotifier notifier = mock(GroupMembershipNotifier.class);
        when(rpcDispatcher.isConsistentWith(notifier)).thenReturn(true);
        when(rpcDispatcher.getClusterNode()).thenReturn(node);

        List<ClusterNode> view = getView(node, viewPos, viewSize);
        when(rpcDispatcher.getClusterNodes()).thenReturn(view);

        SharedLocalYieldingClusterLockManager testee = new SharedLocalYieldingClusterLockManager("test", rpcDispatcher, notifier);

        testee.start();

        ArgumentCaptor<RpcTarget> c = ArgumentCaptor.forClass(RpcTarget.class);
        verify(rpcDispatcher).registerRPCHandler(eq("test"), c.capture());

        return new TesteeSet(testee, rpcDispatcher, c.getValue());
    }

    private List<ClusterNode> getView(ClusterNode member, int viewPos, int numMembers) {
        List<ClusterNode> all = new ArrayList<ClusterNode>(Arrays.asList(node1, node2, node3));
        all.remove(member);
        while (all.size() > numMembers - 1) // -1 'cause we'll add one in a sec
        {
            all.remove(all.size() - 1);
        }
        all.add(viewPos, member);

        return all;
    }

    private static List<RemoteLockResponse> getOKResponses(int numResponses) {
        List<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        for (int i = 0; i < numResponses + 1; i++) {
            rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));
        }
        return rspList;
    }

    private static List<RemoteLockResponse> getRejectionResponses(ClusterNode owner, int numResponses) {
        List<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(owner, RemoteLockResponse.Flag.FAIL));
        for (int i = 1; i < numResponses + 1; i++) {
            rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));
        }
        return rspList;
    }

    private class TesteeSet {
        private final SharedLocalYieldingClusterLockManager testee;
        private final RpcTarget target;
        private final GroupRpcDispatcher rpcDispatcher;

        private TesteeSet(SharedLocalYieldingClusterLockManager testee, GroupRpcDispatcher rpcDispatcher, RpcTarget target) {
            this.testee = testee;
            this.rpcDispatcher = rpcDispatcher;
            this.target = target;
        }
    }

    private class Locker implements Runnable {
        private final TesteeSet ts;
        private final boolean newLock;
        private final CountDownLatch readyLatch;
        private final CountDownLatch startLatch;
        private final CountDownLatch endLatch;
        private LockResult result;
        private Exception exception;

        private Locker(TesteeSet ts, boolean newLock, CountDownLatch readyLatch, CountDownLatch startLatch,
                CountDownLatch endLatch) {
            this.ts = ts;
            this.newLock = newLock;
            this.readyLatch = readyLatch;
            this.startLatch = startLatch;
            this.endLatch = endLatch;
        }

        @Override
        public void run() {
            try {
                readyLatch.countDown();
                boolean gotLatch = startLatch.await(10, TimeUnit.SECONDS);
                if (! gotLatch) {
                    this.exception = new RuntimeException("background Locker thread, timed out waiting for start latch");
                    return;
                }
                result = ts.testee.lock("test", 1000, newLock);
            } catch (Exception e) {
                this.exception = e;
            } finally {
                endLatch.countDown();
            }
        }
    }

    private class RemoteLocker implements Runnable {
        private final TesteeSet ts;
        private final ClusterNode caller;
        private final CountDownLatch readyLatch;
        private final CountDownLatch endLatch;
        private RemoteLockResponse result;
        private Exception exception;

        private RemoteLocker(TesteeSet ts, ClusterNode caller, CountDownLatch readyLatch, CountDownLatch endLatch) {
            this.ts = ts;
            this.caller = caller;
            this.readyLatch = readyLatch;
            this.endLatch = endLatch;
        }

        @Override
        public void run() {
            try {
                readyLatch.countDown();
                result = ts.target.remoteLock("test", caller, 2000);
            } catch (Exception e) {
                this.exception = e;
            } finally {
                endLatch.countDown();
            }
        }
    }
}
