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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

import org.jboss.arquillian.protocol.jmx.JMXTestRunner;
import org.jboss.arquillian.protocol.jmx.JMXTestRunner.TestClassLoader;
import org.jboss.arquillian.spi.TestEnricher;
import org.jboss.arquillian.spi.util.ServiceLoader;
import org.jboss.arquillian.testenricher.msc.ServiceContainerInjector;
import org.jboss.arquillian.testenricher.osgi.BundleAssociation;
import org.jboss.arquillian.testenricher.osgi.BundleContextAssociation;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.osgi.deployment.OSGiDeploymentAttachment;
import org.jboss.as.osgi.service.BundleContextService;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
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
    private ServiceContainer serviceContainer;
    private final Map<String, ArquillianConfig> deployedTests = new HashMap<String, ArquillianConfig>();
    private final Map<String, CountDownLatch> waitingTests = new HashMap<String, CountDownLatch>();
    private JMXTestRunner jmxTestRunner;

    public static void addService(final ServiceTarget serviceTarget) {
        ArquillianService service = new ArquillianService();
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(ArquillianService.SERVICE_NAME, service);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
        serviceBuilder.install();
    }

    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting Arquillian Test Runner");

        final MBeanServer mbeanServer = injectedMBeanServer.getValue();
        final TestClassLoader testClassLoader = new TestClassLoaderImpl();

        // Inject the ServiceContainer in the enrichers
        serviceContainer = context.getController().getServiceContainer();
        ServiceLoader<TestEnricher> loader = ServiceLoader.load(TestEnricher.class, testClassLoader.getServiceClassLoader());
        for (TestEnricher enricher : loader.getProviders()) {
            if (enricher instanceof ServiceContainerInjector)
                ((ServiceContainerInjector) enricher).inject(serviceContainer);
        }

        try {
            jmxTestRunner = new JMXTestRunner() {

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

        long end = System.currentTimeMillis() + 3000;
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

        @Override
        public Class<?> loadTestClass(String className) throws ClassNotFoundException {
            Class<?> result = null;
            ArquillianConfig arqConfig = getConfig(className);
            if (arqConfig != null) {
                if (arqConfig.getTestClasses().contains(className)) {
                    DeploymentUnit context = arqConfig.getDeploymentUnitContext();
                    Module module = context.getAttachment(Attachments.MODULE);
                    if (module != null) {
                        result = module.getClassLoader().loadClass(className);
                    }

                    if (result == null) {
                        Deployment dep = OSGiDeploymentAttachment.getAttachment(context);
                        if (dep != null) {
                            Bundle bundle = dep.getAttachment(Bundle.class);
                            result = bundle.loadClass(className);
                            BundleAssociation.setBundle(bundle);
                            BundleContext sysContext = getSystemBundleContext();
                            BundleContextAssociation.setBundleContext(sysContext);
                            result = bundle.loadClass(className);
                        }
                    }
                }
            }
            if (result == null)
                throw new ClassNotFoundException(className);
            return result;
        }

        @Override
        public ClassLoader getServiceClassLoader() {
            return ArquillianService.class.getClassLoader();
        }

        private BundleContext getSystemBundleContext() {
            ServiceController<?> controller = serviceContainer.getService(BundleContextService.SERVICE_NAME);
            return (BundleContext) (controller != null ? controller.getValue() : null);
        }
    }
}
