/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
