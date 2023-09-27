/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.system.ServiceMBeanSupport;

public class LifecycleListener extends ServiceMBeanSupport implements LifecycleListenerMBean {

    private final List<String> creates = Collections.synchronizedList(new ArrayList<>());
    private final List<String> starts = Collections.synchronizedList(new ArrayList<>());
    private final List<String> stops = Collections.synchronizedList(new ArrayList<>());
    private final List<String> destroys = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void mbeanCreated(String id) {
        creates.add(id);
    }

    @Override
    public synchronized void mbeanStarted(String id) {
        starts.add(id);
    }

    @Override
    public synchronized void mbeanStopped(String id) {
        stops.add(id);
    }

    @Override
    public void mbeanDestroyed(String id) {
        destroys.add(id);
    }

    @Override
    public List<String> getCreates() {
        return creates;
    }

    @Override
    public List<String> getStarts() {
        return starts;
    }

    @Override
    public List<String> getStops() {
        return stops;
    }

    @Override
    public List<String> getDestroys() {
        return destroys;
    }
}
