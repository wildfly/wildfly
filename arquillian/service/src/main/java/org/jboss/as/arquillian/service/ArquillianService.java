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

import static org.jboss.as.server.deployment.Services.JBOSS_DEPLOYMENT;
import static org.jboss.osgi.framework.Services.FRAMEWORK_ACTIVE;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServer;

import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.context.ContextManager;
import org.jboss.arquillian.context.ContextManagerBuilder;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.protocol.jmx.JMXTestRunner;
import org.jboss.arquillian.protocol.jmx.JMXTestRunner.TestClassLoader;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.testenricher.msc.ServiceContainerAssociation;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.osgi.deployment.OSGiDeploymentAttachment;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.client.ModelControllerServerDeploymentManager;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.FutureServiceValue;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.startlevel.StartLevel;

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
    private final Map<ServiceName, ArquillianConfig> deployedTests = new ConcurrentHashMap<ServiceName, ArquillianConfig>();
    private final Map<String, CountDownLatch> waitingTests = new ConcurrentHashMap<String, CountDownLatch>();
    private JMXTestRunner jmxTestRunner;

    @Inject
    @ContainerScoped
    private InstanceProducer<Framework> frameworkInst;

    @Inject
    @ContainerScoped
    private InstanceProducer<BundleContext> bundleContextInst;

    @Inject
    @DeploymentScoped
    private InstanceProducer<Bundle> bundleInst;

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
        final ServiceTarget serviceTarget = context.getChildTarget();

        try {
            jmxTestRunner = new JMXTestRunner(testClassLoader) {

                @Override
                public TestResult runTestMethodRemote(String className, String methodName) {
                    Map<String, Object> properties = Collections.<String, Object> singletonMap(TEST_CLASS_PROPERTY, className);
                    ContextManager contextManager = initializeContextManager(className, properties);
                    try {
                        // actually run the tests
                        return super.runTestMethodRemote(className, methodName);
                    } finally {
                        contextManager.teardown(properties);
                    }
                }

                @Override
                public InputStream runTestMethodEmbedded(String className, String methodName) {
                    Map<String, Object> properties = Collections.<String, Object> singletonMap(TEST_CLASS_PROPERTY, className);
                    ContextManager contextManager = initializeContextManager(className, properties);
                    try {
                        // actually run the tests
                        return super.runTestMethodEmbedded(className, methodName);
                    } finally {
                        contextManager.teardown(properties);
                    }
                }

                private ContextManager initializeContextManager(String className, Map<String, Object> properties) {
                    final ContextManagerBuilder builder = new ContextManagerBuilder();
                    ArquillianConfig config = getConfig(className, 5000);
                    if (config != null) {
                        final DeploymentUnit deployment = config.getDeploymentUnit();
                        final Module module = deployment.getAttachment(Attachments.MODULE);
                        if (module != null) {
                            builder.add(new TCCLSetup(module.getClassLoader()));
                        }
                        builder.addAll(deployment);
                    }

                    ContextManager contextManager = builder.build();
                    contextManager.setup(properties);
                    return contextManager;
                }
            };
            jmxTestRunner.registerMBean(mbeanServer);
        } catch (Throwable t) {
            throw new StartException("Failed to start Arquillian Test Runner", t);
        }

        serviceContainer.addListener(new AbstractServiceListener<Object>() {

            @Override
            public void serviceStarted(ServiceController<? extends Object> controller) {
                ServiceName serviceName = controller.getName();
                String simpleName = serviceName.getSimpleName();
                if (JBOSS_DEPLOYMENT.isParentOf(serviceName) && simpleName.equals(Phase.INSTALL.toString())) {
                    ServiceName parentName = serviceName.getParent();
                    ServiceController<?> parentController = serviceContainer.getService(parentName);
                    DeploymentUnit deploymentUnit = (DeploymentUnit) parentController.getValue();
                    ArquillianConfig arqConfig = new ArquillianRunWithProcessor(deploymentUnit).getArquillianConfig();
                    if (arqConfig != null) {
                        new ArquillianDeploymentProcessor(deploymentUnit).deploy(serviceTarget);
                        new ArquillianDependencyProcessor(deploymentUnit).deploy();
                        registerDeployment(parentName, arqConfig);
                    }
                }
            }

            @Override
            public void serviceStopped(ServiceController<? extends Object> controller) {
                ServiceName serviceName = controller.getName();
                ArquillianConfig arqConfig = deployedTests.remove(serviceName);
                if (arqConfig != null) {
                    DeploymentUnit deploymentUnit = arqConfig.getDeploymentUnit();
                    new ArquillianDeploymentProcessor(deploymentUnit).undeploy();
                }
            }
        });
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

    private void registerDeployment(ServiceName serviceName, ArquillianConfig arqConfig) {
        synchronized (deployedTests) {
            log.infof("Register Arquillian deployment: %s", serviceName);
            deployedTests.put(serviceName, arqConfig);
            for (String className : arqConfig.getTestClasses()) {
                CountDownLatch latch = waitingTests.remove(className);
                if (latch != null) {
                    latch.countDown();
                }
            }
        }
    }

    private ArquillianConfig getConfig(String className, int timeout) {
        CountDownLatch latch = null;
        synchronized (deployedTests) {
            for (ArquillianConfig aux : deployedTests.values()) {
                if (aux.getTestClasses().contains(className)) {
                    waitingTests.remove(className);
                    return aux;
                }
            }
            latch = new CountDownLatch(1);
            waitingTests.put(className, latch);
        }

        if (latch != null && timeout > 0) {
            try {
                log.debugf("Await Arquillian config for: %s", className);
                latch.await(timeout, TimeUnit.MILLISECONDS);
                return getConfig(className, 0);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        return null;
    }

    class TestClassLoaderImpl implements JMXTestRunner.TestClassLoader {

        private ServiceContainer serviceContainer;
        private ServerDeploymentManager deploymentManager;

        TestClassLoaderImpl(ServiceContainer serviceContainer) {
            this.serviceContainer = serviceContainer;
            this.deploymentManager = new ModelControllerServerDeploymentManager(injectedServerController.getValue());
        }

        @Override
        public ClassLoader getServiceClassLoader() {
            return ArquillianService.class.getClassLoader();
        }

        @Override
        public Class<?> loadTestClass(String className) throws ClassNotFoundException {

            ArquillianConfig arqConfig = getConfig(className, 5000);
            if (arqConfig == null)
                throw new ClassNotFoundException(className);

            if (arqConfig.getTestClasses().contains(className) == false)
                throw new ClassNotFoundException(className);

            DeploymentUnit depunit = arqConfig.getDeploymentUnit();
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
                bundleInst.set(bundle);
                ServiceContainerAssociation.setServiceContainer(serviceContainer);
                ServerDeploymentManagerAssociation.setServerDeploymentManager(deploymentManager);
                Framework framework = awaitActiveOSGiFramework();
                frameworkInst.set(framework);
                bundleContextInst.set(framework.getBundleContext());
                testClass = bundle.loadClass(className);
            }

            if (testClass == null)
                throw new ClassNotFoundException(className);

            return testClass;
        }

        @SuppressWarnings("unchecked")
        private Framework awaitActiveOSGiFramework() {
            ServiceController<Framework> controller = (ServiceController<Framework>) serviceContainer.getRequiredService(FRAMEWORK_ACTIVE);
            Future<Framework> future = new FutureServiceValue<Framework>(controller);
            Framework framework;
            try {
                framework = future.get(10, TimeUnit.SECONDS);
                BundleContext context = framework.getBundleContext();
                ServiceReference sref = context.getServiceReference(StartLevel.class.getName());
                StartLevel startLevel = (StartLevel) context.getService(sref);
                if (startLevel.getStartLevel() < 5) {
                    final CountDownLatch latch = new CountDownLatch(1);
                    context.addFrameworkListener(new FrameworkListener() {
                        public void frameworkEvent(FrameworkEvent event) {
                            if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
                                latch.countDown();
                            }
                        }
                    });
                    startLevel.setStartLevel(5);
                    if (latch.await(20, TimeUnit.SECONDS) == false)
                        throw new TimeoutException("Timeout waiting for STARTLEVEL_CHANGED event");
                }
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw new IllegalStateException("Error starting framework", ex);
            }
            return framework;
        }
    }
}
