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

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Response;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.util.LifecycleSupport;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractSessionManager extends ManagerBase implements SessionManager, SessionManagerMBean, Lifecycle {
    /** Maximum of active sessions allowed. -1 is unlimited. */
    protected int maxActiveAllowed = -1;
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);
    /** Has this component been started yet? */
    protected volatile boolean started = false;
    /** Are we allowing backgroundProcess() to execute? We use an object so stop() can lock on it to wait for */
//    protected AtomicBoolean backgroundProcessAllowed = new AtomicBoolean();
    private ReplicationStatistics stats = new ReplicationStatistics();
    /** Number of sessions created by this manager */
    protected AtomicInteger createdCounter = new AtomicInteger();
    /** number of sessions rejected because the number active sessions exceeds maxActive */
    protected AtomicInteger rejectedCounter = new AtomicInteger();
    /** Number of active sessions */
    protected AtomicInteger localActiveCounter = new AtomicInteger();
    /** Maximum number of concurrently locally active sessions */
    protected AtomicInteger maxLocalActiveCounter = new AtomicInteger();
    /** Maximum number of active sessions seen so far */
    protected AtomicInteger maxActiveCounter = new AtomicInteger();
    /** Number of sessions that have been active locally that are now expired. */
    protected AtomicInteger expiredCounter = new AtomicInteger();
    /** Number of ms since last call to reset() */
    protected long timeSinceLastReset = 0;
    /** Cumulative time spent in backgroundProcess */
    protected AtomicLong processingTime = new AtomicLong();
    /** Maximum time in ms a now expired session has been alive */
    protected AtomicInteger maxAliveTime = new AtomicInteger();
    /** Average time in ms a now expired session has been alive */
    protected AtomicInteger averageAliveTime = new AtomicInteger();
    /** Number of times our session id generator has generated an id that matches an existing session. */
    protected AtomicInteger duplicates = new AtomicInteger();

    protected AbstractSessionManager(JBossWebMetaData metaData) {
        this.processExpiresFrequency = 1;

        Integer maxActiveSessions = metaData.getMaxActiveSessions();
        if (maxActiveSessions != null) {
            this.setMaxActiveAllowed(maxActiveSessions.intValue());
        }
        SessionConfigMetaData config = metaData.getSessionConfig();
        if (config != null) {
            // Convert session timeout (minutes) to max inactive interval (seconds)
            this.setMaxInactiveInterval(config.getSessionTimeout() * 60);
        }
    }

    @Override
    public synchronized void start() throws LifecycleException {
        if (!this.initialized) {
            this.init();
        }

        log.debug("Starting JBossManager");

        // Validate and update our current component state
        if (this.started) return;

        this.lifecycle.fireLifecycleEvent(START_EVENT, null);
        this.started = true;
    }

    @Override
    public void stop() throws LifecycleException {
        // Validate and update our current component state
        if (!this.started) return;

        this.started = false;

        // Block for any ongoing backgroundProcess, then disable
        synchronized (this) {
            resetStats();

            this.lifecycle.fireLifecycleEvent(STOP_EVENT, null);

            log.debug("Stopping JBossManager");

            this.destroy();
        }
    }

    @Override
    public void processExpires() {
        synchronized (this) {
            if (this.started) {
                long start = System.currentTimeMillis();

                processExpirationPassivation();

                processingTime.addAndGet(System.currentTimeMillis() - start);
            }
        }
    }

    protected abstract void processExpirationPassivation();

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        this.lifecycle.addLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return this.lifecycle.findLifecycleListeners();
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        this.lifecycle.removeLifecycleListener(listener);
    }

    @Override
    public int getRejectedSessions() {
        return this.rejectedCounter.get();
    }

    @Override
    public void setRejectedSessions(int rejectedSessions) {
        this.rejectedCounter.set(rejectedSessions);
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
        throw new UnsupportedOperationException(MESSAGES.noSessionPassivation());
    }

    @Override
    public void unload() throws IOException {
        throw new UnsupportedOperationException(MESSAGES.noSessionPassivation());
    }

    @Override
    public void setNewSessionCookie(String sessionId, HttpServletResponse response) {
        if (response != null) {
            Context context = (Context) container;
            Connector connector = ((Response) response).getConnector();
            if (context.getCookies()) {
                // set a new session cookie
                Cookie cookie = new Cookie(Globals.SESSION_COOKIE_NAME, sessionId);
                // JBAS-6206. Configure cookie a la o.a.c.connector.Request.configureSessionCookie()
                cookie.setMaxAge(-1);
                if (context.getSessionCookie().getPath() != null) {
                    cookie.setPath(context.getSessionCookie().getPath());
                } else {
                    String contextPath = context.getEncodedPath();
                    if ("".equals(contextPath)) {
                        contextPath = "/";
                    }
                    cookie.setPath(contextPath);
                }
                if (context.getSessionCookie().getComment() != null) {
                    cookie.setComment(context.getSessionCookie().getComment());
                }
                if (context.getSessionCookie().getDomain() != null) {
                    cookie.setDomain(context.getSessionCookie().getDomain());
                }
                if (context.getSessionCookie().isHttpOnly()) {
                    cookie.setHttpOnly(true);
                }
                if (context.getSessionCookie().isSecure()) {
                    cookie.setSecure(true);
                }
                if (connector.getSecure()) {
                    cookie.setSecure(true);
                }

                log.tracef("Setting cookie with session id: %s & name: %s", sessionId, Globals.SESSION_COOKIE_NAME);

                response.addCookie(cookie);
            }
        }
    }

    @Override
    public ReplicationStatistics getReplicationStatistics() {
        return this.stats;
    }

    @Override
    public void resetStats() {
        stats.resetStats();
        maxActiveCounter.set(localActiveCounter.get());
        rejectedCounter.set(0);
        createdCounter.set(0);
        expiredCounter.set(0);
        processingTime.set(0);
        maxAliveTime.set(0);
        averageAliveTime.set(0);
        duplicates.set(0);
        timeSinceLastReset = System.currentTimeMillis();
    }

    @Override
    public long getTimeSinceLastReset() {
        return this.timeSinceLastReset;
    }

    @Override
    public long getActiveSessionCount() {
        return calcActiveSessions();
    }

    @Override
    public long getLocalActiveSessionCount() {
        return this.localActiveCounter.get();
    }

    @Override
    public long getRejectedSessionCount() {
        return this.rejectedCounter.get();
    }

    @Override
    public long getCreatedSessionCount() {
        return this.createdCounter.get();
    }

    @Override
    public long getExpiredSessionCount() {
        return this.expiredCounter.get();
    }

    @Override
    public long getMaxActiveSessionCount() {
        return this.maxActiveCounter.get();
    }

    @Override
    public long getMaxLocalActiveSessionCount() {
        return this.maxLocalActiveCounter.get();
    }

    @Override
    public int getMaxActiveAllowed() {
        return this.maxActiveAllowed;
    }

    @Override
    public void setMaxActiveAllowed(int max) {
        this.maxActiveAllowed = max;
    }

    @Override
    public int getMaxActiveSessions() {
        return this.maxActive;
    }

    @Override
    public Map.Entry<String, String> parse(String sessionId) {
        String realId = sessionId;
        String jvmRoute = null;
        int index = sessionId.indexOf('.', 0);
        if (index > 0) {
            realId = sessionId.substring(0, index);
            if (index < sessionId.length() - 1) {
                jvmRoute = sessionId.substring(index + 1);
            }
        }
        return new AbstractMap.SimpleImmutableEntry<String, String>(realId, jvmRoute);
    }

    @Override
    public String createSessionId(String realId, String jvmRoute) {
        return (jvmRoute != null) ? realId + "." + jvmRoute : realId;
    }

    /**
     * Updates statistics to reflect that a session with a given "alive time" has been expired.
     *
     * @param sessionAliveTime number of ms from when the session was created to when it was expired.
     */
    protected void sessionExpired(int sessionAliveTime) {
        int current = maxAliveTime.get();
        while (sessionAliveTime > current) {
            if (maxAliveTime.compareAndSet(current, sessionAliveTime))
                break;
            else
                current = maxAliveTime.get();
        }

        expiredCounter.incrementAndGet();
        int newAverage;
        do {
            int expCount = expiredCounter.get();
            current = averageAliveTime.get();
            newAverage = ((current * (expCount - 1)) + sessionAliveTime) / expCount;
        } while (averageAliveTime.compareAndSet(current, newAverage) == false);
    }

    /**
     * Calculates the number of active sessions, and updates the max # of local active sessions and max # of sessions.
     * <p>
     * Call this method when a new session is added or when an accurate count of active sessions is needed.
     * </p>
     *
     * @return the size of the sessions map + the size of the unloaded sessions map - the count of passivated sessions
     */
    protected int calcActiveSessions() {
        localActiveCounter.set(sessions.size());
        int active = localActiveCounter.get();
        int maxLocal = maxLocalActiveCounter.get();
        while (active > maxLocal) {
            if (!maxLocalActiveCounter.compareAndSet(maxLocal, active)) {
                maxLocal = maxLocalActiveCounter.get();
            }
        }

        int count = getTotalActiveSessions();
        int max = maxActiveCounter.get();
        while (count > max) {
            if (!maxActiveCounter.compareAndSet(max, count)) {
                max = maxActiveCounter.get();
                // Something changed, so reset our count
                count = getTotalActiveSessions();
            }
        }
        return count;
    }

    /** Get the total number of active sessions */
    protected abstract int getTotalActiveSessions();
}
