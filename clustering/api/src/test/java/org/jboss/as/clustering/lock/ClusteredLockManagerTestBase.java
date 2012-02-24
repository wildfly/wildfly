package org.jboss.as.clustering.lock;

import static org.jboss.as.clustering.lock.LockParamsMatcher.eqLockParams;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupRpcDispatcher;
import org.jboss.as.clustering.MockClusterNode;
import org.jboss.as.clustering.ResponseFilter;
import org.jboss.as.clustering.lock.AbstractClusterLockSupport.RpcTarget;
import org.jboss.marshalling.ClassResolver;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class ClusteredLockManagerTestBase<T extends AbstractClusterLockSupport> {
    protected static final ResponseFilter NULL_FILTER = null;

    protected ClusterNode node1 = new MockClusterNode(1);
    protected ClusterNode node2 = new MockClusterNode(2);
    protected ClusterNode node3 = new MockClusterNode(3);

    @Test
    public void start() throws Exception {
        GroupRpcDispatcher rpcDispatcher = mock(GroupRpcDispatcher.class);
        GroupMembershipNotifier notifier = mock(GroupMembershipNotifier.class);
        LocalLockHandler handler = mock(LocalLockHandler.class);
        try {
            createClusteredLockManager(null, rpcDispatcher, notifier, handler);
            fail("Null serviceHAName should prevent construction");
        } catch (IllegalArgumentException good) {
        }

        try {
            createClusteredLockManager("test", null, notifier, handler);
            fail("Null GroupRpcDispatcher should prevent construction");
        } catch (IllegalArgumentException good) {
        }

        try {
            createClusteredLockManager("test", rpcDispatcher, null, handler);
            fail("Null GroupMembershipNotifier should prevent construction");
        } catch (IllegalArgumentException good) {
        }

        try {
            createClusteredLockManager("test", rpcDispatcher, notifier, null);
            fail("Null LocalLockHandler should prevent construction");
        } catch (IllegalArgumentException good) {
        }

        when(rpcDispatcher.isConsistentWith(notifier)).thenReturn(Boolean.TRUE);
        when(rpcDispatcher.getClusterNode()).thenReturn(node1);
        when(rpcDispatcher.getGroupName()).thenReturn("TestPartition");

        T testee = createClusteredLockManager("test", rpcDispatcher, notifier, handler);

        assertEquals("test", testee.getServiceHAName());
        assertEquals("TestPartition", testee.getGroupName());
        
        try {
            testee.lock("id", 1000);
            fail("Call to lock() should fail if not started");
        } catch (IllegalStateException good) {
        }

        try {
            testee.unlock("id");
            fail("Call to unlock() should fail if not started");
        } catch (IllegalStateException good) {
        }

        assertEquals("Current view is empty when unstarted", 0, testee.getCurrentView().size());

        when(rpcDispatcher.getClusterNodes()).thenReturn(Arrays.asList(node1));

        testee.start();

        verify(rpcDispatcher).registerRPCHandler(eq("test"), any(RpcTarget.class), isA(ClassResolver.class));
        verify(notifier).registerGroupMembershipListener(testee);

        assertEquals("Current view is correct", 1, testee.getCurrentView().size());
        assertTrue(testee.getCurrentView().contains(node1));
    }

    @Test
    public void stop() throws Exception {
        TesteeSet<T> testeeSet = getTesteeSet(node1, 0, 1);
        T testee = testeeSet.impl;
        
        testee.stop();
        
        verify(testee.getGroupMembershipNotifier()).unregisterGroupMembershipListener(same(testee));
        verify(testee.getGroupRpcDispatcher()).unregisterRPCHandler(eq("test"), same(testeeSet.target));

        assertEquals("Current view is empty when stopped", 0, testee.getCurrentView().size());

        try {
            testee.lock("id", 1000);
            fail("Call to lock() should fail if stopped");
        } catch (IllegalStateException good) {
        }

        try {
            testee.unlock("id");
            fail("Call to unlock() should fail if stopped");
        } catch (IllegalStateException good) {
        }
    }

    @Test
    public void getMembers() throws Exception {
        TesteeSet<T> testeeSet = getTesteeSet(node1, 1, 2);
        T testee = testeeSet.impl;

        List<ClusterNode> members = testee.getCurrentView();
        assertEquals(2, members.size());
        assertEquals(node1, members.get(1));

        ClusterNode dead = members.get(0);
        assertFalse(node1.equals(dead));

        List<ClusterNode> newView = getView(node1, 0, 3);
        newView.remove(dead);

        List<ClusterNode> addedMembers = new ArrayList<ClusterNode>(newView);
        addedMembers.removeAll(members);

        List<ClusterNode> deadMembers = new ArrayList<ClusterNode>();
        deadMembers.add(dead);

        testee.membershipChanged(deadMembers, addedMembers, newView);

        members = testee.getCurrentView();
        assertEquals(2, members.size());
        assertEquals(node1, members.get(0));
        assertFalse(node1.equals(members.get(1)));
        assertFalse(members.contains(dead));
    }

    /**
     * Simple test of acquiring a cluster-wide lock in a two node cluster where local-only locks are supported.
     *
     * @throws Exception
     */
    @Test
    public void basicClusterLock() throws Exception {
        basicClusterLockTest(2);
    }

    /**
     * Simple test of acquiring a cluster-wide lock in a cluster where the caller is the only member and where local-only locks
     * are supported.
     *
     * @throws Exception
     */
    @Test
    public void standaloneClusterLock() throws Exception {
        basicClusterLockTest(1);
    }

    private void basicClusterLockTest(int viewSize) throws Exception {
        int viewPos = viewSize == 1 ? 0 : 1;
        TesteeSet<T> testeeSet = getTesteeSet(node1, viewPos, viewSize);
        AbstractClusterLockSupport testee = testeeSet.impl;
        GroupRpcDispatcher rpcDispatcher = testee.getGroupRpcDispatcher();
        LocalLockHandler handler = testee.getLocalHandler();

        List<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        for (int i = 0; i < viewSize - 1; i++) {
            rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));
        }
        when(rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), LockParamsMatcher.eqLockParams(node1, 200000),
                AdditionalMatchers.aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        assertTrue(testee.lock("test", 200000));
        
        verify(handler).lockFromCluster(eq("test"), eq(node1), anyLong());
    }

    @Test
    public void testRemoteRejectionFromSuperiorCaller() throws Exception {
        TesteeSet<T> testeeSet = getTesteeSet(node1, 1, 3);
        AbstractClusterLockSupport testee = testeeSet.impl;
        GroupRpcDispatcher rpcDispatcher = testee.getGroupRpcDispatcher();
/*
        resetToNice(partition);
        resetToStrict(handler);
*/
        ClusterNode superior = testee.getCurrentView().get(0);

        List<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.REJECT, superior));

        when(rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        assertFalse(testee.lock("test", 50));
    }

    @Test
    public void testRemoteRejectionFromInferiorCaller() throws Exception {
        TesteeSet<T> testeeSet = getTesteeSet(node1, 1, 3);
        AbstractClusterLockSupport testee = testeeSet.impl;
        GroupRpcDispatcher rpcDispatcher = testee.getGroupRpcDispatcher();
        LocalLockHandler handler = testee.getLocalHandler();
/*
        resetToStrict(partition);
        resetToStrict(handler);
*/
        ClusterNode inferior = testee.getCurrentView().get(2);

        List<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.REJECT, inferior));

        when(rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        when((List<Object>) rpcDispatcher.callMethodOnCluster(eq("test"), eq("releaseRemoteLock"), aryEq(new Object[] { "test", node1 }),
                aryEq(AbstractClusterLockSupport.RELEASE_REMOTE_LOCK_TYPES), eq(true))).thenReturn(new ArrayList<Object>());

        rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));

        when(rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        assertTrue(testee.lock("test", 50));

        verify(handler).lockFromCluster(eq("test"), eq(node1), anyLong());
    }

    @Test
    public void testLocalLockingStateRejectsSuperiorRemoteCaller() throws Exception {
        TesteeSet<T> testeeSet = getTesteeSet(node1, 1, 3);
        T testee = testeeSet.impl;
        GroupRpcDispatcher rpcDispatcher = testee.getGroupRpcDispatcher();
        LocalLockHandler handler = testee.getLocalHandler();
        final RpcTarget target = testeeSet.target;

        ClusterNode superiorCaller = testee.getCurrentView().get(0);
        assertFalse(node1.equals(superiorCaller));
/*
        resetToStrict(partition);
        makeThreadSafe(partition, true);
        resetToStrict(handler);
        makeThreadSafe(handler, true);
*/
        List<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));

        when(rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        // When caller 1 invokes, block before giving response
        CountDownLatch answerAwaitLatch = new CountDownLatch(1);
        CountDownLatch answerStartLatch = new CountDownLatch(1);
        CountDownLatch answerDoneLatch = new CountDownLatch(1);
        BlockingAnswer<Boolean> caller1Answer = new BlockingAnswer<Boolean>(Boolean.TRUE, answerAwaitLatch, answerStartLatch, null);
        doAnswer(caller1Answer).when(handler).lockFromCluster(eq("test"), eq(node1), anyLong());

        LocalLockCaller winner = new LocalLockCaller(testee, null, null, answerDoneLatch, 500);

        Thread t1 = new Thread(winner);
        t1.setDaemon(true);

        try {
            t1.start();
            assertTrue(answerStartLatch.await(500, TimeUnit.SECONDS));
            // t1 should now be blocking in caller1Answer

            RemoteLockResponse rsp = target.remoteLock("test", superiorCaller, 1);
            assertEquals(RemoteLockResponse.Flag.REJECT, rsp.flag);
            assertEquals(node1, rsp.holder);

            // release t1
            answerAwaitLatch.countDown();

            // wait for t1 to complete
            assertTrue(answerDoneLatch.await(5, TimeUnit.SECONDS));

            rethrow("winner had an exception", winner.getException());

            Boolean locked = winner.getResult();
            assertEquals(Boolean.TRUE, locked);
        } finally {
            if (t1.isAlive()) {
                t1.interrupt();
            }
        }
    }

    @Test
    public void testRemoteLockingStateAllowsSuperiorRemoteCaller() throws Exception {
        TesteeSet<T> testeeSet = getTesteeSet(node1, 1, 3);
        T testee = testeeSet.impl;
        GroupRpcDispatcher rpcDispatcher = testee.getGroupRpcDispatcher();
        LocalLockHandler handler = testee.getLocalHandler();
        final RpcTarget target = testeeSet.target;

        ClusterNode superiorCaller = testee.getCurrentView().get(0);
        assertFalse(node1.equals(superiorCaller));

        // When caller 1 invokes, block before giving response
        CountDownLatch answerAwaitLatch = new CountDownLatch(1);
        CountDownLatch answerStartLatch = new CountDownLatch(1);

        ArrayList<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.REJECT, superiorCaller));

        BlockingAnswer<List<RemoteLockResponse>> caller1Answer = new BlockingAnswer<List<RemoteLockResponse>>(rspList,
                answerAwaitLatch, answerStartLatch, null);

        when(rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        doAnswer(caller1Answer).when(rpcDispatcher).callMethodOnCluster(eq("test"), eq("remoteLock"), eqLockParams(node1, 200000),
                        aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false));

        handler.lockFromCluster(eq("test"), eq(superiorCaller), anyLong());

        when((List<Object>) rpcDispatcher.callMethodOnCluster(eq("test"), eq("releaseRemoteLock"), aryEq(new Object[] { "test",
                        node1 }), aryEq(AbstractClusterLockSupport.RELEASE_REMOTE_LOCK_TYPES), eq(true))).thenReturn(
                new ArrayList<Object>());
