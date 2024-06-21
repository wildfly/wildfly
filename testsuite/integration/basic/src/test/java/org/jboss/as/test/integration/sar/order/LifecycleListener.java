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

    private final List<Tuple> allEvents = Collections.synchronizedList(new ArrayList<>());


    @Override
    public void mbeanEvent(String id, String event) {
        allEvents.add(new Tuple(id, event));
    }


    @Override
    public List<Tuple> getAllEvents() {
        return allEvents;
    }

}
