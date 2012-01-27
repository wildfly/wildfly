/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.session;

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.as.clustering.web.ClusteringNotSupportedException;
import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.DistributedCacheManager;
import org.jboss.as.clustering.web.DistributedCacheManagerFactory;
import org.jboss.as.clustering.web.IncomingDistributableSessionData;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingAttributeGranularitySessionData;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.OutgoingSessionGranularitySessionData;
import org.jboss.as.web.WebLogger;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationCapability;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationCause;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationPolicy;
import org.jboss.as.web.session.notification.IgnoreUndeployLegacyClusteredSessionNotificationPolicy;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.PassivationConfig;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.jboss.metadata.web.jboss.SnapshotMode;

/**
 * @author Paul Ferraro
 */
public class DistributableSessionManager<O extends OutgoingDistributableSessionData> extends AbstractSessionManager implements LocalDistributableSessionManager, ClusteredSessionManager<O>, DistributableSessionManagerMBean, LifecycleListener {
    private static final String info = "DistributableSessionManager/1.0";

    private static final int TOTAL_PERMITS = Integer.MAX_VALUE;

    private final String name;
    private final String hostName;
    private final String contextName;
    private final DistributedCacheManager<O> distributedCacheManager;

    private SnapshotManager snapshotManager;

    private final ReplicationConfig replicationConfig;

    private ClusteredSessionNotificationPolicy notificationPolicy;
    private final OutdatedSessionChecker outdatedSessionChecker = new AskSessionOutdatedSessionChecker();
    private final Semaphore semaphore = new Semaphore(TOTAL_PERMITS, true);
    private final Lock valveLock = new SemaphoreLock(this.semaphore);
    /** Number of passivated sessions */
    private final AtomicInteger passivatedCount = new AtomicInteger();
    /** Maximum number of concurrently passivated sessions */
    private final AtomicInteger maxPassivatedCount = new AtomicInteger();
    /**
     * Session passivation flag set in jboss-web.xml by the user. If true, then the session passivation is enabled for this web
     * application, otherwise, it's disabled
     */
    private final boolean passivate;
    /**
     * Min time (milliseconds) the session must be idle since lastAccesstime before it's eligible for passivation if passivation
     * is enabled and more than maxActiveAllowed sessions are in memory. Setting to -1 means it's ignored.
     */
    private final long passivationMinIdleTime;

    /**
     * Max time (milliseconds) the session must be idle since lastAccesstime before it will be passivated if passivation is
     * enabled. Setting to -1 means session should not be forced out.
     */
    private final long passivationMaxIdleTime;

    private volatile int maxUnreplicatedInterval;

    /** Id/timestamp of sessions in distributedcache that we haven't loaded locally */
    private final Map<String, OwnedSessionUpdate> unloadedSessions = new ConcurrentHashMap<String, OwnedSessionUpdate>();
    /** Sessions that have been created but not yet loaded. Used to ensure concurrent threads trying to load the same session */
    private final ConcurrentMap<String, ClusteredSession<O>> embryonicSessions = new ConcurrentHashMap<String, ClusteredSession<O>>();

    public DistributableSessionManager(DistributedCacheManagerFactory factory, Context context, JBossWebMetaData metaData) throws ClusteringNotSupportedException {
        super(metaData);

        PassivationConfig passivationConfig = metaData.getPassivationConfig();
        Boolean useSessionPassivation = (passivationConfig != null) ? passivationConfig.getUseSessionPassivation() : null;
        this.passivate = (useSessionPassivation != null) ? useSessionPassivation.booleanValue() : false;
        Integer minIdleTime = (passivationConfig != null) ? passivationConfig.getPassivationMinIdleTime() : null;
        this.passivationMinIdleTime = (minIdleTime != null) && this.passivate ? minIdleTime.intValue() : -1;
        Integer maxIdleTime = (passivationConfig != null) ? passivationConfig.getPassivationMaxIdleTime() : null;
        this.passivationMaxIdleTime = (maxIdleTime != null) && this.passivate ? maxIdleTime.intValue() : -1;

        ReplicationConfig config = metaData.getReplicationConfig();
        this.replicationConfig = (config != null) ? config : new ReplicationConfig();

        if (this.replicationConfig.getReplicationGranularity() == ReplicationGranularity.FIELD) {
            this.replicationConfig.setReplicationGranularity(ReplicationGranularity.SESSION);
        }

        Integer interval = this.replicationConfig.getMaxUnreplicatedInterval();
        this.maxUnreplicatedInterval = (interval != null) ? interval.intValue() : -1;

        this.notificationPolicy = this.createClusteredSessionNotificationPolicy();

        String host = context.getParent().getName();
        this.hostName = (host == null) ? "localhost" : host;
        this.contextName = context.getName();
        this.name = String.format("//%s/%s", this.hostName, this.contextName);
        this.distributedCacheManager = factory.getDistributedCacheManager(this);
    }

