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

package org.jboss.as.arquillian.service;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.management.MBeanServer;

import org.jboss.arquillian.context.ContextManager;
import org.jboss.arquillian.context.ContextManagerBuilder;
import org.jboss.arquillian.protocol.jmx.JMXTestRunner;
import org.jboss.arquillian.protocol.jmx.JMXTestRunner.TestClassLoader;
import org.jboss.arquillian.spi.TestResult;
import org.jboss.arquillian.testenricher.msc.ServiceContainerAssociation;
import org.jboss.arquillian.testenricher.osgi.BundleAssociation;
import org.jboss.arquillian.testenricher.osgi.BundleContextAssociation;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.osgi.deployment.OSGiDeploymentAttachment;
import org.jboss.as.osgi.service.BundleContextService;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.Services;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.client.impl.ModelControllerServerDeploymentManager;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Service responsible for creating and managing the life-cycle of the Arquillian service.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public class ArquillianService implements Service<ArquillianService> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("arquillian", "testrunner");
    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<ServerController> injectedServerController = new InjectedValue<ServerController>();
    private final Map<String, ArquillianConfig> deployedTests = new HashMap<String, ArquillianConfig>();
    private final Map<String, CountDownLatch> waitingTests = new HashMap<String, CountDownLatch>();
    private JMXTestRunner jmxTestRunner;

    public static void addService(final ServiceTarget serviceTarget) {
        ArquillianService service = new ArquillianService();
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(ArquillianService.SERVICE_NAME, service);
        serviceBuilder.addDependency(Services.JBOSS_SERVER_CONTROLLER, ServerController.class, service.injectedServerController);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
        serviceBuilder.install();
    }

    public static final String TEST_CLASS_PROPERTY = "org.jboss.as.arquillian.testClass";

    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting Arquillian Test Runner");

        final MBeanServer mbeanServer = injectedMBeanServer.getValue();
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();
        final TestClassLoader testClassLoader = new TestClassLoaderImpl(serviceContainer);

        try {
            jmxTestRunner = new JMXTestRunner() {

                @Override
                public TestResult runTestMethod(String className, String methodName, Map<String, String> props) {

                    final ContextManagerBuilder builder = new ContextManagerBuilder();
                    ArquillianConfig config = getConfig(className);
                    if (config != null) {
                        final DeploymentUnit deployment = config.getDeploymentUnitContext();
                        final Module module = deployment.getAttachment(Attachments.MODULE);
                        if (module != null) {
                            builder.add(new TCCLSetup(module.getClassLoader()));
                        }
                        builder.addAll(deployment);
                    }

                    Map<String, Object> properties = Collections.<String, Object> singletonMap(TEST_CLASS_PROPERTY, className);
                    ContextManager contextManager = builder.build();
                    contextManager.setup(properties);

                    try {
                        // actually run the tests
                        return super.runTestMethod(className, methodName, props);
                    } finally {
                        contextManager.teardown(properties);
                    }
                }

                @Override
                protected TestClassLoader getTestClassLoader() {
                    return testClassLoader;
                }
            };
            jmxTestRunner.registerMBean(mbeanServer);
        } catch (Throwable t) {
            throw new StartException("Failed to start Arquillian Test Runner", t);
        }

    }

    public synchronized void stop(StopContext context) {
        log.debugf("Stopping Arquillian Test Runner");
        try {
            if (jmxTestRunner != null) {
                jmxTestRunner.unregisterMBean(injectedMBeanServer.getValue());
            }
        } catch (Exception ex) {
            log.errorf(ex, "Cannot stop Arquillian Test Runner");
        }
    }

    @Override
    public ArquillianService getValue() throws IllegalStateException {
        return this;
    }

    void registerDeployment(ArquillianConfig arqConfig) {
        synchronized (deployedTests) {
            for (String className : arqConfig.getTestClasses()) {
                deployedTests.put(className, arqConfig);
                CountDownLatch latch = waitingTests.remove(className);
                if (latch != null) {
                    latch.countDown();
                }
            }
        }
    }

    void unregisterDeployment(ArquillianConfig arqConfig) {
        synchronized (deployedTests) {
            for (String className : arqConfig.getTestClasses()) {
                deployedTests.remove(className);
            }
        }
    }

    private ArquillianConfig getConfig(String className) {
        CountDownLatch latch = null;
        synchronized (deployedTests) {
            ArquillianConfig config = deployedTests.get(className);
            if (config != null) {
                return config;
            }
            latch = new CountDownLatch(1);
            waitingTests.put(className, latch);
        }

        long end = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                latch.await(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException e) {
            }
        }

        synchronized (deployedTests) {
            waitingTests.remove(className);
            return deployedTests.get(className);
        }
    }

    class TestClassLoaderImpl implements JMXTestRunner.TestClassLoader {

        private ServiceContainer serviceContainer;
        private ServerDeploymentManager deploymentManager;

        TestClassLoaderImpl(ServiceContainer serviceContainer) {
            this.serviceContainer = serviceContainer;
            this.deploymentManager = new ModelControllerServerDeploymentManager(injectedServerController.getValue());
        }

        @Override
        public Class<?> loadTestClass(String className) throws ClassNotFoundException {

            ArquillianConfig arqConfig = getConfig(className);
            if (arqConfig == null)
                throw new ClassNotFoundException(className);

            if (arqConfig.getTestClasses().contains(className) == false)
                throw new ClassNotFoundException(className);

            DeploymentUnit depunit = arqConfig.getDeploymentUnitContext();
            Module module = depunit.getAttachment(Attachments.MODULE);
            Deployment osgidep = OSGiDeploymentAttachment.getDeployment(depunit);
            if (module != null && osgidep != null)
                throw new IllegalStateException("Found MODULE attachment for Bundle deployment: " + depunit);

            Class<?> testClass = null;
            if (module != null) {
                ServiceContainerAssociation.setServiceContainer(serviceContainer);
                ServerDeploymentManagerAssociation.setServerDeploymentManager(deploymentManager);
                testClass = module.getClassLoader().loadClass(className);
            }

            else if (osgidep != null) {
                Bundle bundle = osgidep.getAttachment(Bundle.class);
                BundleAssociation.setBundle(bundle);
                ServiceContainerAssociation.setServiceContainer(serviceContainer);
                ServerDeploymentManagerAssociation.setServerDeploymentManager(deploymentManager);
                testClass = bundle.loadClass(className);
            }

            if (testClass == null)
                throw new ClassNotFoundException(className);

            // [TODO] Move the startup of the OSGi subsystem to a {@link SetupAction}
            if (isOSGiSubsystemRequired(testClass)) {
                ServiceController<?> controller = serviceContainer.getRequiredService(BundleContextService.SERVICE_NAME);
                if (controller.getState() != State.UP) {
                    controller.setMode(Mode.ACTIVE);
                    assertServiceState(BundleContextService.SERVICE_NAME, State.UP, 5000);
                }
                BundleContextAssociation.setBundleContext((BundleContext) controller.getValue());
            }

            return testClass;
        }

        @Override
        public ClassLoader getServiceClassLoader() {
            return ArquillianService.class.getClassLoader();
        }

        boolean isOSGiSubsystemRequired(Class<?> testClass) {
            for (Field field : testClass.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (field.isAnnotationPresent(Inject.class)) {
                    if (fieldType.isAssignableFrom(BundleContext.class)) {
                        return true;
                    } else if (fieldType.isAssignableFrom(Bundle.class)) {
                        return true;
                    }
                }
            }
            return false;
        }

        void assertServiceState(ServiceName serviceName, State expState, long timeout) {
            ServiceController<?> controller = serviceContainer.getService(serviceName);
            State state = controller != null ? controller.getState() : null;
            while ((state == null || state != expState) && timeout > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // ignore
                }
                controller = serviceContainer.getService(serviceName);
                state = controller != null ? controller.getState() : null;
                timeout -= 100;
            }
            if (expState != state)
                throw new IllegalArgumentException(serviceName + " expected: " + expState + " but was " + state);
        }
    }
}
