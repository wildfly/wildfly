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

public interface LifecycleEmitterMBean {

    String MBEAN_CREATED = "mbeanCreated";
    String MBEAN_STARTED = "mbeanStarted";
    String MBEAN_STOPPED = "mbeanStopped";
    String MBEAN_DESTROYED = "mbeanDestroyed";
    String MBEAN_PRE_REGISTERED = "mbeanPreRegistered";
    String MBEAN_POST_REGISTERED = "mbeanPostRegistered";
    String MBEAN_PRE_DEREGISTERED = "mbeanPreDeregistered";
    String MBEAN_POST_DEREGISTERED = "mbeanPostDeregistered";

    String[] LISTENER_SIG = { "java.lang.String", "java.lang.String" };


    String getId();

    void setId(String id);

    ObjectName getDependency();

    void setDependency(ObjectName dependency);

    ObjectName getLifecycleListener();

    void setLifecycleListener(ObjectName lifecycleListener);

    static void safeInvokeListener(String id, String methodName, MBeanServer mBeanServer) {
        try {
            invokeListener(id, methodName, mBeanServer);
        } catch (MalformedObjectNameException | ReflectionException | InstanceNotFoundException | MBeanException e) {
            throw new RuntimeException(e);
        }
    }

    static void invokeListener(String id, String eventName, MBeanServer mBeanServer) throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException, MBeanException {
        if ("A".equals(id)) {
            // Add a delay to give the other mbeans a chance to (incorrectly) move ahead
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        mBeanServer.invoke(ObjectName.getInstance("jboss:name=OrderListener"), "mbeanEvent", new Object[]{ id, eventName }, LISTENER_SIG);
    }
}
