/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.order;

import static org.jboss.as.test.integration.sar.order.LifecycleEmitterMBean.invokeListener;
import static org.jboss.as.test.integration.sar.order.LifecycleEmitterMBean.safeInvokeListener;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.system.Service;

public class CustomLifecycleEmitter implements CustomLifecycleEmitterMBean, Service, MBeanRegistration {

    private volatile String id;
    private volatile ObjectName listener;
    private volatile ObjectName dependency;
    private volatile MBeanServer server;
    private final List<String> unrecordedEvents = new ArrayList<>();


    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ObjectName getDependency() {
        return dependency;
    }

    @Override
    public void setDependency(ObjectName dependency) {
        this.dependency = dependency;
    }

    @Override
    public ObjectName getLifecycleListener() {
        return listener;
    }

    @Override
    public void setLifecycleListener(ObjectName lifecycleListener) {
        this.listener = lifecycleListener;
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.server = server;
        for (String unrecordedEvent : unrecordedEvents) {
            invokeListener(id, unrecordedEvent, server);
        }
        invokeListener(id, MBEAN_PRE_REGISTERED, server);
        return name;
    }

    @Override
    public void postRegister(Boolean registrationDone) {
        safeInvokeListener(id, MBEAN_POST_REGISTERED, server);
    }

    @Override
    public void preDeregister() throws Exception {
        invokeListener(id, MBEAN_PRE_DEREGISTERED, server);
    }

    @Override
    public void postDeregister() {
        safeInvokeListener(id, MBEAN_POST_DEREGISTERED, server);
    }

    @Override
    public void create() throws Exception {
        if (server != null) {
            invokeListener(id, MBEAN_CREATED, server);
        } else {
            unrecordedEvents.add(MBEAN_CREATED);
        }
    }

    @Override
    public void start() throws Exception {
        if (server != null) {
            invokeListener(id, MBEAN_STARTED, server);
        } else {
            unrecordedEvents.add(MBEAN_STARTED);
        }
    }

    @Override
    public void stop() {
        safeInvokeListener(id, MBEAN_STOPPED, server);
    }

    @Override
    public void destroy() {
        safeInvokeListener(id, MBEAN_DESTROYED, server);
    }
}
