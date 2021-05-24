/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