/*
        replay(partition);
        replay(handler);
*/
        CountDownLatch finishedLatch = new CountDownLatch(1);
        LocalLockCaller loser = new LocalLockCaller(testee, null, null, finishedLatch);

        Thread t1 = new Thread(loser);
        t1.setDaemon(true);

        try {
            t1.start();
            assertTrue(answerStartLatch.await(1, TimeUnit.SECONDS));
            // t1 should now be blocking in caller1Answer

            RemoteLockResponse rsp = target.remoteLock("test", superiorCaller, 1);
            assertEquals(RemoteLockResponse.Flag.OK, rsp.flag);

            // release t1
            answerAwaitLatch.countDown();

            // wait for t1 to complete
            assertTrue(finishedLatch.await(5, TimeUnit.SECONDS));

            rethrow("winner had an exception", loser.getException());

            Boolean locked = loser.getResult();
            assertEquals(Boolean.FALSE, locked);
        } finally {
            if (t1.isAlive()) {
                t1.interrupt();
            }
        }
    }

    @Test
    public void testRemoteLockingStateRejectsInferiorRemoteCaller() throws Exception {
        TesteeSet<T> testeeSet = getTesteeSet(node1, 1, 3);
        T testee = testeeSet.impl;
        GroupRpcDispatcher rpcDispatcher = testee.getGroupRpcDispatcher();
        LocalLockHandler handler = testee.getLocalHandler();
        final RpcTarget target = testeeSet.target;

        ClusterNode inferiorNode = testee.getCurrentView().get(2);
        assertFalse(node1.equals(inferiorNode));

        ClusterNode superiorNode = testee.getCurrentView().get(0);
        assertFalse(node1.equals(superiorNode));

        // When caller 1 invokes, block before giving response
        CountDownLatch answerAwaitLatch = new CountDownLatch(1);
        CountDownLatch answerStartLatch = new CountDownLatch(1);

        ArrayList<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(superiorNode, RemoteLockResponse.Flag.OK));
        rspList.add(new RemoteLockResponse(inferiorNode, RemoteLockResponse.Flag.REJECT, inferiorNode));

        BlockingAnswer<List<RemoteLockResponse>> caller1Answer = new BlockingAnswer<List<RemoteLockResponse>>(rspList, answerAwaitLatch, answerStartLatch, null);

        rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(superiorNode, RemoteLockResponse.Flag.OK));
        rspList.add(new RemoteLockResponse(inferiorNode, RemoteLockResponse.Flag.OK));

        when(rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), LockParamsMatcher.eqLockParams(node1, 200000),
                AdditionalMatchers.aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenAnswer(caller1Answer).thenReturn(rspList);

        when((List<Object>) rpcDispatcher.callMethodOnCluster(eq("test"), eq("releaseRemoteLock"), AdditionalMatchers.aryEq(new Object[] { "test", node1 }), AdditionalMatchers.aryEq(AbstractClusterLockSupport.RELEASE_REMOTE_LOCK_TYPES), eq(true))).thenReturn(new ArrayList<Object>());

        doNothing().when(handler).lockFromCluster(eq("test"), eq(node1), anyLong());

        CountDownLatch finishedLatch = new CountDownLatch(1);
        LocalLockCaller winner = new LocalLockCaller(testee, null, null, finishedLatch);

        Thread t1 = new Thread(winner);
        t1.setDaemon(true);

        try {
            t1.start();
            assertTrue(answerStartLatch.await(1, TimeUnit.SECONDS));
            // t1 should now be blocking in caller1Answer

            RemoteLockResponse rsp = target.remoteLock("test", inferiorNode, 1);
            assertEquals(RemoteLockResponse.Flag.REJECT, rsp.flag);
            assertEquals(node1, rsp.holder);

            // release t1
            answerAwaitLatch.countDown();

            // wait for t1 to complete
            assertTrue(finishedLatch.await(5, TimeUnit.SECONDS));

            rethrow("winner had an exception", winner.getException());

            Boolean locked = winner.getResult();
            assertEquals(Boolean.TRUE, locked);
        } finally {
            if (t1.isAlive()) {
                t1.interrupt();
            }
        }
    }

    /**
     * Local node acquires a lock; remote node tries to releasem which is ignored.
     *
     * @throws Exception
     */
    @Test
    public void testSpuriousRemoteLockReleaseIgnored() throws Exception {
        TesteeSet<T> testeeSet = getTesteeSet(node1, 1, 2);
        AbstractClusterLockSupport testee = testeeSet.impl;
        GroupRpcDispatcher rpcDispatcher = testee.getGroupRpcDispatcher();
        LocalLockHandler handler = testee.getLocalHandler();

        ClusterNode other = testee.getCurrentView().get(0);

        ArrayList<RemoteLockResponse> rspList = new ArrayList<RemoteLockResponse>();
        rspList.add(new RemoteLockResponse(null, RemoteLockResponse.Flag.OK));

        when(rpcDispatcher.getMethodCallTimeout()).thenReturn(60000l);
        when(rpcDispatcher.<RemoteLockResponse>callMethodOnCluster(eq("test"), eq("remoteLock"), LockParamsMatcher.eqLockParams(node1, 200000),
                AdditionalMatchers.aryEq(AbstractClusterLockSupport.REMOTE_LOCK_TYPES), eq(true), eq(NULL_FILTER), anyInt(), eq(false))).thenReturn(rspList);

        when(handler.getLockHolder("test")).thenReturn(node1);

        assertTrue(testee.lock("test", 200000));

        verify(handler).lockFromCluster(eq("test"), eq(node1), anyLong());
        
        testeeSet.target.releaseRemoteLock("test", other);

        verify(handler, never()).unlockFromCluster("test", other);
    }

    protected TesteeSet<T> getTesteeSet(ClusterNode node, int viewPos, int viewSize) throws Exception {
        GroupRpcDispatcher rpcDispatcher = mock(GroupRpcDispatcher.class);
        GroupMembershipNotifier notifier = mock(GroupMembershipNotifier.class);
        LocalLockHandler handler = mock(LocalLockHandler.class);
        when(rpcDispatcher.isConsistentWith(notifier)).thenReturn(true);
        when(rpcDispatcher.getClusterNode()).thenReturn(node);
        when(rpcDispatcher.getGroupName()).thenReturn("TestPartition");

        ArgumentCaptor<RpcTarget> c = ArgumentCaptor.forClass(RpcTarget.class);
        
        List<ClusterNode> view = getView(node, viewPos, viewSize);
        when(rpcDispatcher.getClusterNodes()).thenReturn(view);
        when(rpcDispatcher.isConsistentWith(notifier)).thenReturn(true);
        
        T testee = createClusteredLockManager("test", rpcDispatcher, notifier, handler);

        testee.start();
        
        verify(rpcDispatcher).registerRPCHandler(eq("test"), c.capture(), isA(ClassResolver.class));
        verify(notifier).registerGroupMembershipListener(same(testee));
        verify(handler).setLocalNode(same(node));
        
        return new TesteeSet<T>(testee, c.getValue());
    }

    protected abstract T createClusteredLockManager(String serviceHAName, GroupRpcDispatcher rpcDispatcher, GroupMembershipNotifier notifier, LocalLockHandler handler);

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

    protected static void rethrow(String msg, Throwable t) throws Exception {
        if (t != null) {
            if (t instanceof AssertionError) {
                AssertionError rethrow = new AssertionError(msg);
                rethrow.initCause(t);
                throw rethrow;
            }

            throw new RuntimeException(msg, t);
        }
    }

    protected class TesteeSet<C extends AbstractClusterLockSupport> {
        public final C impl;
        public final RpcTarget target;

        TesteeSet(C impl, RpcTarget target) {
            this.impl = impl;
            this.target = target;
        }
    }

    /**
     * Allows EasyMock to block before returning.
     *
     * @author Brian Stansberry
     *
     * @param <T>
     */
    protected class BlockingAnswer<C> implements Answer<C> {
        private final C answer;
        private final Exception toThrow;
        private final CountDownLatch startLatch;
        private final CountDownLatch awaitlatch;
        private final CountDownLatch endLatch;
        private final long timeout;

        public BlockingAnswer(C answer, CountDownLatch awaitLatch, CountDownLatch startLatch, CountDownLatch endLatch) {
            this(answer, awaitLatch, 0, startLatch, endLatch);
        }

        public BlockingAnswer(C answer, CountDownLatch awaitLatch, long timeout, CountDownLatch startLatch,
                CountDownLatch endLatch) {
            this.awaitlatch = awaitLatch;
            this.startLatch = startLatch;
            this.endLatch = endLatch;
            this.timeout = timeout;
            this.answer = answer;
            this.toThrow = null;
        }

        public BlockingAnswer(Exception toThrow, CountDownLatch awaitLatch, long timeout, CountDownLatch startLatch,
                CountDownLatch endLatch) {
            this.awaitlatch = awaitLatch;
            this.startLatch = startLatch;
            this.endLatch = endLatch;
            this.timeout = timeout;
            this.answer = null;
            this.toThrow = toThrow;
        }

        /**
         * {@inheritDoc}
         * @see org.mockito.stubbing.Answer#answer(org.mockito.invocation.InvocationOnMock)
         */
        @Override
        public C answer(InvocationOnMock invocation) throws Throwable {
            if (startLatch != null) {
                startLatch.countDown();
            }

            try {
                if (timeout > 0) {
                    awaitlatch.await(timeout, TimeUnit.MILLISECONDS);
                } else {
                    awaitlatch.await();
                }

                if (toThrow != null) {
                    throw toThrow;
                }

                return answer;
            } finally {
                if (endLatch != null) {
                    endLatch.countDown();
                }
            }
        }
    }

    protected abstract class AbstractCaller<C> implements Runnable {
        private final CountDownLatch startLatch;
        private final CountDownLatch proceedLatch;
        private final CountDownLatch finishLatch;
        private C result;
        private Throwable exception;

        AbstractCaller(CountDownLatch startLatch, CountDownLatch proceedLatch, CountDownLatch finishLatch) {
            this.startLatch = startLatch;
            this.proceedLatch = proceedLatch;
            this.finishLatch = finishLatch;
        }

        @Override
        public void run() {
            try {
                if (startLatch != null) {
                    startLatch.countDown();
                }

                if (proceedLatch != null) {
                    proceedLatch.await();
                }

                result = execute();
            } catch (Throwable t) {
                exception = t;
            } finally {
                if (finishLatch != null) {
                    finishLatch.countDown();
                }

            }
        }

        protected abstract C execute();

        public C getResult() {
            return result;
        }

        public Throwable getException() {
            return exception;
        }
    }

    protected class RemoteLockCaller extends AbstractCaller<RemoteLockResponse> {
        private final RpcTarget target;
        private final ClusterNode caller;

        public RemoteLockCaller(RpcTarget target, ClusterNode caller, CountDownLatch startLatch, CountDownLatch proceedLatch, CountDownLatch finishLatch) {
            super(startLatch, proceedLatch, finishLatch);
            this.target = target;
            this.caller = caller;
        }

        @Override
        protected RemoteLockResponse execute() {
            return target.remoteLock("test", caller, 1000);
        }
    }

    protected class LocalLockCaller extends AbstractCaller<Boolean> {
        private final AbstractClusterLockSupport target;
        private final long timeout;

        public LocalLockCaller(AbstractClusterLockSupport target, CountDownLatch startLatch, CountDownLatch proceedLatch, CountDownLatch finishLatch) {
            this(target, startLatch, proceedLatch, finishLatch, 3000);
        }

        public LocalLockCaller(AbstractClusterLockSupport target, CountDownLatch startLatch, CountDownLatch proceedLatch, CountDownLatch finishLatch, long timeout) {
            super(startLatch, proceedLatch, finishLatch);
            this.target = target;
            this.timeout = timeout;
        }

        @Override
        protected Boolean execute() {
            return Boolean.valueOf(target.lock("test", timeout));
        }
    }

}