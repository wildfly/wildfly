/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.wildfly.extension.mod_cluster;

import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.ActiveSessionsLoadMetric;
import org.jboss.modcluster.load.metric.impl.AverageSystemLoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;
import org.jboss.modcluster.load.metric.impl.HeapMemoryUsageLoadMetric;
import org.jboss.modcluster.load.metric.impl.ReceiveTrafficLoadMetric;
import org.jboss.modcluster.load.metric.impl.RequestCountLoadMetric;
import org.jboss.modcluster.load.metric.impl.SendTrafficLoadMetric;

/**
 * Enumeration of mod_cluster load metrics.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public enum LoadMetricEnum {

    CPU("cpu", AverageSystemLoadMetric.class),
    @Deprecated SYSTEM_MEMORY("mem", null),
    HEAP_MEMORY("heap", HeapMemoryUsageLoadMetric.class),
    ACTIVE_SESSIONS("sessions", ActiveSessionsLoadMetric.class),
    RECEIVE_TRAFFIC("receive-traffic", ReceiveTrafficLoadMetric.class),
    SEND_TRAFFIC("send-traffic", SendTrafficLoadMetric.class),
    REQUEST_COUNT("requests", RequestCountLoadMetric.class),
    BUSY_CONNECTORS("busyness", BusyConnectorsLoadMetric.class),
    ;

    private final String type;
    private final Class<? extends LoadMetric> loadMetricClass;

    LoadMetricEnum(String type, Class<? extends LoadMetric> loadMetricClass) {
        this.type = type;
        this.loadMetricClass = loadMetricClass;
    }

    public String getType() {
        return this.type;
    }

    public Class<? extends LoadMetric> getLoadMetricClass() {
        return this.loadMetricClass;
    }

    public static LoadMetricEnum forType(String type) {
        for (LoadMetricEnum metric : LoadMetricEnum.values()) {
            if (metric.type.equals(type)) {
                return metric;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.type;
    }
}
