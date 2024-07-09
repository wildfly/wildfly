/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.management.MBeanServer;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.service.component.ServiceComponentInstantiator;
import org.jboss.as.service.logging.SarLogger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.subsystem.service.AsyncServiceBuilder;

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

    private final CreateDestroyService createDestroyService;
    private final ServiceBuilder<?> createDestroyServiceBuilder;
    private final ServiceBuilder<?> startStopServiceBuilder;
    private final ServiceBuilder<?> registerUnregisterServiceBuilder;
    private boolean installed;

    MBeanServices(final String mBeanName, final Object mBeanInstance, final List<ClassReflectionIndex> mBeanClassHierarchy,
                  final RequirementServiceTarget target, final ServiceComponentInstantiator componentInstantiator,
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

        final Method createMethod = ReflectionUtils.getNoArgMethod(mBeanClassHierarchy, CREATE_METHOD_NAME);
        final Method destroyMethod = ReflectionUtils.getNoArgMethod(mBeanClassHierarchy, DESTROY_METHOD_NAME);
        ServiceName createDestroyServiceName = ServiceNameFactory.newCreateDestroy(mBeanName);
        createDestroyServiceBuilder = new AsyncServiceBuilder<>(target.addService());
        Consumer<Object> mBeanInstanceConsumer = createDestroyServiceBuilder.provides(createDestroyServiceName);
        createDestroyService = new CreateDestroyService(mBeanInstance, createMethod, destroyMethod, componentInstantiator, setupActions, mbeanContextClassLoader, mBeanInstanceConsumer);
        createDestroyServiceBuilder.setInstance(createDestroyService);
        if(componentInstantiator != null) {
            // the service that starts the EE component needs to start first
            createDestroyServiceBuilder.requires(componentInstantiator.getComponentStartServiceName());
        }

        final Method startMethod = ReflectionUtils.getNoArgMethod(mBeanClassHierarchy, START_METHOD_NAME);
        final Method stopMethod = ReflectionUtils.getNoArgMethod(mBeanClassHierarchy, STOP_METHOD_NAME);
        ServiceName startStopServiceName = ServiceNameFactory.newStartStop(mBeanName);
        startStopServiceBuilder = new AsyncServiceBuilder<>(target.addService());
        mBeanInstanceConsumer = startStopServiceBuilder.provides(startStopServiceName);
        StartStopService startStopService = new StartStopService(mBeanInstance, startMethod, stopMethod, setupActions, mbeanContextClassLoader, mBeanInstanceConsumer);
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
        createDestroyServiceBuilder.requires(injectedMBeanRegisterUnregisterServiceName);
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
