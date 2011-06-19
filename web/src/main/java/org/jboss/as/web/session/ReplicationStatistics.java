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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A session replication statistics collection class.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81134 $
 */
public class ReplicationStatistics implements Serializable {
    /** The serial version ID */
    private static final long serialVersionUID = 9153807780893455734L;

    /** A HashMap<String, TimeStatistic> of the method invocations */
    private Map<String, TimeStatistic> ctxStats;
    /** Time of the last resetStats call */
    public long lastResetTime = System.currentTimeMillis();

    public static class TimeStatistic {
        public long replicationCount;
        public long minPassivationTime = Long.MAX_VALUE;
        public long maxPassivationTime;
        public long totalPassivationTime;
        public long minReplicationTime = Long.MAX_VALUE;
        public long maxReplicationTime;
        public long totalReplicationlTime;

        public long loadCount;
        public long minLoadTime = Long.MAX_VALUE;
        public long maxLoadTime;
        public long totalLoadlTime;

        public void reset() {
            replicationCount = 0;
            minPassivationTime = Long.MAX_VALUE;
            maxPassivationTime = 0;
            totalPassivationTime = 0;
            minReplicationTime = Long.MAX_VALUE;
            maxReplicationTime = 0;
            totalReplicationlTime = 0;
            loadCount = 0;
            minLoadTime = Long.MAX_VALUE;
            maxLoadTime = 0;
            totalLoadlTime = 0;
        }
    }

    public ReplicationStatistics() {
        ctxStats = new ConcurrentHashMap<String, TimeStatistic>(256, 0.75f, 32);
    }

    public void updatePassivationStats(String ctx, long elapsed) {
        TimeStatistic stat = getTimeStatistic(ctx);
        stat.totalPassivationTime += elapsed;
        if (stat.minPassivationTime > elapsed)
            stat.minPassivationTime = elapsed;
        if (stat.maxPassivationTime < elapsed)
            stat.maxPassivationTime = elapsed;
    }

    /**
     * Update the TimeStatistic for the given ctx. This does not synchronize on the TimeStatistic so the results are an
     * approximate values.
     *
     * @param ctx the method to update the statistics for.
     * @param elapsed the elapsed time in milliseconds for the invocation.
     */
    public void updateReplicationStats(String ctx, long elapsed) {
        TimeStatistic stat = getTimeStatistic(ctx);
        stat.replicationCount++;
        stat.totalReplicationlTime += elapsed;
        if (stat.minReplicationTime > elapsed)
            stat.minReplicationTime = elapsed;
        if (stat.maxReplicationTime < elapsed)
            stat.maxReplicationTime = elapsed;
    }

    public void updateLoadStats(String ctx, long elapsed) {
        TimeStatistic stat = getTimeStatistic(ctx);
        stat.loadCount++;
        stat.totalLoadlTime += elapsed;
        if (stat.minLoadTime > elapsed)
            stat.minLoadTime = elapsed;
        if (stat.maxLoadTime < elapsed)
            stat.maxLoadTime = elapsed;
    }

    /**
     * Resets all current TimeStatistics.
     *
     */
    public void resetStats() {
        synchronized (ctxStats) {
            for (TimeStatistic stat : ctxStats.values()) {
                stat.reset();
            }
        }
        lastResetTime = System.currentTimeMillis();
    }

    public void removeStats(String id) {
        ctxStats.remove(id);
    }

    /**
     * Access the current collection of ctx invocation statistics
     *
     * @return A HashMap<String, TimeStatistic> of the ctx invocations
     */
    public Map<String, TimeStatistic> getStats() {
        return ctxStats;
    }

    @Override
    public String toString() {
        StringBuffer tmp = new StringBuffer();
        for (Map.Entry<String, TimeStatistic> entry : ctxStats.entrySet()) {
            TimeStatistic stat = (TimeStatistic) entry.getValue();
            if (stat != null) {
                tmp.append("[sessionID: ");
                tmp.append(entry.getKey());
                tmp.append(", replicationCount=");
                tmp.append(stat.replicationCount);
                tmp.append(", minPassivationTime=");
                tmp.append(stat.minPassivationTime);
                tmp.append(", maxPassivationTime=");
                tmp.append(stat.maxPassivationTime);
                tmp.append(", totalPassivationTime=");
                tmp.append(stat.totalPassivationTime);
                tmp.append(", minReplicationTime=");
                tmp.append(stat.minReplicationTime);
                tmp.append(", maxReplicationTime=");
                tmp.append(stat.maxReplicationTime);
                tmp.append(", totalReplicationlTime=");
                tmp.append(stat.totalReplicationlTime);
                tmp.append(", loadCount=");
                tmp.append(stat.loadCount);
                tmp.append(", minLoadTime=");
                tmp.append(stat.minLoadTime);
                tmp.append(", maxLoadTime=");
                tmp.append(stat.maxLoadTime);
                tmp.append(", totaLoadlTime=");
                tmp.append(stat.totalLoadlTime);
                tmp.append("];");
            }
        }
        tmp.append(")");
        return tmp.toString();
    }

    private TimeStatistic getTimeStatistic(String ctx) {
        TimeStatistic stat = (TimeStatistic) ctxStats.get(ctx);
        if (stat == null) {
            stat = new TimeStatistic();
            ctxStats.put(ctx, stat);
        }
        return stat;
    }

}
