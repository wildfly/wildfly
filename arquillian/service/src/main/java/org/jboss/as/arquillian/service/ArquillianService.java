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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.as.arquillian.jmx.JMXTestRunner;
import org.jboss.as.arquillian.jmx.JMXTestRunner.TestClassLoader;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderService;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

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
    private final InjectedValue<ClassifyingModuleLoaderService> injectedModuleLoader = new InjectedValue<ClassifyingModuleLoaderService>();
    private final Map<String, Class<?>> deployedTests = new HashMap<String, Class<?>>();
    private JMXTestRunner jmxTestRunner;

    public static void addService(final BatchBuilder batchBuilder) {
        ArquillianService service = new ArquillianService();
        BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(ArquillianService.SERVICE_NAME, service);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
        serviceBuilder.addDependency(ClassifyingModuleLoaderService.SERVICE_NAME, ClassifyingModuleLoaderService.class, service.injectedModuleLoader);
    }

    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting Arquillian Test Runner");

        final MBeanServer mbeanServer = injectedMBeanServer.getValue();
        final TestClassLoader testClassLoader = new TestClassLoaderImpl();

        // Inject the ServiceContainer in the enrichers
        log.warn("ServiceContainerInjector temporarily disabled during porting of code");
//        ServiceContainer serviceContainer = context.getController().getServiceContainer();
//        ServiceLoader<TestEnricher> loader = ServiceLoader.load(TestEnricher.class, testClassLoader.getServiceClassLoader());
//        for (TestEnricher enricher : loader.getProviders()) {
//            if (enricher instanceof ServiceContainerInjector)
//                ((ServiceContainerInjector) enricher).inject(serviceContainer);
//        }

        try {
            jmxTestRunner = new JMXTestRunner() {

                @Override
                protected ObjectName getObjectName() {
                    return OBJECT_NAME;
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

    void registerDeployment(DeploymentUnitContext context) {
        synchronized (deployedTests) {
            ArquillianConfig config = context.getAttachment(ArquillianConfig.ATTACHMENT_KEY);
            for (Class<?> clazz : config.getTestClasses()) {
                if (deployedTests.containsKey(clazz.getName())) {
                    log.warn("Not registering existing class " + clazz);
                    config.removeTestClass(clazz);
                }
                System.out.println("------> Registering " + clazz.getName());
                deployedTests.put(clazz.getName(), clazz);
            }
        }
    }

    void unregisterDeployment(DeploymentUnitContext context) {
        synchronized (deployedTests) {
            ArquillianConfig config = context.getAttachment(ArquillianConfig.ATTACHMENT_KEY);
            for (Class<?> clazz : config.getTestClasses()) {
                System.out.println("------> Removing " + clazz.getName());
                if (deployedTests.remove(clazz.getName()) == null){
                    log.warn("Could not find class " + clazz.getName());
                }
            }
        }
    }

    class TestClassLoaderImpl implements JMXTestRunner.TestClassLoader {

        @Override
        public Class<?> loadTestClass(String className) throws ClassNotFoundException {
            synchronized (deployedTests) {
                System.out.println("------> Looking for " + className);

                Class<?> result = deployedTests.get(className);
                if (result == null)
                    throw new ClassNotFoundException(className);
                return result;
            }
        }

        @Override
        public ClassLoader getServiceClassLoader() {
            return ArquillianService.class.getClassLoader();
        }
    }
}
