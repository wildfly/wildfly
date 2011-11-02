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

/**
 * The MBean-interface for the JBossManager
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @author Brian Stansberry
 *
 * @version $Revision: 106011 $
 */
public interface SessionManagerMBean {
    /**
     * Gets the replication statistics for the sessions managed by this manager.
     *
     * @return the statistics
     */
    ReplicationStatistics getReplicationStatistics();

    /**
     * Resets all statistics.
     */
    void resetStats();

    /**
     * Gets the elapsed time (in seconds) since this manager was instantiated or the last call to resetStats()
     */
    long getTimeSinceLastReset();

    /**
     * Gets the number of sessions active on this node. This includes replicated sessions that have not been accessed on this
     * node.
     */
    long getActiveSessionCount();

    /**
     * Gets the count of sessions known to this manager, excluding those in the distributed stored that have not been accessed
     * on this node.
     */
    long getLocalActiveSessionCount();

    /**
     * Gets the number of times session creation has failed because the number of active sessions exceeds
     * {@link #getMaxActiveAllowed() maxActiveAllowed}
     */
    long getRejectedSessionCount();

    /**
     * Gets the number of sessions created on this node. Does not include sessions initially created on other nodes, even if
     * those sessions were accessed on this node.
     */
    long getCreatedSessionCount();

    /**
     * Gets the number of sessions that have been expired on this node.
     */
    long getExpiredSessionCount();

    /**
     * Gets the highest number of sessions concurrently active on this node. This includes replicated sessions that have not
     * been accessed on this node.
     */
    long getMaxActiveSessionCount();

    /**
     * Gets the highest value seen for {@link #getLocalSessionCount()}
     */
    long getMaxLocalActiveSessionCount();

    /**
     * Gets the maximum number of {@link #getActiveSessionCount() active sessions} that will concurrently be allowed on this
     * node. This includes replicated sessions that have not been accessed on this node.
     */
    int getMaxActiveAllowed();

    /**
     * Sets the maximum number of active sessions that will concurrently be allowed on this node, excluding any sessions that
     * have been passivated. This includes replicated sessions that have not been accessed on this node. If the
     * {@link #getActiveSessionCount() active session count} exceeds this value and an attempt to create a new session is made,
     * session creation will fail with an {@link IllegalStateException}.
     *
     * @param max the max number of sessions, or <code>-1</code> if there is no limit.
     */
    void setMaxActiveAllowed(int max);

    /**
     * Gets the maximum time interval, in seconds, between client requests after which sessions created by this manager should
     * be expired. A negative time indicates that the session should never time out.
     */
    int getMaxInactiveInterval();

    /**
     * Sets the maximum time interval, in seconds, between client requests after which sessions created by this manager should
     * be expired. A negative time indicates that the session should never time out.
     *
     * @param interval The new maximum interval
     */
    void setMaxInactiveInterval(int seconds);

    /**
     * Gets whether this manager's sessions are distributable.
     */
    boolean getDistributable();

    /**
     * Set the distributable flag for the sessions supported by this Manager. If this flag is set, all user data objects added
     * to sessions associated with this manager must implement Serializable.
     *
     * @param distributable the distributable flag
     */
    void setDistributable(boolean distributable);

    /**
     * Gets the cumulative number of milliseconds spent in the <code>Manager.backgroundProcess()</code> method.
     */
    long getProcessingTime();

    /**
     * Sets the cumulative number of milliseconds spent in the <code>Manager.backgroundProcess()</code> method.
     *
     * @param processingTime the processing time
     */
    void setProcessingTime(long processingTime);

    /**
     * Outputs the replication statistics as an HTML table, with one row per session.
     */
    // String reportReplicationStatistics();

    /**
     * Outputs the replication statistics as a comma-separated-values, with one row per session. First row is a header listing
     * field names.
     */
    // String reportReplicationStatisticsCSV();

    /**
     * Outputs the replication statistics for the given session as a set of comma-separated-values. First row is a header
     * listing field names.
     */
    // String reportReplicationStatisticsCSV(String sessionId);

    /**
     * Gets the number of characters used in creating a session id. Excludes any jvmRoute.
     */
    int getSessionIdLength();

    // StandardManager Attributes

    /**
     * Gets the fully qualified class name of the managed object
     */
    String getClassName();

    /**
     * Gets the maximum number of active Sessions allowed, or -1 for no limit.
     */
    int getMaxActiveSessions();

    /**
     * Gets the frequency of the manager checks (expiration and passivation)
     */
    int getProcessExpiresFrequency();

    /**
     * Sets the frequency of the manager checks (expiration and passivation)
     */
    void setProcessExpiresFrequency(int frequency);

    /**
     * Gets the name of this Manager implementation.
     */
    String getName();

    /**
     * Number of active sessions at this moment. Same as {@link #getActiveSessionCount()}.
     */
    int getActiveSessions();

    /**
     * Total number of sessions created by this manager. Same as {@link #getCreatedSessionCount()}
     */
    int getSessionCounter();

    /**
     * Sets the total number of sessions created by this manager.
     *
     * @param sessionCounter the new created session count
     */
    void setSessionCounter(int sessionCounter);

    /**
     * Gets the maximum number of active sessions so far. Same as {@link #getMaxActiveSessionCount()}
     */
    int getMaxActive();

    /**
     * Sets the maximum number of active sessions so far.
     *
     * @param maxActive the new maximum number of active sessions
     */
    void setMaxActive(int maxActive);

    /**
     * Gets the longest time an expired session had been alive
     */
    int getSessionMaxAliveTime();

    /**
     * Sets the longest time an expired session had been alive
     *
     * @param sessionAliveTime the new longest session life
     */
    void setSessionMaxAliveTime(int sessionAliveTime);

    /**
     * Gets the average time an expired session had been alive
     */
    int getSessionAverageAliveTime();

    /**
     * Gets the number of sessions that expired. Same as {@link #getExpiredSessionCount()}
     */
    int getExpiredSessions();

    /**
     * Gets the number of sessions we rejected due to maxActive being reached. Same as {@link #getRejectedSessionCount()}
     */
    int getRejectedSessions();

}
