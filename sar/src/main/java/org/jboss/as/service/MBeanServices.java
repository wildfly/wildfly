/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.service;

import java.lang.reflect.Method;
import java.util.List;

import javax.management.MBeanServer;

import org.jboss.as.jmx.MBeanRegistrationService;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;

/**
 * Services associated with MBean responsible for dependencies & injection management.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class MBeanServices {

    private static final String CREATE_METHOD_NAME = "create";
    private static final String DESTROY_METHOD_NAME = "destroy";
    private static final String START_METHOD_NAME = "start";
    private static final String STOP_METHOD_NAME = "stop";
    private static final Class<?>[] NO_ARGS = new Class<?>[0];
    private final String mBeanName;
    private final ServiceName createDestroyServiceName;
    private final ServiceName startStopServiceName;
    private final Service<Object> createDestroyService;
    private final Service<Object> startStopService;
    private final ServiceBuilder<?> createDestroyServiceBuilder;
    private final ServiceBuilder<?> startStopServiceBuilder;
    private final ServiceTarget target;
    private boolean installed;

    MBeanServices(final String mBeanName, final Object mBeanInstance, final List<ClassReflectionIndex<?>> mBeanClassHierarchy, final ServiceTarget target) {
        if (mBeanClassHierarchy == null) {
            throw SarMessages.MESSAGES.nullVar("mBeanName");
        }
        if (mBeanInstance == null) {
            throw SarMessages.MESSAGES.nullVar("mBeanInstance");
        }
        if (target == null) {
            throw SarMessages.MESSAGES.nullVar("target");
        }

        final Method createMethod = ReflectionUtils.getMethod(mBeanClassHierarchy, CREATE_METHOD_NAME, NO_ARGS, false);
        final Method destroyMethod = ReflectionUtils.getMethod(mBeanClassHierarchy, DESTROY_METHOD_NAME, NO_ARGS, false);
        createDestroyService = new CreateDestroyService(mBeanInstance, createMethod, destroyMethod);
        createDestroyServiceName = ServiceNameFactory.newCreateDestroy(mBeanName);
        createDestroyServiceBuilder = target.addService(createDestroyServiceName, createDestroyService);

        final Method startMethod = ReflectionUtils.getMethod(mBeanClassHierarchy, START_METHOD_NAME, NO_ARGS, false);
        final Method stopMethod = ReflectionUtils.getMethod(mBeanClassHierarchy, STOP_METHOD_NAME, NO_ARGS, false);
        startStopService = new StartStopService(mBeanInstance, startMethod, stopMethod);
        startStopServiceName = ServiceNameFactory.newStartStop(mBeanName);
        startStopServiceBuilder = target.addService(startStopServiceName, startStopService);
        startStopServiceBuilder.addDependency(createDestroyServiceName);

        this.mBeanName = mBeanName;
        this.target = target;
    }

    Service<Object> getCreateDestroyService() {
        assertState();
        return createDestroyService;
    }

    Service<Object> getStartStopService() {
        assertState();
        return startStopService;
    }

    void addDependency(final String dependencyMBeanName, final Injector<Object> injector) {
        assertState();
        final ServiceName injectedMBeanCreateDestroyServiceName = ServiceNameFactory.newCreateDestroy(dependencyMBeanName);
        createDestroyServiceBuilder.addDependency(injectedMBeanCreateDestroyServiceName, injector);
        final ServiceName injectedMBeanStartStopServiceName = ServiceNameFactory.newStartStop(dependencyMBeanName);
        startStopServiceBuilder.addDependency(injectedMBeanStartStopServiceName);
    }

    void addInjectionValue(final Injector<Object> injector, final Value<?> value) {
        assertState();
        createDestroyServiceBuilder.addInjectionValue(injector, value);
    }

    void install() {
        assertState();
        createDestroyServiceBuilder.install();
        startStopServiceBuilder.install();

        // Add service to register the mbean in the mbean server
        final MBeanRegistrationService<Object> mbeanRegistrationService = new MBeanRegistrationService<Object>(mBeanName);
        target.addService(MBeanRegistrationService.SERVICE_NAME.append(mBeanName), mbeanRegistrationService)
            .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, mbeanRegistrationService.getMBeanServerInjector())
            .addDependency(startStopServiceName, Object.class, mbeanRegistrationService.getValueInjector())
            .install();

        installed = true;
    }

    private void assertState() {
        if (installed) {
            throw new IllegalStateException();
        }
    }

}
