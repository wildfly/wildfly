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

package org.jboss.as.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.deployment.DeploymentService;
import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainProvider.Selector;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderInjector;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderService;
import org.jboss.as.deployment.module.TempFileProviderService;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.osgi.deployment.OSGiDeploymentService;
import org.jboss.as.osgi.parser.OSGiSubsystemState;
import org.jboss.as.osgi.service.BundleManagerService;
import org.jboss.as.osgi.service.Configuration;
import org.jboss.as.osgi.service.FrameworkService;
import org.jboss.as.osgi.service.PackageAdminService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.framework.bundle.BundleManager;
import org.jboss.osgi.framework.bundle.BundleManager.IntegrationMode;
import org.jboss.osgi.framework.plugin.ModuleManagerPlugin;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * OSGi subsystem support.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class OSGiSubsystemSupport {

    private final static Logger log = Logger.getLogger(OSGiSubsystemSupport.class);

    public static final byte[] BLANK_SHA1 = new byte[20];
    private static final AtomicInteger archiveCount = new AtomicInteger();

    private final ServiceContainer serviceContainer;
    private OSGiSubsystemState subsystemState;
    private DeploymentChain deploymentChain;
    private Selector selector;

    public OSGiSubsystemSupport() throws Exception {
        this(OSGiSubsystemState.DEFAULT_CONFIGURATION);
    }

    public OSGiSubsystemSupport(final OSGiSubsystemState subsystemState) throws Exception {
        if (subsystemState == null)
            throw new IllegalArgumentException("Null subsystemState");

        this.subsystemState = subsystemState;

        // Setup the default services
        serviceContainer = ServiceContainer.Factory.create();
        BatchedWork work = new BatchedWork() {
            @Override
            public void execute(BatchBuilder batchBuilder) throws Exception {
                setupServices(batchBuilder);
            }
        };
        runWithLatchedBatch(work);
    }

    public ServiceContainer getServiceContainer() {
        return serviceContainer;
    }

    public ModuleLoader getClassifyingModuleLoader() {
        ServiceController<?> controller = serviceContainer.getService(ClassifyingModuleLoaderService.SERVICE_NAME);
        return controller != null ? ((ClassifyingModuleLoaderService)controller.getValue()).getModuleLoader() : null;
    }

    public TestModuleLoader getTestModuleLoader() {
        return TestModuleLoader.getServiceValue(serviceContainer);
    }

    public DeploymentChain getDeploymentChain() {
        return deploymentChain;
    }

    public String getUniqueName(String prefix) {
        return prefix + archiveCount.incrementAndGet();
    }

    public void assertServiceUp(ServiceName serviceName) {
        ServiceController<?> controller = serviceContainer.getService(serviceName);
        if (controller == null)
            fail("Cannot find registered service: " + serviceName.getCanonicalName());
        if (controller.getState() != State.UP)
            fail("Unexpected service state: " + controller.getState());
    }

    public void assertServiceDown(ServiceName serviceName) {
        ServiceController<?> controller = serviceContainer.getService(serviceName);
        if (controller == null)
            return;
        if (controller.getState() != State.DOWN)
            fail("Unexpected service state: " + controller.getState());
    }

    public void shutdown() {
        if (deploymentChain != null && selector != null)
            removeDeploymentChain();
        if (serviceContainer != null)
            serviceContainer.shutdown();
    }

    public void removeDeploymentChain() {
        DeploymentChainProvider.INSTANCE.removeDeploymentChain(deploymentChain, selector, 0);
    }

    public void setupServices(final BatchBuilder batchBuilder) throws Exception {
        setupEnvironmentServices(batchBuilder);
        setupModuleLoaderServices(batchBuilder);
        setupDeploymentServices(batchBuilder);
        setupFrameworkServices(batchBuilder);
    }

    public void setupEnvironmentServices(final BatchBuilder batchBuilder) {
        ServerEnvironment environment = Mockito.mock(ServerEnvironment.class);
        Mockito.stub(environment.getServerDataDir()).toReturn(new File("./target"));
        ServerEnvironmentService.addService(environment, batchBuilder);
        Configuration.addService(batchBuilder, subsystemState);
    }

    public void setupModuleLoaderServices(final BatchBuilder batchBuilder) {
        batchBuilder.addService(ClassifyingModuleLoaderService.SERVICE_NAME, new ClassifyingModuleLoaderService());
        batchBuilder.addService(TestModuleLoader.SERVICE_NAME, new TestModuleLoader("test")).addDependency(
                ClassifyingModuleLoaderService.SERVICE_NAME);
    }

    public void setupDeploymentServices(final BatchBuilder batchBuilder) {
        batchBuilder.addService(TestServerDeploymentRepository.SERVICE_NAME, new TestServerDeploymentRepository());
        selector = getDeploymentChainSelector();
        deploymentChain = new DeploymentChainImpl("deployment.chain");
        DeploymentChainProvider.INSTANCE.addDeploymentChain(deploymentChain, selector, 0);
        OSGiDeploymentService.enableListener = false;
    }

    public void setupFrameworkServices(final BatchBuilder batchBuilder) {
        batchBuilder.addService(MBeanServerService.SERVICE_NAME, new MBeanServerService());
        TestBundleManagerService.addService(batchBuilder);
        FrameworkService.addService(batchBuilder, Mode.IMMEDIATE);
        PackageAdminService.addService(batchBuilder);
    }

    private DeploymentChainProvider.Selector getDeploymentChainSelector() {
        DeploymentChainProvider.Selector selector;
        selector = new DeploymentChainProvider.Selector() {
            public boolean supports(DeploymentUnitContext context) {
                return true;
            }
        };
        return selector;
    }

    public BundleManager getBundleManager() {
        return TestBundleManagerService.getServiceValue(serviceContainer);
    }

    public BundleContext getSystemContext() {
        return FrameworkService.getServiceValue(serviceContainer);
    }

    public Configuration getSubsystemConfig() {
        return Configuration.getServiceValue(serviceContainer);
    }

    public void assertLoadClass(ModuleIdentifier identifier, String className) throws Exception
    {
       Class<?> clazz = loadClass(identifier, className);
       assertNotNull(clazz);
    }

    public void assertLoadClass(ModuleIdentifier identifier, String className, ModuleIdentifier exporterId) throws Exception
    {
       Class<?> clazz = loadClass(identifier, className);
       assertEquals(loadModule(exporterId).getClassLoader(), clazz.getClassLoader());
    }

    public void assertLoadClassFails(ModuleIdentifier identifier, String className) throws Exception
    {
       try
       {
          loadClass(identifier, className);
          fail("ClassNotFoundException expected");
       }
       catch (ClassNotFoundException ex)
       {
          // expected
       }
    }

    public Class<?> loadClass(ModuleIdentifier identifier, String className) throws Exception
    {
       Class<?> clazz = loadModule(identifier).getClassLoader().loadClass(className, true);
       return clazz;
    }

    public Module loadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        Module module = getClassifyingModuleLoader().loadModule(identifier);
        return module;
    }

    public Bundle executeDeploy(final JavaArchive archive) throws Exception {
        final String depname = archive.getName();
        TestServerDeploymentRepository.getServiceValue(serviceContainer).registerDeploymentArchive(depname, archive);

        BatchedWork work = new BatchedWork() {
            public void execute(BatchBuilder batchBuilder) throws Exception {
                ServerGroupDeploymentElement depElement = new ServerGroupDeploymentElement(depname, depname, BLANK_SHA1, true);
                depElement.activate(new ServiceActivatorContextImpl(batchBuilder), serviceContainer);
            }
        };

        Bundle result = null;
        List<ServiceName> depServiceNames = runWithLatchedBatch(work);
        for (ServiceName sname : depServiceNames) {
            if (sname.getCanonicalName().startsWith(OSGiDeploymentService.SERVICE_NAME.getCanonicalName())) {
                ServiceController<?> controller = serviceContainer.getRequiredService(sname);
                result = ((Deployment) controller.getValue()).getAttachment(Bundle.class);
            }
        }
        return result;
    }

    public void executeUndeploy(final JavaArchive archive) throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        AbstractServiceListener<Object> serviceStopListener = new AbstractServiceListener<Object>() {
            @Override
            public void serviceStopped(ServiceController<?> controller) {
                latch.countDown();
            }
        };

        ServiceName serviceName = DeploymentService.SERVICE_NAME.append(archive.getName());
        ServiceController<?> controller = serviceContainer.getService(serviceName);
        controller.addListener(serviceStopListener);
        controller.setMode(ServiceController.Mode.REMOVE);

        latch.await(5L, TimeUnit.SECONDS);
        if (latch.getCount() != 0)
            fail("Did not uninstall deployment within 5 seconds.");
    }

    List<ServiceName> runWithLatchedBatch(final BatchedWork work) throws Exception {
        final BatchBuilderSupport batchBuilder = new BatchBuilderSupport(serviceContainer.batchBuilder());
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean completed = new AtomicBoolean(false);

        Runnable finishTask = new Runnable() {
            public void run() {
                completed.set(true);
                latch.countDown();
            }
        };
        final TestServiceListener listener = new TestServiceListener(finishTask, batchBuilder.getInitialModes());
        batchBuilder.addListener(listener);

        work.execute(batchBuilder);

        batchBuilder.install();
        listener.finishBatch();
        latch.await(5L, TimeUnit.SECONDS);
        if (!completed.get()) {

            List<ServiceName> notstartedServices = new ArrayList<ServiceName>(listener.registeredServices);
            for (ServiceName aux : listener.startedServices)
                notstartedServices.remove(aux);

            StringBuffer message = new StringBuffer("Did not install services within 5 seconds.");
            message.append("\n registered services: " + listener.registeredServices);
            message.append("\n on-demand services: " + listener.initialModes.get(Mode.ON_DEMAND));
            message.append("\n started services: " + listener.startedServices);
            message.append("\n failed services: " + listener.failedServices);
            message.append("\n not-started services: " + notstartedServices);
            fail(message.toString());
        }

        return Collections.unmodifiableList(listener.registeredServices);
    }

    public interface BatchedWork {
        void execute(final BatchBuilder batchBuilder) throws Exception;
    }

    private static class TestServiceListener extends AbstractServiceListener<Object> {
        final List<ServiceName> registeredServices = new ArrayList<ServiceName>();
        final List<ServiceName> startedServices = new ArrayList<ServiceName>();
        final List<ServiceName> failedServices = new ArrayList<ServiceName>();
        final Map<Mode, List<ServiceName>> initialModes;
        private final AtomicInteger count = new AtomicInteger(1);
        private final Runnable finishTask;

        public TestServiceListener(Runnable finishTask, Map<Mode, List<ServiceName>> initialModes) {
            this.finishTask = finishTask;
            this.initialModes = initialModes;
        }

        public void listenerAdded(final ServiceController<? extends Object> controller) {
            log.debugf("[%d] listenerAdded: %s", count.get(), controller.getName());
            registeredServices.add(controller.getName());
            if (initialModes.get(Mode.ON_DEMAND).contains(controller.getName()) == false)
                count.incrementAndGet();
        }

        public void serviceStarted(final ServiceController<? extends Object> controller) {
            log.debugf("[%d] serviceStarted: %s", count.get(), controller.getName());
            startedServices.add(controller.getName());
            if (count.decrementAndGet() == 0) {
                batchComplete();
            }
            controller.removeListener(this);
        }

        public void serviceFailed(ServiceController<? extends Object> controller, StartException reason) {
            log.debugf(reason, "[%d] serviceFailed: %s", count.get(), controller.getName());
            failedServices.add(controller.getName());
            fail("Service failed to start: " + controller.getName());
            controller.removeListener(this);
        }

        @Override
        public void serviceRemoved(ServiceController<? extends Object> controller) {
            log.debugf("[%d] serviceRemoved: %s", count.get(), controller.getName());
            controller.removeListener(this);
        }

        public void finishBatch() {
            log.debugf("[%d] finishBatch", count.get());
            if (count.decrementAndGet() == 0) {
                batchComplete();
            }
        }

        private void batchComplete() {
            log.debugf("[%d] batchComplete", count.get());
            finishTask.run();
        }
    }

    public static class TestModuleLoader extends ModuleLoader implements Service<TestModuleLoader> {
        public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("module", "loader", "support");

        private String prefix;
        private Map<ModuleIdentifier, ModuleSpec> modules = new HashMap<ModuleIdentifier, ModuleSpec>();
        private Injector<ClassifyingModuleLoaderService> injector;

        TestModuleLoader(String prefix) {
            this.prefix = prefix;
        }

        public void addModuleSpec(ModuleSpec moduleSpec) {
            modules.put(moduleSpec.getModuleIdentifier(), moduleSpec);
        }

        @Override
        protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
            ModuleSpec moduleSpec = modules.get(identifier);
            return moduleSpec;
        }

        public static TestModuleLoader getServiceValue(ServiceContainer container) {
            try {
                ServiceController<?> controller = container.getRequiredService(SERVICE_NAME);
                return (TestModuleLoader) controller.getValue();
            } catch (ServiceNotFoundException ex) {
                throw new IllegalStateException("Cannot obtain required service: " + SERVICE_NAME);
            }
        }

        public synchronized void start(StartContext context) throws StartException {
            ClassifyingModuleLoaderService moduleLoaderService;
            try {
                ServiceContainer serviceContainer = context.getController().getServiceContainer();
                ServiceController<?> controller = serviceContainer
                        .getRequiredService(ClassifyingModuleLoaderService.SERVICE_NAME);
                moduleLoaderService = (ClassifyingModuleLoaderService) controller.getValue();
            } catch (ServiceNotFoundException ex) {
                throw new StartException("Cannot get required service: " + ClassifyingModuleLoaderService.SERVICE_NAME);
            }
            Value<ModuleLoader> value = new ImmediateValue<ModuleLoader>(this);
            injector = new ClassifyingModuleLoaderInjector(prefix, value);
            injector.inject(moduleLoaderService);
        }

        public synchronized void stop(StopContext context) {
            if (injector != null)
                injector.uninject();
        }

        public synchronized TestModuleLoader getValue() throws IllegalStateException {
            return this;
        }

        @Override
        public String toString() {
            return "TestModuleLoader[" + prefix + "]";
        }
    }

    /**
     * In contrary to the {@link BundleManagerService} does this service setup the BundleManager to run in
     * {@link IntegrationMode#STANDALONE}
     *
     * It also does not setup the framework dependent modules.
     */
    static class TestBundleManagerService implements Service<BundleManager> {
        public static final ServiceName SERVICE_NAME = BundleManagerService.SERVICE_NAME;
        private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

        private InjectedValue<Configuration> injectedConfig = new InjectedValue<Configuration>();
        private InjectedValue<ClassifyingModuleLoaderService> injectedModuleLoader = new InjectedValue<ClassifyingModuleLoaderService>();
        private Injector<ClassifyingModuleLoaderService> moduleLoaderInjector;
        private BundleManager bundleManager;

        public static void addService(final BatchBuilder batchBuilder) {
            TestBundleManagerService service = new TestBundleManagerService();
            BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(BundleManagerService.SERVICE_NAME, service);
            serviceBuilder.addDependency(ClassifyingModuleLoaderService.SERVICE_NAME, ClassifyingModuleLoaderService.class, service.injectedModuleLoader);
            serviceBuilder.addDependency(Configuration.SERVICE_NAME, Configuration.class, service.injectedConfig);
        }

        public static BundleManager getServiceValue(ServiceContainer container) {
            try {
                ServiceController<?> controller = container.getRequiredService(SERVICE_NAME);
                return (BundleManager) controller.getValue();
            } catch (ServiceNotFoundException ex) {
                throw new IllegalStateException("Cannot obtain required service: " + SERVICE_NAME);
            }
        }

        public synchronized void start(StartContext context) throws StartException {
            log.debugf("Starting OSGi BundleManager");
            try {
                // Setup the OSGi {@link Framework} properties
                Configuration config = injectedConfig.getValue();
                ServiceContainer serviceContainer = context.getController().getServiceContainer();
                ModuleLoader classifyingModuleLoader = injectedModuleLoader.getValue().getModuleLoader();
                final Map<String, Object> props = new HashMap<String, Object>(config.getProperties());
                props.put(IntegrationMode.class.getName(), IntegrationMode.STANDALONE);
                props.put(ModuleLoader.class.getName(), classifyingModuleLoader);
                props.put(ServiceContainer.class.getName(), serviceContainer);

                // Get {@link ModuleLoader} for the OSGi layer
                bundleManager = new BundleManager(props);
                ModuleManagerPlugin plugin = bundleManager.getPlugin(ModuleManagerPlugin.class);
                ModuleLoader moduleLoader = plugin.getModuleLoader();

                // Register the {@link ModuleLoader} with the {@link ClassifyingModuleLoaderService}
                ServiceController<?> controller = serviceContainer.getRequiredService(ClassifyingModuleLoaderService.SERVICE_NAME);
                ClassifyingModuleLoaderService moduleLoaderService = (ClassifyingModuleLoaderService) controller.getValue();
                Value<ModuleLoader> value = new ImmediateValue<ModuleLoader>(moduleLoader);
                moduleLoaderInjector = new ClassifyingModuleLoaderInjector(Constants.JBOSGI_PREFIX, value);
                moduleLoaderInjector.inject(moduleLoaderService);

            } catch (Throwable t) {
                throw new StartException("Failed to create BundleManager", t);
            }
        }

        public synchronized void stop(StopContext context) {
            log.debugf("Stopping OSGi BundleManager");
            try {
                if (moduleLoaderInjector != null)
                    moduleLoaderInjector.uninject();

                bundleManager = null;

            } catch (Exception ex) {
                log.errorf(ex, "Cannot stop OSGi BundleManager");
            }
        }

        @Override
        public BundleManager getValue() throws IllegalStateException {
            return bundleManager;
        }
    }

    static class TestServerDeploymentRepository implements ServerDeploymentRepository, Service<ServerDeploymentRepository> {

        private Map<String, JavaArchive> repository = new HashMap<String, JavaArchive>();

        public static TestServerDeploymentRepository getServiceValue(ServiceContainer container) {
            try {
                ServiceController<?> controller = container.getRequiredService(SERVICE_NAME);
                return (TestServerDeploymentRepository) controller.getValue();
            } catch (ServiceNotFoundException ex) {
                throw new IllegalStateException("Cannot obtain required service: " + SERVICE_NAME);
            }
        }

        public void registerDeploymentArchive(String name, JavaArchive archive) {
            repository.put(name, archive);
        }

        @Override
        public void start(StartContext context) throws StartException {
        }

        @Override
        public void stop(StopContext context) {
        }

        @Override
        public ServerDeploymentRepository getValue() throws IllegalStateException {
            return this;
        }

        @Override
        public byte[] addDeploymentContent(String name, String runtimeName, InputStream stream) throws IOException {
            return null;
        }

        @Override
        public Closeable mountDeploymentContent(String name, String runtimeName, byte[] deploymentHash, VirtualFile mountPoint)
                throws IOException {
            JavaArchive archive = repository.remove(name);
            ZipExporter exporter = archive.as(ZipExporter.class);
            return VFS.mountZip(exporter.exportZip(), archive.getName(), mountPoint, TempFileProviderService.provider());
        }
    }
}