    @Override
    public String getHostName() {
        return this.hostName;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public synchronized void start() throws LifecycleException {
        // Identify ourself more clearly
        this.log = Logger.getLogger(getClass().getName() + "." + getContainer().getName().replaceAll("/", ""));

        super.start();

        this.notificationPolicy = this.createClusteredSessionNotificationPolicy();

        // Start the DistributedCacheManager
        // Will need to pass the classloader that is associated with this
        // web app so de-serialization will work correctly.
        try {
            this.distributedCacheManager.start();

            initializeUnloadedSessions();

            // Setup our SnapshotManager
            this.snapshotManager = createSnapshotManager();
            this.snapshotManager.start();

            // Add SnapshotValve and, if needed, JvmRouteValve and batch repl valve
            installValves();

            log.debug("start(): DistributedCacheManager started");
        } catch (Exception e) {
            throw new LifecycleException(MESSAGES.failToStartManager(), e);
        }

        Container container = this.getContainer();
        if (container instanceof Lifecycle) {
            Lifecycle lifecycle = (Lifecycle) container;
            LifecycleListener[] listeners = lifecycle.findLifecycleListeners();

            // Remove existing listeners
            for (LifecycleListener listener : listeners) {
                lifecycle.removeLifecycleListener(listener);
            }

            // Register our listener first
            lifecycle.addLifecycleListener(this);

            // Re-register the old listeners
            for (LifecycleListener listener : listeners) {
                lifecycle.addLifecycleListener(listener);
            }
        }

        // Handle re-entrance
        if (!this.semaphore.tryAcquire()) {
            log.debug("Opening up LockingValve");

            // Make all permits available to locking valve
            this.semaphore.release(TOTAL_PERMITS);
        } else {
            // Release the one we just acquired
            this.semaphore.release();
        }
    }

    /**
     * Instantiate a SnapshotManager and ClusteredSessionValve and add the valve to our parent Context's pipeline. Add a
     * JvmRouteValve and BatchReplicationClusteredSessionValve if needed.
     */
    protected void installValves() {
        log.debug("Adding LockingValve");
        this.installContextValve(new LockingValve(this.valveLock));

        if (this.getUseJK()) {
            log.debug("We are using JK for load-balancing. Adding JvmRouteValve.");
            this.installContextValve(new JvmRouteValve(this));
        }

        // Add clustered session valve
        ClusteredSessionValve valve = new ClusteredSessionValve(this, null);
        log.debug("Adding ClusteredSessionValve");
        this.installContextValve(valve);
    }

    private void installContextValve(Valve valve) {
        if (this.container instanceof Pipeline) {
            ((Pipeline) this.container).addValve(valve);
        } else {
            // No choice; have to add it to the context's pipeline
            this.container.getPipeline().addValve(valve);
        }
    }

    protected SnapshotManager createSnapshotManager() {
        String ctxPath = ((Context) this.container).getPath();
        switch (this.getSnapshotMode()) {
            case INTERVAL: {
                int interval = this.getSnapshotInterval();
                if (interval > 0) {
                    return new IntervalSnapshotManager(this, ctxPath, interval);
                }
                WebLogger.WEB_SESSION_LOGGER.invalidSnapshotInterval();
            }
            case INSTANT: {
                return new InstantSnapshotManager(this, ctxPath);
            }
            default: {
                throw MESSAGES.invalidSnapshotMode();
            }
        }
    }

    /**
     * Gets the ids of all sessions in the distributed cache and adds them to the unloaded sessions map, along with their
     * lastAccessedTime and their maxInactiveInterval. Passivates overage or excess sessions.
     */
    protected void initializeUnloadedSessions() {
        Map<String, String> sessions = this.distributedCacheManager.getSessionIds();
        if (sessions != null) {
            boolean passivate = isPassivationEnabled();

            long passivationMax = passivationMaxIdleTime * 1000L;
            long passivationMin = passivationMinIdleTime * 1000L;

            for (Map.Entry<String, String> entry : sessions.entrySet()) {
                String realId = entry.getKey();
                String owner = entry.getValue();

                long ts = -1;
                DistributableSessionMetadata md = null;
                try {
                    IncomingDistributableSessionData sessionData = this.distributedCacheManager.getSessionData(realId, owner, false);
                    if (sessionData == null) {
                        log.debugf("Metadata unavailable for unloaded session %s", realId);
                        continue;
                    }
                    ts = sessionData.getTimestamp();
                    md = sessionData.getMetadata();
                } catch (Exception e) {
                    // most likely a lock conflict if the session is being updated remotely;
                    // ignore it and use default values for timstamp and maxInactive
                    log.debug("Problem reading metadata for session " + realId + " -- " + e.toString(), e);
                }

                long lastMod = ts == -1 ? System.currentTimeMillis() : ts;
                int maxLife = md == null ? getMaxInactiveInterval() : md.getMaxInactiveInterval();

                OwnedSessionUpdate osu = new OwnedSessionUpdate(owner, lastMod, maxLife, false);
                unloadedSessions.put(realId, osu);
            }

            if (passivate) {
                for (Map.Entry<String, OwnedSessionUpdate> entry : unloadedSessions.entrySet()) {
                    String realId = entry.getKey();
                    OwnedSessionUpdate osu = entry.getValue();
                    try {
                        long elapsed = System.currentTimeMillis() - osu.getUpdateTime();
                        // if maxIdle time configured, means that we need to passivate sessions that have
                        // exceeded the max allowed idle time
                        if (passivationMax >= 0 && elapsed > passivationMax) {
                            log.tracef("Elapsed time of %d for session %s exceeds max of %d; passivating", elapsed, realId, passivationMax);
                            processUnloadedSessionPassivation(realId, osu);
                        }
                        // If the session didn't exceed the passivationMaxIdleTime_, see
                        // if the number of sessions managed by this manager greater than the max allowed
                        // active sessions, passivate the session if it exceed passivationMinIdleTime_
                        else if ((maxActiveAllowed > 0) && (passivationMin >= 0) && (calcActiveSessions() > maxActiveAllowed) && (elapsed >= passivationMin)) {
                            log.tracef("Elapsed time of %d for session %s exceeds min of %d; passivating", elapsed, realId, passivationMin);
                            processUnloadedSessionPassivation(realId, osu);
                        }
                    } catch (Exception e) {
                        // most likely a lock conflict if the session is being updated remotely; ignore it
                        log.debugf("Problem passivating session %s -- %s", realId, e);
                    }
                }
            }
        }
    }

    /**
     * Session passivation logic for sessions only in the distributed store.
     *
     * @param realId the session id, minus any jvmRoute
     */
    private void processUnloadedSessionPassivation(String realId, OwnedSessionUpdate osu) {
        log.tracef("Passivating session with id: %s", realId);

        this.distributedCacheManager.evictSession(realId, osu.getOwner());
        osu.setPassivated(true);
        sessionPassivated();
    }

    private void sessionPassivated() {
        int pc = passivatedCount.incrementAndGet();
        int max = maxPassivatedCount.get();
        while (pc > max) {
            if (!maxPassivatedCount.compareAndSet(max, pc)) {
                max = maxPassivatedCount.get();
            }
        }
    }

    protected ClusteredSessionNotificationPolicy createClusteredSessionNotificationPolicy() {
        String policyClass = this.replicationConfig.getSessionNotificationPolicy();
        if (policyClass == null || policyClass.isEmpty()) {
            policyClass = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty("jboss.web.clustered.session.notification.policy", IgnoreUndeployLegacyClusteredSessionNotificationPolicy.class.getName());
                }
            });
        }

        try {
            ClusteredSessionNotificationPolicy policy = loadClass(policyClass, ClusteredSessionNotificationPolicy.class).newInstance();
            policy.setClusteredSessionNotificationCapability(new ClusteredSessionNotificationCapability());
            return policy;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw MESSAGES.failToCreateSessionNotificationPolicy(ClusteredSessionNotificationPolicy.class.getName(), policyClass, e);
        }
    }

    private static <T> Class<? extends T> loadClass(String className, Class<T> targetClass) throws Exception {
        Exception lastException = new IllegalStateException();
        for (ClassLoader loader: Arrays.asList(Thread.currentThread().getContextClassLoader(), DistributableSessionManager.class.getClassLoader())) {
            if (loader != null) {
                try {
                    return loader.loadClass(className).asSubclass(targetClass);
                } catch (ClassNotFoundException e) {
                    lastException = e;
                }
            }
        }
        throw lastException;
    }

    @Override
    public void stop() throws LifecycleException {
        log.debug("Stopping");
        // Validate and update our current component state
        if (!this.started)  return;

        this.started = false;
        synchronized (this) {
            log.trace("Waiting until backgroundProcess() short-circuits.");
        }

        Container container = this.getContainer();
        if (container instanceof Lifecycle) {
            ((Lifecycle) container).removeLifecycleListener(this);
        }

        // Handle re-entrance
        if (this.semaphore.tryAcquire()) {
            try {
                log.debug("Closing off LockingValve");

                // Acquire all remaining permits, shutting off locking valve
                this.semaphore.acquire(TOTAL_PERMITS - 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.semaphore.release();

                throw new LifecycleException(e);
            }
        }

        resetStats();

        clearSessions();

        this.distributedCacheManager.stop();

        this.snapshotManager.stop();
        this.snapshotManager = null;

        // Clean up maps
        this.sessions.clear();
        this.unloadedSessions.clear();

        this.passivatedCount.set(0);

        // Notify our interested LifecycleListeners
        this.lifecycle.fireLifecycleEvent(STOP_EVENT, this);

        this.destroy();
    }

    /**
     * Clear the underlying cache store.
     */
    private void clearSessions() {
        boolean passivation = isPassivationEnabled();
        // First, the sessions we have actively loaded
        for (Session session: this.sessions.values()) {
            ClusteredSession<O> ses = cast(session);

            log.tracef("clearSessions(): clear session by expiring or passivating: %s", ses);

            try {
                // if session passivation is enabled, passivate sessions instead of expiring them which means
                // they'll be available to the manager for activation after a restart.
                if (passivation && ses.isValid()) {
                    processSessionPassivation(ses.getRealId());
                } else {
                    boolean notify = true;
                    boolean localCall = true;
                    boolean localOnly = true;
                    ses.expire(notify, localCall, localOnly, ClusteredSessionNotificationCause.UNDEPLOY);
                }
            } catch (Throwable t) {
                log.warn(MESSAGES.errorPassivatingSession(ses.getIdInternal()), t);
            } finally {
                // Guard against leaking memory if anything is holding a
                // ref to the session by clearing its internal state
                ses.recycle();
            }
        }

        Set<Map.Entry<String, OwnedSessionUpdate>> unloaded = unloadedSessions.entrySet();
        for (Iterator<Map.Entry<String, OwnedSessionUpdate>> it = unloaded.iterator(); it.hasNext();) {
            Map.Entry<String, OwnedSessionUpdate> entry = it.next();
            String realId = entry.getKey();
            try {
                if (passivation) {
                    OwnedSessionUpdate osu = entry.getValue();
                    // Ignore the marker entries for our passivated sessions
                    if (!osu.isPassivated()) {
                        this.distributedCacheManager.evictSession(realId, osu.getOwner());
                    }
                } else {
                    this.distributedCacheManager.removeSessionLocal(realId);
                }
            } catch (Exception e) {
                // Not as big a problem; we don't own the session
                log.debugf("Problem %s session %s -- %s", passivation ? "evicting" : "removing", realId, e);
            }
            it.remove();
        }
    }

    /**
     * Session passivation logic for an actively managed session.
     *
     * @param realId the session id, minus any jvmRoute
     */
    private void processSessionPassivation(String realId) {
        // get the session from the local map
        ClusteredSession<O> session = cast(this.sessions.get(realId));
        // Remove actively managed session and add to the unloaded sessions
        // if it's already unloaded session (session == null) don't do anything,
        if (session != null) {
            synchronized (session) {
                log.tracef("Passivating session with id: %s", realId);

                session.notifyWillPassivate(ClusteredSessionNotificationCause.PASSIVATION);
                this.distributedCacheManager.evictSession(realId);
                sessionPassivated();

                // Put the session in the unloadedSessions map. This will
                // expose the session to regular invalidation.
                Object obj = unloadedSessions.put(realId, new OwnedSessionUpdate(null, session.getLastAccessedTimeInternal(), session.getMaxInactiveInterval(), true));
                if (obj == null) {
                    log.tracef("New session %s added to unloaded session map", realId);
                } else {
                    log.tracef("Updated timestamp for unloaded session %s", realId);
                }
                sessions.remove(realId);
            }
        } else {
            log.tracef("processSessionPassivation():  could not find session %s", realId);
        }
    }

    @Override
    public Session createSession(String sessionId, Random random) {
        Session session = null;
        try {
            // [JBAS-7123] Make sure we're either in the call stack where LockingValve has
            // a lock, or that we acquire one ourselves
            boolean inLockingValve = SessionReplicationContext.isLocallyActive();
            if (inLockingValve || this.valveLock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    session = createSessionInternal(sessionId, random);
                } finally {
                    if (!inLockingValve) {
                        this.valveLock.unlock();
                    }
                }
            } else {
                log.trace("createEmptySession(): Manager is not handling requests; returning null");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return session;
    }

    private ClusteredSession<O> createSessionInternal(String sessionId, Random random) {
        // First check if we've reached the max allowed sessions,
        // then try to expire/passivate sessions to free memory
        // maxActiveAllowed -1 is unlimited
        // We check here for maxActive instead of in add(). add() gets called
        // when we load an already existing session from the distributed cache
        // (e.g. in a failover) and we don't want to fail in that situation.

        if (maxActiveAllowed != -1 && calcActiveSessions() >= maxActiveAllowed) {
            log.tracef("createSession(): active sessions = %d and max allowed sessions = %d", calcActiveSessions(), maxActiveAllowed);

            processExpires();

            if (calcActiveSessions() >= maxActiveAllowed) {
                // Exceeds limit. We need to reject it.
                rejectedCounter.incrementAndGet();
                // Catalina api does not specify what happens
                // but we will throw a runtime exception for now.
                String msgEnd = (sessionId == null) ? "" : " id " + sessionId;
                throw MESSAGES.tooManyActiveSessions(maxActiveAllowed, msgEnd);
            }
        }

        ClusteredSession<O> session = createEmptyClusteredSession();

        if (session != null) {
            session.setNew(true);
            session.setCreationTime(System.currentTimeMillis());
            session.setMaxInactiveInterval(this.maxInactiveInterval);
            session.setValid(true);

            String clearInvalidated = null; // see below

            if (sessionId == null) {
                sessionId = this.generateSessionId(random);
            } else {
                clearInvalidated = sessionId;
            }

            session.setId(sessionId); // Setting the id leads to a call to add()

            getDistributedCacheManager().sessionCreated(session.getRealId());

            session.tellNew(ClusteredSessionNotificationCause.CREATE);

            log.tracef("Created a ClusteredSession with id: %s", sessionId);

            createdCounter.incrementAndGet(); // the call to add() handles the other counters

            // Add this session to the set of those potentially needing replication
            SessionReplicationContext.bindSession(session, snapshotManager);

            if (clearInvalidated != null) {
                // We no longer need to track any earlier session w/ same id
                // invalidated by this thread
                SessionInvalidationTracker.clearInvalidatedSession(clearInvalidated, this);
            }
        }

        return session;
    }

    @Override
    protected boolean appendJVMRoute() {
        return this.getUseJK();
    }

    @Override
    public ClusteredSession<O> createEmptySession() {
        try {
            // [JBAS-7123] Make sure we're either in the call stack where LockingValve has
            // a lock, or that we acquire one ourselves
            boolean inLockingValve = SessionReplicationContext.isLocallyActive();
            if (inLockingValve || this.valveLock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    log.trace("Creating an empty ClusteredSession");

                    return createEmptyClusteredSession();
                } finally {
                    if (!inLockingValve) {
                        this.valveLock.unlock();
                    }
                }
            } else {
                log.trace("createEmptySession(): Manager is not handling requests; returning null");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return null;
    }

    @Override
    public ClusteredSession<O> findSession(String id) {
        String realId = this.parse(id).getKey();
        // Find it from the local store first
        ClusteredSession<O> session = cast(this.sessions.get(realId));
        // If we didn't find it locally, only check the distributed cache
        // if we haven't previously handled this session id on this request.
        // If we handled it previously but it's no longer local, that means
        // it's been invalidated. If we request an invalidated session from
        // the distributed cache, it will be missing from the local cache but
        // may still exist on other nodes (i.e. if the invalidation hasn't
        // replicated yet because we are running in a tx). With buddy replication,
        // asking the local cache for the session will cause the out-of-date
        // session from the other nodes to be gravitated, thus resuscitating
        // the session.
        if (session == null && !SessionInvalidationTracker.isSessionInvalidated(realId, this)) {
            log.tracef("Checking for session %s in the distributed cache", realId);

            session = loadSession(realId);
            // if (session != null)
            // {
            // add(session);
            // // We now notify, since we've added a policy to allow listeners
            // // to discriminate. But the default policy will not allow the
            // // notification to be emitted for FAILOVER, so the standard
            // // behavior is unchanged.
            // session.tellNew(ClusteredSessionNotificationCause.FAILOVER);
            // }
        } else if (session != null && this.outdatedSessionChecker.isSessionOutdated(session)) {
            log.tracef("Updating session %s from the distributed cache", realId);

            // Need to update it from the cache
            session = loadSession(realId);
            if (session == null) {
                // We have a session locally but it's no longer available
                // from the distributed store; i.e. it's been invalidated elsewhere
                // So we need to clean up
                // TODO what about notifications?
                this.sessions.remove(realId);
            }
        }

        if (session != null) {
            // Add this session to the set of those potentially needing replication
            SessionReplicationContext.bindSession(session, snapshotManager);

            // If we previously called passivate() on the session due to
            // replication, we need to make an offsetting activate() call
            if (session.getNeedsPostReplicateActivation()) {
                session.notifyDidActivate(ClusteredSessionNotificationCause.REPLICATION);
            }
        }

        return session;
    }

    @Override
    public Session[] findSessions() {
        try {
            // [JBAS-7123] Make sure we're either in the call stack where LockingValve has
            // a lock, or that we acquire one ourselves
            boolean inLockingValve = SessionReplicationContext.isLocallyActive();
            if (inLockingValve || this.valveLock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    // Need to load all the unloaded sessions
                    if (unloadedSessions.size() > 0) {
                        // Make a thread-safe copy of the new id list to work with
                        Set<String> ids = new HashSet<String>(unloadedSessions.keySet());

                        log.tracef("findSessions: loading sessions from distributed cache: %s", ids);

                        for (String id : ids) {
                            loadSession(id);
                        }
                    }

                    // All sessions are now "local" so just return the local sessions
                    return this.sessions.values().toArray(new Session[0]);
                } finally {
                    if (!inLockingValve) {
                        this.valveLock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return null;
    }

    @Override
    public void remove(Session s) {
        ClusteredSession<O> session = cast(s);
        synchronized (session) {
            String realId = session.getRealId();
            if (realId == null)
                return;

            log.tracef("Removing session from store with id: %s", realId);

            try {
                session.removeMyself();
            } finally {
                // We don't want to replicate this session at the end
                // of the request; the removal process took care of that
                SessionReplicationContext.sessionExpired(session, realId, snapshotManager);

                // Track this session to prevent reincarnation by this request
                // from the distributed cache
                SessionInvalidationTracker.sessionInvalidated(realId, this);

                sessions.remove(realId);
                this.getReplicationStatistics().removeStats(realId);

                // Compute how long this session has been alive, and update
                // our statistics accordingly
                int timeAlive = (int) ((System.currentTimeMillis() - session.getCreationTimeInternal()) / 1000);
                sessionExpired(timeAlive);
            }
        }
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        // Force synchronous replication upon initiating undeploy
        // to ensure sessions get replicated prior to stopping context
        this.handleForceSynchronousNotification(event.getType(), Lifecycle.BEFORE_STOP_EVENT, Lifecycle.AFTER_STOP_EVENT);
    }

    private void handleForceSynchronousNotification(String type, String enableType, String disableType) {
        boolean enabled = type.equals(enableType);

        if (enabled || type.equals(disableType)) {
            if (this.distributedCacheManager != null) {
                this.distributedCacheManager.setForceSynchronous(enabled);
            }
        }
    }

    @Override
    public String locate(String sessionId) {
        return this.distributedCacheManager.locate(sessionId);
    }

    @Override
    public void removeLocal(Session s) {
        ClusteredSession<O> session = cast(s);
        synchronized (session) {
            String realId = session.getRealId();
            if (realId == null)
                return;

            log.tracef("Removing session from local store with id: %s", realId);

            try {
                session.removeMyselfLocal();
            } finally {
                // We don't want to replicate this session at the end
                // of the request; the removal process took care of that
                SessionReplicationContext.sessionExpired(session, realId, snapshotManager);

                // Track this session to prevent reincarnation by this request
                // from the distributed cache
                SessionInvalidationTracker.sessionInvalidated(realId, this);

                sessions.remove(realId);
                this.getReplicationStatistics().removeStats(realId);

                // Compute how long this session has been alive, and update
                // our statistics accordingly
                int timeAlive = (int) ((System.currentTimeMillis() - session.getCreationTimeInternal()) / 1000);
                this.sessionExpired(timeAlive);
            }
        }
    }

    @Override
    public boolean storeSession(Session s) {
        boolean stored = false;
        if (s != null) {
            ClusteredSession<O> session = cast(s);
            synchronized (session) {
                log.tracef("check to see if needs to store and replicate session with id %s ", session.getIdInternal());

                if (session.isValid() && (session.isSessionDirty() || session.getMustReplicateTimestamp())) {
                    String realId = session.getRealId();

                    // Notify all session attributes that they get serialized (SRV 7.7.2)
                    long begin = System.currentTimeMillis();
                    session.notifyWillPassivate(ClusteredSessionNotificationCause.REPLICATION);
                    long elapsed = System.currentTimeMillis() - begin;
                    ReplicationStatistics stats = this.getReplicationStatistics();
                    stats.updatePassivationStats(realId, elapsed);

                    // Do the actual replication
                    begin = System.currentTimeMillis();
                    processSessionRepl(session);
                    elapsed = System.currentTimeMillis() - begin;
                    stored = true;
                    stats.updateReplicationStats(realId, elapsed);
                } else {
                    log.tracef("Session %s did not require replication.", session.getIdInternal());
                }
            }
        }

        return stored;
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public void add(Session session) {
        if (session == null) return;
        try {
            // [JBAS-7123] Make sure we're either in the call stack where LockingValve has
            // a lock, or that we acquire one ourselves
            boolean inLockingValve = SessionReplicationContext.isLocallyActive();
            if (inLockingValve || this.valveLock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    add(this.cast(session), false); // wait to replicate until req end
                } finally {
                    if (!inLockingValve) {
                        this.valveLock.unlock();
                    }
                }
            } else {
                log.trace("add(): ignoring add -- Manager is not actively handling requests");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Adds the given session to the collection of those being managed by this Manager.
     *
     * @param session the session. Cannot be <code>null</code>.
     * @param replicate whether the session should be replicated
     *
     * @throws NullPointerException if <code>session</code> is <code>null</code>.
     */
    private void add(ClusteredSession<O> session, boolean replicate) {
        // TODO -- why are we doing this check? The request checks session
        // validity and will expire the session; this seems redundant
        if (!session.isValid()) {
            // Not an error; this can happen if a failover request pulls in an
            // outdated session from the distributed cache (see TODO above)
            log.debugf("Cannot add session with id=%s because it is invalid", session.getIdInternal());
            return;
        }

        String realId = session.getRealId();
        Object existing = sessions.put(realId, session);
        unloadedSessions.remove(realId);

        if (!session.equals(existing)) {
            if (replicate) {
                storeSession(session);
            }

            // Update counters
            calcActiveSessions();

            log.tracef("Session with id=%s added. Current active sessions %d", session.getIdInternal(), localActiveCounter.get());
        }
    }

    @Override
    public String getCacheConfigName() {
        return this.replicationConfig.getCacheName();
    }

    @Override
    public ReplicationGranularity getReplicationGranularity() {
        ReplicationGranularity granularity = this.replicationConfig.getReplicationGranularity();
        return (granularity != null) ? granularity : ReplicationGranularity.SESSION;
    }

    @Override
    public SnapshotMode getSnapshotMode() {
        SnapshotMode mode = this.replicationConfig.getSnapshotMode();
        return (mode != null) ? mode : SnapshotMode.INSTANT;
    }

    @Override
    public int getSnapshotInterval() {
        Integer interval = this.replicationConfig.getSnapshotInterval();
        return (interval != null) ? interval.intValue() : -1;
    }

    @Override
    public void setMaxUnreplicatedInterval(int maxUnreplicatedInterval) {
        this.maxUnreplicatedInterval = maxUnreplicatedInterval;
    }

    @Override
    public String listLocalSessionIds() {
        List<String> ids = new ArrayList<String>(sessions.size());
        this.addLocal(ids, sessions.keySet());
        return reportSessionIds(ids);
    }

    private void addLocal(Collection<String> localIds, Collection<String> ids) {
        for (String id : ids) {
            if (this.distributedCacheManager.isLocal(id)) {
                localIds.add(id);
            }
        }
    }

    private String reportSessionIds(Collection<String> sessions) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> ids = sessions.iterator();
        while (ids.hasNext()) {
            builder.append(ids.next());
            if (ids.hasNext()) {
                builder.append(',');
            }
        }
        return builder.toString();
    }

    @Override
    public long getPassivatedSessionCount() {
        return this.passivatedCount.get();
    }

    @Override
    public long getMaxPassivatedSessionCount() {
        return this.maxPassivatedCount.get();
    }

    @Override
    public long getPassivationMaxIdleTime() {
        return this.passivationMaxIdleTime;
    }

    @Override
    public long getPassivationMinIdleTime() {
        return this.passivationMinIdleTime;
    }

    @Override
    public int getMaxUnreplicatedInterval() {
        return this.maxUnreplicatedInterval;
    }

    @Override
    public ClusteredSessionNotificationPolicy getNotificationPolicy() {
        return this.notificationPolicy;
    }

    @Override
    public ReplicationTrigger getReplicationTrigger() {
        ReplicationTrigger trigger = this.replicationConfig.getReplicationTrigger();
        return (trigger != null) ? trigger : ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET;
    }

    @Override
    public boolean getUseJK() {
        Boolean useJK = this.replicationConfig.getUseJK();
        return (useJK != null) ? useJK : true;
    }

    @Override
    public DistributedCacheManager<O> getDistributedCacheManager() {
        return this.distributedCacheManager;
    }

    @Override
    public boolean isPassivationEnabled() {
        return this.passivate;
    }

    @Override
    public String getEngineName() {
        Engine engine = this.getEngine();
        return (engine != null) ? engine.getName() : null;
    }

    @Override
    public ClassLoader getApplicationClassLoader() {
        return this.getContainer().getLoader().getClassLoader();
    }

    @Override
    public ReplicationConfig getReplicationConfig() {
        return this.replicationConfig;
    }

    @Override
    public void notifyRemoteInvalidation(String realId) {
        // Remove the session from our local map
        ClusteredSession<O> session = cast(this.sessions.remove(realId));
        if (session == null) {
            // We weren't managing the session anyway. But remove it
            // from the list of cached sessions we haven't loaded
            if (unloadedSessions.remove(realId) != null) {
                log.tracef("Removed entry for session %s from unloaded session map", realId);
            }

            // If session has failed over and has been passivated here,
            // session will be null, but we'll have a TimeStatistic to clean up
            this.getReplicationStatistics().removeStats(realId);
        } else {
            // Expire the session
            // DON'T SYNCHRONIZE ON SESSION HERE -- isValid() and
            // expire() are meant to be multi-threaded and synchronize
            // properly internally; synchronizing externally can lead
            // to deadlocks!!
            boolean notify = false; // Don't notify listeners. SRV.10.7
                                    // allows this, and sending notifications
                                    // leads to all sorts of issues; e.g.
                                    // circular calls with ClusteredSSO and
                                    // notifying when all that's happening is
                                    // data gravitation due to random failover
            boolean localCall = false; // this call originated from the cache;
                                       // we have already removed session
            boolean localOnly = true; // Don't pass attr removals to cache

            try {
                // Don't track this invalidation is if it were from a request
                SessionInvalidationTracker.suspend();

                session.expire(notify, localCall, localOnly, ClusteredSessionNotificationCause.INVALIDATE);
            } finally {
                SessionInvalidationTracker.resume();

                // Remove any stats for this session
                this.getReplicationStatistics().removeStats(realId);
            }
        }
    }

    @Override
    public void notifyLocalAttributeModification(String realId) {
        ClusteredSession<O> session = cast(this.sessions.get(realId));
        if (session != null) {
            session.sessionAttributesDirty();
        } else {
            log.warn(MESSAGES.notificationForInactiveSession(realId));
        }
    }

    @Override
    public void sessionActivated() {
        int pc = passivatedCount.decrementAndGet();
        // Correct for drift since we don't know the true passivation
        // count when we started. We can get activations of sessions
        // we didn't know were passivated.
        // FIXME -- is the above statement still correct? Is this needed?
        if (pc < 0) {
            // Just reverse our decrement.
            passivatedCount.incrementAndGet();
        }
    }

    @Override
    public boolean sessionChangedInDistributedCache(String realId, String dataOwner, int distributedVersion, long timestamp, DistributableSessionMetadata metadata) {
        boolean updated = true;

        ClusteredSession<O> session = cast(this.sessions.get(realId));
        if (session != null) {
            // Need to invalidate the loaded session. We get back whether
            // this an actual version increment
            updated = session.setVersionFromDistributedCache(distributedVersion);
            if (updated) {
                log.tracef("session in-memory data is invalidated for id: %s new version: %d", realId, distributedVersion);
            }
        } else {
            int maxLife = metadata == null ? getMaxInactiveInterval() : metadata.getMaxInactiveInterval();

            Object existing = unloadedSessions.put(realId, new OwnedSessionUpdate(dataOwner, timestamp, maxLife, false));
            if (existing == null) {
                calcActiveSessions();
                log.tracef("New session %s added to unloaded session map", realId);
            } else {
                log.tracef("Updated timestamp for unloaded session %s", realId);
            }
        }

        return updated;
    }


    @Override
    protected void processExpirationPassivation() {
        boolean expire = maxInactiveInterval >= 0;
        boolean passivate = isPassivationEnabled();

        long passivationMax = passivationMaxIdleTime * 1000L;
        long passivationMin = passivationMinIdleTime * 1000L;

        log.trace("processExpirationPassivation(): Looking for sessions that have expired ...");
        log.tracef("processExpirationPassivation(): active sessions = %d", calcActiveSessions());
        log.tracef("processExpirationPassivation(): expired sessions = %d", expiredCounter.get());
        if (passivate) {
            log.tracef("processExpirationPassivation(): passivated count = %d", getPassivatedSessionCount());
        }

        // Holder for sessions or OwnedSessionUpdates that survive expiration,
        // sorted by last acccessed time
        TreeSet<PassivationCheck> passivationChecks = new TreeSet<PassivationCheck>();

        try {
            // Don't track sessions invalidated via this method as if they
            // were going to be re-requested by the thread
            SessionInvalidationTracker.suspend();

            // First, handle the sessions we are actively managing
            for (Session s: this.sessions.values()) {
                if (!this.started) return;

                boolean likelyExpired = false;
                String realId = null;

                try {
                    ClusteredSession<O> session = cast(s);

                    realId = session.getRealId();
                    likelyExpired = expire;

                    if (expire) {
                        // JBAS-2403. Check for outdated sessions where we think
                        // the local copy has timed out. If found, refresh the
                        // session from the cache in case that might change the timeout
                        likelyExpired = (session.isValid(false) == false);
                        if (likelyExpired && this.outdatedSessionChecker.isSessionOutdated(session)) {
                            // With JBC, every time we get a notification from the distributed
                            // cache of an update, we get the latest timestamp. So
                            // we shouldn't need to do a full session load here. A load
                            // adds a risk of an unintended data gravitation. However,
                            // with a database instead of JBC we don't get notifications

                            // JBAS-2792 don't assign the result of loadSession to session
                            // just update the object from the cache or fall through if
                            // the session has been removed from the cache
                            loadSession(session.getRealId());
                        }

                        // Do a normal invalidation check that will expire the
                        // session if it has timed out
                        // DON'T SYNCHRONIZE on session here -- isValid() and
                        // expire() are meant to be multi-threaded and synchronize
                        // properly internally; synchronizing externally can lead
                        // to deadlocks!!
                        if (!session.isValid())
                            continue;

                        likelyExpired = false;
                    }

                    // we now have a valid session; store it so we can check later
                    // if we need to passivate it
                    if (passivate) {
                        passivationChecks.add(new PassivationCheck(session));
                    }

                } catch (Exception e) {
                    if (likelyExpired) {
                        // JBAS-7397 clean up
                        bruteForceCleanup(realId, e);
                    } else {
                        log.error(MESSAGES.failToPassivateLoad(realId), e);
                    }

                }
            }

            // Next, handle any unloaded sessions

            // We may have not gotten replication of a timestamp for requests
            // that occurred w/in maxUnreplicatedInterval of the previous
            // request. So we add a grace period to avoid flushing a session early
            // and permanently losing part of its node structure in JBoss Cache.
            long maxUnrep = maxUnreplicatedInterval < 0 ? 60 : maxUnreplicatedInterval;

            for (Map.Entry<String, OwnedSessionUpdate> entry : this.unloadedSessions.entrySet()) {
                if (!this.started) return;

                String realId = entry.getKey();
                OwnedSessionUpdate osu = entry.getValue();
                boolean likelyExpired = false;

                long now = System.currentTimeMillis();
                long elapsed = (now - osu.getUpdateTime());
                try {
                    likelyExpired = expire && osu.getMaxInactive() >= 1 && elapsed >= (osu.getMaxInactive() + maxUnrep) * 1000L;
                    if (likelyExpired) {
                        // if (osu.passivated && osu.owner == null)
                        if (osu.isPassivated()) {
                            // Passivated session needs to be expired. A call to
                            // findSession will bring it out of passivation
                            Session session = findSession(realId);
                            if (session != null) {
                                session.isValid(); // will expire
                                continue;
                            }
                        }

                        // If we get here either !osu.passivated, or we don't own
                        // the session or the session couldn't be reactivated (invalidated by user).
                        // Either way, do a cleanup
                        this.distributedCacheManager.removeSessionLocal(realId, osu.getOwner());
                        unloadedSessions.remove(realId);
                        this.getReplicationStatistics().removeStats(realId);

                    } else if (passivate && !osu.isPassivated()) {
                        // we now have a valid session; store it so we can check later
                        // if we need to passivate it
                        passivationChecks.add(new PassivationCheck(realId, osu));
                    }
                } catch (Exception e) {
                    // JBAS-7397 Don't try forever
                    if (likelyExpired) {
                        // JBAS-7397
                        bruteForceCleanup(realId, e);
                    } else {
                        log.error(MESSAGES.failToPassivateUnloaded(realId), e);
                    }
                }
            }

            if (!this.started) return;

            // Now, passivations
            if (passivate) {
                // Iterate through sessions, earliest lastAccessedTime to latest
                for (PassivationCheck passivationCheck : passivationChecks) {
                    try {
                        long timeNow = System.currentTimeMillis();
                        long timeIdle = timeNow - passivationCheck.getLastUpdate();
                        // if maxIdle time configured, means that we need to passivate sessions that have
                        // exceeded the max allowed idle time
                        if (passivationMax >= 0 && timeIdle > passivationMax) {
                            passivationCheck.passivate();
                        }
                        // If the session didn't exceed the passivationMaxIdleTime_, see
                        // if the number of sessions managed by this manager greater than the max allowed
                        // active sessions, passivate the session if it exceed passivationMinIdleTime_
                        else if ((maxActiveAllowed > 0) && (passivationMin > 0) && (calcActiveSessions() >= maxActiveAllowed) && (timeIdle > passivationMin)) {
                            passivationCheck.passivate();
                        } else {
                            // the entries are ordered by lastAccessed, so once
                            // we don't passivate one, we won't passivate any
                            break;
                        }
                    } catch (Exception e) {
                        log.error(MESSAGES.failToPassivate(passivationCheck.isUnloaded() ? "unloaded " : "", passivationCheck.getRealId()), e);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("processExpirationPassivation(): failed with exception: " + ex, ex);
        } finally {
            SessionInvalidationTracker.resume();
        }

        log.trace("processExpirationPassivation(): Completed ...");
        log.tracef("processExpirationPassivation(): active sessions = %d", calcActiveSessions());
        log.tracef("processExpirationPassivation(): expired sessions = %d", expiredCounter.get());
        if (passivate) {
            log.tracef("processExpirationPassivation(): passivated count = %d", getPassivatedSessionCount());
        }
    }

    /**
     * Loads a session from the distributed store. If an existing session with the id is already under local management, that
     * session's internal state will be updated from the distributed store. Otherwise a new session will be created and added to
     * the collection of those sessions under local management.
     *
     * @param realId id of the session-id with any jvmRoute removed
     *
     * @return the session or <code>null</code> if the session cannot be found in the distributed store
     */
    private ClusteredSession<O> loadSession(String realId) {
        if (realId == null) return null;

        try {
            // [JBAS-7123] Make sure we're either in the call stack where LockingValve has
            // a lock, or that we acquire one ourselves
            boolean inLockingValve = SessionReplicationContext.isLocallyActive();
            if (inLockingValve || this.valveLock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    long begin = System.currentTimeMillis();
                    boolean mustAdd = false;
                    boolean passivated = false;

                    ClusteredSession<O> session = cast(this.sessions.get(realId));
                    boolean initialLoad = false;
                    if (session == null) {
                        initialLoad = true;
                        // This is either the first time we've seen this session on this
                        // server, or we previously expired it and have since gotten
                        // a replication message from another server
                        mustAdd = true;
                        session = createEmptyClusteredSession();

                        // JBAS-7379 Ensure concurrent threads trying to load same session id
                        // use the same session
                        ClusteredSession<O> embryo = this.embryonicSessions.putIfAbsent(realId, session);
                        if (embryo != null) {
                            session = embryo;
                        }

                        OwnedSessionUpdate osu = unloadedSessions.get(realId);
                        passivated = (osu != null && osu.isPassivated());
                    }

                    synchronized (session) {
                        // JBAS-7379 check if we lost the race to the sync block
                        // and another thread has already loaded this session
                        if (initialLoad && session.isOutdated() == false) {
                            // some one else loaded this
                            return session;
                        }

                        IncomingDistributableSessionData data = this.distributedCacheManager.getSessionData(realId, initialLoad);
                        if (data != null) {
                            session.update(data);
                        } else {
                            // Clunky; we set the session variable to null to indicate
                            // no data so move on
                            session = null;
                        }

                        if (session != null) {
                            ClusteredSessionNotificationCause cause = passivated ? ClusteredSessionNotificationCause.ACTIVATION
                                    : ClusteredSessionNotificationCause.FAILOVER;
                            session.notifyDidActivate(cause);
                        }

                        if (session != null) {
                            if (mustAdd) {
                                add(session, false); // don't replicate
                                if (!passivated) {
                                    session.tellNew(ClusteredSessionNotificationCause.FAILOVER);
                                }
                            }
                            long elapsed = System.currentTimeMillis() - begin;
                            this.getReplicationStatistics().updateLoadStats(realId, elapsed);

                            log.tracef("loadSession(): id=%s, session=%s", realId, session);
                        } else {
                            log.tracef("loadSession(): session %s not found in distributed cache", realId);
                        }

                        if (initialLoad) {
                            // The session is now in the regular map, or the session
                            // doesn't exist in the distributed cache. either way
                            // it's now safe to stop tracking this embryonic session
                            embryonicSessions.remove(realId);
                        }
                    }

                    return session;
                } finally {
                    if (!inLockingValve) {
                        this.valveLock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ClusteredSession<O> createEmptyClusteredSession() {
        try {
            // [JBAS-7123] Make sure we're either in the call stack where LockingValve has
            // a lock, or that we acquire one ourselves
            boolean inLockingValve = SessionReplicationContext.isLocallyActive();
            if (inLockingValve || this.valveLock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    switch (this.getReplicationGranularity()) {
                        case ATTRIBUTE:
                            return (ClusteredSession<O>) new AttributeBasedClusteredSession((ClusteredSessionManager<OutgoingAttributeGranularitySessionData>) this);
                        default:
                            return (ClusteredSession<O>) new SessionBasedClusteredSession((ClusteredSessionManager<OutgoingSessionGranularitySessionData>) this);
                    }
                } finally {
                    if (!inLockingValve) {
                        this.valveLock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private void bruteForceCleanup(String realId, Exception ex) {
        log.warn(MESSAGES.bruteForceCleanup(realId, ex.getLocalizedMessage()));
        try {
            this.distributedCacheManager.removeSessionLocal(realId, null);
        } catch (Exception e) {
            log.error(MESSAGES.failToBruteForceCleanup(realId), e);
        } finally {
            // Get rid of our refs even if distributed store fails
            unloadedSessions.remove(realId);
            this.getReplicationStatistics().removeStats(realId);
        }
    }

    @Override
    public Entry<String, String> parse(String sessionId) {
        return this.getUseJK() ? super.parse(sessionId) : new AbstractMap.SimpleImmutableEntry<String, String>(sessionId, null);
    }

    @Override
    public String createSessionId(String realId, String jvmRoute) {
        return this.getUseJK() ? super.createSessionId(realId, jvmRoute) : realId;
    }

    @Override
    protected int getTotalActiveSessions() {
        return localActiveCounter.get() + unloadedSessions.size() - passivatedCount.get();
    }

    /**
     * Places the current session contents in the distributed cache and replicates them to the cluster
     *
     * @param session the session. Cannot be <code>null</code>.
     */
    private void processSessionRepl(ClusteredSession<O> session) {
        boolean endBatch = false;
        BatchingManager batchingManager = this.distributedCacheManager.getBatchingManager();
        try {
            // We need transaction so all the replication are sent in batch.
            // Don't do anything if there is already transaction context
            // associated with this thread.
            if (!batchingManager.isBatchInProgress()) {
                batchingManager.startBatch();
                endBatch = true;
            }

            session.processSessionReplication();
        } catch (Exception ex) {
            log.debug("processSessionRepl(): failed with exception", ex);

            RuntimeException exception = null;
            try {
                batchingManager.setBatchRollbackOnly();
            } catch (RuntimeException e) {
                exception = e;
            } catch (Exception e) {
                exception = MESSAGES.failedSessionReplication(e);
            }
            if (exception != null) {
                log.error(MESSAGES.exceptionRollingBackTransaction(), exception);
                throw exception;
            }
        } finally {
            if (endBatch) {
                batchingManager.endBatch();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ClusteredSession<O> cast(Session session) {
        if (session == null) return null;
        if (!(session instanceof ClusteredSession)) {
            throw MESSAGES.invalidSession(getClass().getName());
        }
        return (ClusteredSession<O>) session;
    }

    private class PassivationCheck implements Comparable<PassivationCheck> {
        private final String realId;
        private final OwnedSessionUpdate osu;
        private final ClusteredSession<O> session;

        private PassivationCheck(String realId, OwnedSessionUpdate osu) {
            assert osu != null : MESSAGES.nullOsu();
            assert realId != null : MESSAGES.nullSessionId();

            this.realId = realId;
            this.osu = osu;
            this.session = null;
        }

        private PassivationCheck(ClusteredSession<O> session) {
            assert session != null : MESSAGES.nullSession();

            this.realId = session.getRealId();
            this.session = session;
            this.osu = null;
        }

        private long getLastUpdate() {
            return osu == null ? session.getLastAccessedTimeInternal() : osu.getUpdateTime();
        }

        private void passivate() {
            if (osu == null) {
                DistributableSessionManager.this.processSessionPassivation(realId);
            } else {
                DistributableSessionManager.this.processUnloadedSessionPassivation(realId, osu);
            }
        }

        private String getRealId() {
            return realId;
        }

        private boolean isUnloaded() {
            return osu != null;
        }

        // This is what causes sorting based on lastAccessed
        @Override
        public int compareTo(PassivationCheck o) {
            long thisVal = getLastUpdate();
            long anotherVal = o.getLastUpdate();
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }
    }

    private static class SemaphoreLock implements Lock {
        private final Semaphore semaphore;

        SemaphoreLock(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        /**
         * @see java.util.concurrent.locks.Lock#lock()
         */
        @Override
        public void lock() {
            this.semaphore.acquireUninterruptibly();
        }

        /**
         * @see java.util.concurrent.locks.Lock#lockInterruptibly()
         */
        @Override
        public void lockInterruptibly() throws InterruptedException {
            this.semaphore.acquire();
        }

        /**
         * @see java.util.concurrent.locks.Lock#newCondition()
         */
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.concurrent.locks.Lock#tryLock()
         */
        @Override
        public boolean tryLock() {
            return this.semaphore.tryAcquire();
        }

        /**
         * @see java.util.concurrent.locks.Lock#tryLock(long, java.util.concurrent.TimeUnit)
         */
        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return this.semaphore.tryAcquire(timeout, unit);
        }

        /**
         * @see java.util.concurrent.locks.Lock#unlock()
         */
        @Override
        public void unlock() {
            this.semaphore.release();
        }
    }
}
