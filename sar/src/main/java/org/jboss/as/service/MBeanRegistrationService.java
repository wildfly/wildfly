/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import static org.jboss.as.service.logging.SarLogger.ROOT_LOGGER;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service used to register and unregister an mbean with an mbean server.
 *
 * @author John Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class MBeanRegistrationService implements Service {
    /** @deprecated Use {@link ServiceNameFactory#newRegisterUnregister(String)} -- only kept for a while in case user code looks for this */
    @Deprecated
    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("mbean", "registration");
    private final Supplier<MBeanServer> mBeanServerSupplier;
    private final Supplier<Object> objectSupplier;
    private final String name;
    private ObjectName objectName;
    private final List<SetupAction> setupActions;

    public MBeanRegistrationService(final String name, final List<SetupAction> setupActions,
                                    final Supplier<MBeanServer> mBeanServerSupplier,
                                    final Supplier<Object> objectSupplier) {
        this.name = name;
        this.setupActions = setupActions;
        this.mBeanServerSupplier = mBeanServerSupplier;
        this.objectSupplier = objectSupplier;
    }

    public synchronized void start(final StartContext context) throws StartException {
        final MBeanServer mBeanServer = getMBeanServer();
        final Object value = objectSupplier.get();
        try {
            objectName = new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw ROOT_LOGGER.mbeanRegistrationFailed(e, name);
        }
        try {
            for (SetupAction action : setupActions) {
                action.setup(Collections.<String, Object>emptyMap());
            }
            try {
                ROOT_LOGGER.debugf("Registering [%s] with name [%s]", value, objectName);
                mBeanServer.registerMBean(value, objectName);
            } catch (Exception e) {
                throw ROOT_LOGGER.mbeanRegistrationFailed(e, name);
            }
        } finally {
            ListIterator<SetupAction> it = setupActions.listIterator(setupActions.size());
            while (it.hasPrevious()) {
                SetupAction action = it.previous();
                action.teardown(Collections.<String, Object>emptyMap());
            }
        }
    }

    public synchronized void stop(final StopContext context) {
        if (objectName == null) {
            ROOT_LOGGER.cannotUnregisterObject();
        }
        final MBeanServer mBeanServer = getMBeanServer();
        try {
            for (SetupAction action : setupActions) {
                action.setup(Collections.<String, Object>emptyMap());
            }
            try {
                mBeanServer.unregisterMBean(objectName);
            } catch (Exception e) {
                ROOT_LOGGER.unregistrationFailure(e, objectName);
            }
        } finally {
            ListIterator<SetupAction> it = setupActions.listIterator(setupActions.size());
            while (it.hasPrevious()) {
                SetupAction action = it.previous();
                action.teardown(Collections.<String, Object>emptyMap());
            }
        }
    }

    private MBeanServer getMBeanServer() {
        MBeanServer mBeanServer = mBeanServerSupplier.get();
        if (mBeanServer == null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mBeanServer;
    }

}
