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

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

import org.jboss.arquillian.protocol.jmx.JMXTestRunner;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.testenricher.osgi.BundleAssociation;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.osgi.deployment.OSGiDeploymentAttachment;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.SetupAction;
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
import org.osgi.framework.Bundle;

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
    private final Map<String, ArquillianConfig> deployedTests = new ConcurrentHashMap<String, ArquillianConfig>();
    private final Map<String, CountDownLatch> waitingTests = new ConcurrentHashMap<String, CountDownLatch>();
    private ServiceContainer serviceContainer;
    private JMXTestRunner jmxTestRunner;

    public static void addService(final ServiceTarget serviceTarget) {
        ArquillianService service = new ArquillianService();
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(ArquillianService.SERVICE_NAME, service);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
        serviceBuilder.install();
    }

    public static final String TEST_CLASS_PROPERTY = "org.jboss.as.arquillian.testClass";

    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting Arquillian Test Runner");

        final MBeanServer mbeanServer = injectedMBeanServer.getValue();
        serviceContainer = context.getController().getServiceContainer();
        try {
            jmxTestRunner = new ExtendedJMXTestRunner();
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
                    ArquillianRunWithProcessor processor = new ArquillianRunWithProcessor(serviceContainer, serviceName, deploymentUnit);
                    ArquillianConfig arqConfig = processor.process().getValue();
                    if (arqConfig != null) {
                        new BundleContextProcessor(serviceContainer, deploymentUnit).process();
                        registerConfig(arqConfig);
                    }
                }
            }

            @Override
            public void serviceStopped(ServiceController<? extends Object> controller) {
                ServiceName serviceName = controller.getName();
                ArquillianConfig arqConfig = deployedTests.get(serviceName);
                if (arqConfig != null) {
                    unregisterConfig(arqConfig);
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

    private void registerConfig(final ArquillianConfig arqConfig) {
        synchronized (deployedTests) {
            log.debugf("Register Arquillian Deployment: %s", arqConfig.getDeploymentUnit());
            for (String className : arqConfig.getTestClasses()) {
                log.infof("Register Arquillian test class: %s", className);
                deployedTests.put(className, arqConfig);
                final CountDownLatch latch = waitingTests.remove(className);
                if (latch != null) {
                    latch.countDown();
                }
            }
        }
    }

    private void unregisterConfig(final ArquillianConfig arqConfig) {
        synchronized (deployedTests) {
            log.debugf("Unregister Arquillian Deployment: %s", arqConfig.getDeploymentUnit());
            for (String className : arqConfig.getTestClasses()) {
                log.infof("Unregister Arquillian test class: %s", className);
                deployedTests.remove(className);
                waitingTests.remove(className);
            }
        }
    }

    private ArquillianConfig getConfig(final String className) {
        CountDownLatch latch = null;
        synchronized (deployedTests) {
            ArquillianConfig arqConfig = deployedTests.get(className);
            if (arqConfig != null) {
                waitingTests.remove(className);
                return arqConfig;
            }
            latch = new CountDownLatch(1);
            waitingTests.put(className, latch);
        }

        try {
            latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        synchronized (deployedTests) {
            waitingTests.remove(className);
            return deployedTests.get(className);
        }
    }

    class ExtendedJMXTestRunner extends JMXTestRunner {

        ExtendedJMXTestRunner() {
            super(new ExtendedTestClassLoader());
        }

        @Override
        public TestResult runTestMethodRemote(final String className, final String methodName) {
            Map<String, Object> properties = Collections.<String, Object> singletonMap(TEST_CLASS_PROPERTY, className);
            ContextManager contextManager = initializeContextManager(className, properties);
            try {
                return super.runTestMethodRemote(className, methodName);
            } finally {
                contextManager.teardown(properties);
            }
        }

        @Override
        public InputStream runTestMethodEmbedded(final String className, final String methodName) {
            Map<String, Object> properties = Collections.<String, Object> singletonMap(TEST_CLASS_PROPERTY, className);
            ContextManager contextManager = initializeContextManager(className, properties);
            try {
                return super.runTestMethodEmbedded(className, methodName);
            } finally {
                contextManager.teardown(properties);
            }
        }

        private ContextManager initializeContextManager(final String className, final Map<String, Object> properties) {
            final ContextManagerBuilder builder = new ContextManagerBuilder();
            final ArquillianConfig config = getConfig(className);
            if (config != null) {
                final DeploymentUnit deployment = config.getDeploymentUnit();
                final Module module = deployment.getAttachment(Attachments.MODULE);
                if (module != null) {
                    builder.add(new TCCLSetupAction(module.getClassLoader()));
                }
                builder.addAll(deployment);
            }
            ContextManager contextManager = builder.build();
            contextManager.setup(properties);
            return contextManager;
        }
    }

    class ExtendedTestClassLoader implements JMXTestRunner.TestClassLoader {

        @Override
        public Class<?> loadTestClass(final String className) throws ClassNotFoundException {

            final ArquillianConfig arqConfig = getConfig(className);
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
                // ServiceContainerAssociation.setServiceContainer(serviceContainer);
                testClass = module.getClassLoader().loadClass(className);
            }

            else if (osgidep != null) {
                Bundle bundle = osgidep.getAttachment(Bundle.class);
                testClass = bundle.loadClass(className);
                BundleAssociation.setBundle(bundle);
            }

            if (testClass == null)
                throw new ClassNotFoundException(className);

            return testClass;
        }
    }

    /**
     * Sets and restores the Thread Context ClassLoader
     *
     * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
     * @author Stuart Douglas
     */
    private static final class TCCLSetupAction implements SetupAction {
        private final ThreadLocal<ClassLoader> oldClassLoader = new ThreadLocal<ClassLoader>();

        private final ClassLoader classLoader;

        TCCLSetupAction(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public int priority() {
            return 10000;
        }

        @Override
        public void setup(Map<String, Object> properties) {
            oldClassLoader.set(getTccl());
            setTccl(classLoader);
        }

        @Override
        public void teardown(Map<String, Object> properties) {
            ClassLoader old = oldClassLoader.get();
            oldClassLoader.remove();
            setTccl(old);
        }
    }

    /**
     * {@link PrivilegedAction} implementation to get at the TCCL
     *
     * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
     */
    private enum GetTcclAction implements PrivilegedAction<ClassLoader> {
        INSTANCE;

        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    }

    private static ClassLoader getTccl() {
        return AccessController.doPrivileged(GetTcclAction.INSTANCE);
    }

    private static void setTccl(final ClassLoader cl) {
        assert cl != null : "ClassLoader must be specified";
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(cl);
                return null;
            }
        });
    }
}
