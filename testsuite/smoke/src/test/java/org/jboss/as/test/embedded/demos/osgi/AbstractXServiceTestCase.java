/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.embedded.demos.osgi;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.jmx.ObjectNameFactory;
import org.jboss.osgi.testing.OSGiRemoteRuntime;
import org.jboss.osgi.testing.OSGiRuntimeTest;
import org.junit.Before;
import org.osgi.jmx.framework.FrameworkMBean;

/**
 * Abstract base for XService testing.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
public abstract class AbstractXServiceTestCase extends OSGiRuntimeTest {
    private static ObjectName SERVICE_CONTAINER_OBJECTNAME = ObjectNameFactory.create("jboss.internal", "mbean",
            "ServiceContainer");
    private OSGiRemoteRuntime runtime;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        runtime = (OSGiRemoteRuntime) getRuntime();

        MBeanServerConnection mbeanServer = runtime.getMBeanServer();
        activateBundleContextService(mbeanServer);
    }

    public OSGiRemoteRuntime getRemoteRuntime() {
        return runtime;
    }

    protected long registerModule(ModuleIdentifier moduleId) throws Exception {
        ObjectName oname = ObjectNameFactory.create("jboss.osgi:service=jmx,type=BundleManager");
        MBeanServerConnection mbeanServer = runtime.getMBeanServer();
        if (isRegisteredWithTimeout(mbeanServer, oname, 10000) == false)
            throw new InstanceNotFoundException(oname.getCanonicalName());

        try {
            Object[] params = new Object[] { moduleId };
            String[] signature = new String[] { ModuleIdentifier.class.getName() };
            Long bundleId = (Long) mbeanServer.invoke(oname, "installBundle", params, signature);
            return bundleId;
        } catch (MBeanException ex) {
            throw ex.getTargetException();
        }
    }

    protected State getServiceState(ServiceName serviceName, State expState, long timeout) throws Exception {
        MBeanServerConnection mbeanServer = runtime.getMBeanServer();
        Object[] params = new Object[] { serviceName.getCanonicalName() };
        String[] signature = new String[] { String.class.getName() };
        String state = (String) mbeanServer.invoke(SERVICE_CONTAINER_OBJECTNAME, "getState", params, signature);
        while ((state == null || state != expState.toString()) && timeout > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // ignore
            }
            state = (String) mbeanServer.invoke(SERVICE_CONTAINER_OBJECTNAME, "getState", params, signature);
            timeout -= 100;
        }
        return state != null ? State.valueOf(state) : null;
    }

    private void activateBundleContextService(MBeanServerConnection mbeanServer) {
        try {
            Object[] params = new Object[] { "jboss.osgi.context" };
            String[] signature = new String[] { String.class.getName() };
            String state = (String) mbeanServer.invoke(SERVICE_CONTAINER_OBJECTNAME, "getState", params, signature);
            if (State.UP != State.valueOf(state)) {
                params = new Object[] { "jboss.osgi.context", "ACTIVE" };
                signature = new String[] { String.class.getName(), String.class.getName() };
                mbeanServer.invoke(SERVICE_CONTAINER_OBJECTNAME, "setMode", params, signature);
            }
            FrameworkMBean frameworkMBean = runtime.getFrameworkMBean();
            assertNotNull("FrameworkMBean not null", frameworkMBean);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new RuntimeException("Cannot activate BundleContextService", ex);
        }
    }

    private boolean isRegisteredWithTimeout(MBeanServerConnection mbeanServer, ObjectName objectName, int timeout)
            throws IOException {
        boolean registered = mbeanServer.isRegistered(objectName);
        while (registered == false && timeout > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // ignore
            }
            registered = mbeanServer.isRegistered(objectName);
            timeout -= 100;
        }
        return registered;
    }
}
