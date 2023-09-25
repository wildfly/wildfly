/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.order;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.jboss.system.ServiceMBeanSupport;

public class LifecycleEmitter extends ServiceMBeanSupport implements LifecycleEmitterMBean {

    private static final String[] LISTENER_SIG = { "java.lang.String" };
    private static volatile ObjectName LISTENER;

    private volatile String id;
    private volatile ObjectName listener;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ObjectName getLifecycleListener() {
        return listener;
    }

    @Override
    public void setLifecycleListener(ObjectName listener) {
        this.listener = listener;
    }

    @Override
    protected void createService() throws Exception {
        invokeListener("mbeanCreated");
    }

    @Override
    protected void startService() throws Exception {
        invokeListener("mbeanStarted");
    }

    @Override
    protected void stopService() throws Exception {
        invokeListener("mbeanStopped");
    }

    @Override
    protected void destroyService() throws Exception {
        invokeListener("mbeanDestroyed");
    }

    private void invokeListener(String methodName) throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException, MBeanException {
        if ("A".equals(id)) {
            // Add a delay to give the other mbeans a chance to (incorrectly) move ahead
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        getServer().invoke(getListenerObjectName(), methodName, new Object[]{ id }, LISTENER_SIG);
    }

    private static ObjectName getListenerObjectName() throws MalformedObjectNameException {
        if (LISTENER == null) {
            LISTENER = ObjectName.getInstance("jboss:name=OrderListener");
        }
        return LISTENER;
    }
}
