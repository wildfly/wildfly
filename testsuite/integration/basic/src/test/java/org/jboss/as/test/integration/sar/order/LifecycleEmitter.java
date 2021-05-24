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
