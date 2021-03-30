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
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.management.MBeanServer;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.service.component.ServiceComponentInstantiator;
import org.jboss.as.service.logging.SarLogger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

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

    private final CreateDestroyService createDestroyService;
    private final ServiceBuilder<?> createDestroyServiceBuilder;
    private final ServiceBuilder<?> startStopServiceBuilder;
    private final ServiceBuilder<?> registerUnregisterServiceBuilder;
    private boolean installed;

    MBeanServices(final String mBeanName, final Object mBeanInstance, final List<ClassReflectionIndex> mBeanClassHierarchy,
                  final ServiceTarget target, final ServiceComponentInstantiator componentInstantiator,
                  final List<SetupAction> setupActions, final ClassLoader mbeanContextClassLoader, final ServiceName mbeanServerServiceName) {
        if (mBeanClassHierarchy == null) {
            throw SarLogger.ROOT_LOGGER.nullVar("mBeanClassHierarchy");
        }
        if (mBeanInstance == null) {
            throw SarLogger.ROOT_LOGGER.nullVar("mBeanInstance");
        }
        if (target == null) {
            throw SarLogger.ROOT_LOGGER.nullVar("target");
        }
        if (mbeanServerServiceName == null) {
            throw SarLogger.ROOT_LOGGER.nullVar("mbeanServerServiceName");
        }

        final Method createMethod = ReflectionUtils.getMethod(mBeanClassHierarchy, CREATE_METHOD_NAME, NO_ARGS, false);
        final Method destroyMethod = ReflectionUtils.getMethod(mBeanClassHierarchy, DESTROY_METHOD_NAME, NO_ARGS, false);
        ServiceName createDestroyServiceName = ServiceNameFactory.newCreateDestroy(mBeanName);
        createDestroyServiceBuilder = target.addService(createDestroyServiceName);
        Consumer<Object> mBeanInstanceConsumer = createDestroyServiceBuilder.provides(createDestroyServiceName);
        Supplier<ExecutorService> executorSupplier = createDestroyServiceBuilder.requires(AbstractControllerService.EXECUTOR_CAPABILITY.getCapabilityServiceName());
        createDestroyService = new CreateDestroyService(mBeanInstance, createMethod, destroyMethod, componentInstantiator, setupActions, mbeanContextClassLoader, mBeanInstanceConsumer, executorSupplier);
        createDestroyServiceBuilder.setInstance(createDestroyService);
        if(componentInstantiator != null) {
            // the service that starts the EE component needs to start first
            createDestroyServiceBuilder.requires(componentInstantiator.getComponentStartServiceName());
        }

        final Method startMethod = ReflectionUtils.getMethod(mBeanClassHierarchy, START_METHOD_NAME, NO_ARGS, false);
        final Method stopMethod = ReflectionUtils.getMethod(mBeanClassHierarchy, STOP_METHOD_NAME, NO_ARGS, false);
        ServiceName startStopServiceName = ServiceNameFactory.newStartStop(mBeanName);
        startStopServiceBuilder = target.addService(startStopServiceName);
        mBeanInstanceConsumer = startStopServiceBuilder.provides(startStopServiceName);
        executorSupplier = startStopServiceBuilder.requires(AbstractControllerService.EXECUTOR_CAPABILITY.getCapabilityServiceName());
        StartStopService startStopService = new StartStopService(mBeanInstance, startMethod, stopMethod, setupActions, mbeanContextClassLoader, mBeanInstanceConsumer, executorSupplier);
        startStopServiceBuilder.setInstance(startStopService);
        startStopServiceBuilder.requires(createDestroyServiceName);

        ServiceName registerUnregisterServiceName = ServiceNameFactory.newRegisterUnregister(mBeanName);
        registerUnregisterServiceBuilder = target.addService(registerUnregisterServiceName);
        // register with the legacy alias as well
        registerUnregisterServiceBuilder.provides(registerUnregisterServiceName, MBeanRegistrationService.SERVICE_NAME.append(mBeanName));
        final Supplier<MBeanServer> mBeanServerSupplier = registerUnregisterServiceBuilder.requires(mbeanServerServiceName);
        final Supplier<Object> objectSupplier = registerUnregisterServiceBuilder.requires(startStopServiceName);
        registerUnregisterServiceBuilder.setInstance(new MBeanRegistrationService(mBeanName, setupActions, mBeanServerSupplier, objectSupplier));

        for (SetupAction action : setupActions) {
            for (ServiceName dependency : action.dependencies()) {
                startStopServiceBuilder.requires(dependency);
                createDestroyServiceBuilder.requires(dependency);
            }
        }

    }

    void addDependency(final String dependencyMBeanName)  {
        assertState();
        final ServiceName injectedMBeanCreateDestroyServiceName = ServiceNameFactory.newCreateDestroy(dependencyMBeanName);
        createDestroyServiceBuilder.requires(injectedMBeanCreateDestroyServiceName);
        final ServiceName injectedMBeanStartStopServiceName = ServiceNameFactory.newStartStop(dependencyMBeanName);
        startStopServiceBuilder.requires(injectedMBeanStartStopServiceName);
        final ServiceName injectedMBeanRegisterUnregisterServiceName = ServiceNameFactory.newRegisterUnregister(dependencyMBeanName);
        registerUnregisterServiceBuilder.requires(injectedMBeanRegisterUnregisterServiceName);
    }

    void addAttribute(final String attributeMBeanName, final Method setter, final DelegatingSupplier propertySupplier) {
        assertState();
        final ServiceName injectedMBeanCreateDestroyServiceName = ServiceNameFactory.newCreateDestroy(attributeMBeanName);
        final Supplier<Object> injectedMBeanSupplier = createDestroyServiceBuilder.requires(injectedMBeanCreateDestroyServiceName);
        propertySupplier.setObjectSupplier(injectedMBeanSupplier);
        createDestroyService.inject(setter, propertySupplier);
        final ServiceName injectedMBeanStartStopServiceName = ServiceNameFactory.newStartStop(attributeMBeanName);
        startStopServiceBuilder.requires(injectedMBeanStartStopServiceName);
    }

    void addValue(final Method setter, final Supplier<Object> objectSupplier) {
        assertState();
        createDestroyService.inject(setter, objectSupplier);
    }

    void install() {
        assertState();
        createDestroyServiceBuilder.install();
        startStopServiceBuilder.install();
        registerUnregisterServiceBuilder.install();

        installed = true;
    }

    private void assertState() {
        if (installed) {
            throw new IllegalStateException();
        }
    }
}
