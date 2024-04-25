/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.order;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.jboss.system.ServiceMBeanSupport;

public class LifecycleEmitter extends ServiceMBeanSupport implements LifecycleEmitterMBean {

    private static final String[] LISTENER_SIG = { "java.lang.String", "java.lang.String" };
    private static volatile ObjectName LISTENER;

    private volatile String id;
    private volatile ObjectName listener;
    private volatile ObjectName dependency;

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
    public void setLifecycleListener(ObjectName listener) {
        this.listener = listener;
    }

    @Override
    protected void createService() throws Exception {
        invokeListener(MBEAN_CREATED);
    }

    @Override
    protected void startService() throws Exception {
        invokeListener(MBEAN_STARTED);
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        ObjectName result = super.preRegister(server, name);
        invokeListener(MBEAN_PRE_REGISTERED);
        return result;
    }

    @Override
    public void postRegister(Boolean registrationDone) {
        super.postRegister(registrationDone);
        safeInvokeListener(MBEAN_POST_REGISTERED);
    }

    @Override
    public void preDeregister() throws Exception {
        super.preDeregister();
        invokeListener(MBEAN_PRE_DEREGISTERED);
    }

    @Override
    public void postDeregister() {
        super.postDeregister();
        safeInvokeListener(MBEAN_POST_DEREGISTERED);
    }

    @Override
    protected void stopService() throws Exception {
        invokeListener(MBEAN_STOPPED);
    }

    @Override
    protected void destroyService() throws Exception {
        invokeListener(MBEAN_DESTROYED);
    }

    private void safeInvokeListener(String methodName) {
        try {
            invokeListener(methodName);
        } catch (MalformedObjectNameException  | ReflectionException  | InstanceNotFoundException  | MBeanException e) {
            throw new RuntimeException(e);
        }
    }

    private void invokeListener(String eventName) throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException, MBeanException {
        if ("A".equals(id)) {
            if (!MBEAN_PRE_DEREGISTERED.equals(eventName) && !MBEAN_POST_DEREGISTERED.equals(eventName)) {
                // Add a delay to give the other mbeans a chance to (incorrectly) move ahead
                sleep(100);
            }
        } else if (MBEAN_STOPPED.equals(eventName) || MBEAN_DESTROYED.equals(eventName)) {
            // Add a delay to give A chance to (incorrectly) deregister
            sleep(50);
        }
        getServer().invoke(getListenerObjectName(), "mbeanEvent", new Object[]{ id, eventName }, LISTENER_SIG);
    }

    private static ObjectName getListenerObjectName() throws MalformedObjectNameException {
        if (LISTENER == null) {
            LISTENER = ObjectName.getInstance("jboss:name=OrderListener");
        }
        return LISTENER;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
