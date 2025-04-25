/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.mockito.Mockito;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.io.WorkerService;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.service.ServiceInstaller;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

class RuntimeInitialization extends DefaultInitialization {
    private final Map<ServiceName, Supplier<Object>> values;

    RuntimeInitialization(final Map<ServiceName, Supplier<Object>> values, final UndertowSubsystemSchema schema) {
        super(schema);
        this.values = values;
    }

    private void record(ServiceTarget target, ServiceName name) {
        this.record(target, name, ServiceController.Mode.PASSIVE);
    }

    private void start(ServiceTarget target, ServiceName name) {
        this.record(target, name, ServiceController.Mode.ACTIVE);
    }

    private void record(ServiceTarget target, ServiceName name, ServiceController.Mode mode) {
        ServiceBuilder<?> builder = target.addService();
        this.values.put(name, builder.requires(name));
        builder.setInstance(Service.NULL).setInitialMode(mode).install();
    }

    @Override
    protected RunningMode getRunningMode() {
        return RunningMode.NORMAL;
    }

    @Override
    protected void addExtraServices(ServiceTarget target) {
        // AbstractUndertowSubsystemTestCase.testRuntime(...) methods require the recording of specific service values, and requires starting specific on-demand services
        // TODO Consider removing those testRuntime(...) methods - the value of such testing is questionable
        if (this.values != null) {
            this.start(target, UndertowService.FILTER.append("limit-connections"));
            this.start(target, UndertowService.FILTER.append("headers"));
            this.start(target, UndertowService.FILTER.append("mod-cluster"));
            this.record(target, UndertowRootDefinition.UNDERTOW_CAPABILITY.getCapabilityServiceName());
            this.record(target, ServerDefinition.SERVER_CAPABILITY.getCapabilityServiceName("some-server"));
            this.start(target, HostDefinition.HOST_CAPABILITY.getCapabilityServiceName("some-server", "default-virtual-host"));
            this.start(target, HostDefinition.HOST_CAPABILITY.getCapabilityServiceName("some-server", "other-host"));
            this.record(target, LocationDefinition.LOCATION_CAPABILITY.getCapabilityServiceName("some-server", "default-virtual-host", "/"));
            this.start(target, ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName("myContainer"));
            this.start(target, UndertowService.filterRefName("some-server", "other-host", "/", "static-gzip"));
            this.start(target, UndertowService.filterRefName("some-server", "other-host", "headers"));
            this.record(target, UndertowService.DEFAULT_HOST);
            this.record(target, UndertowService.DEFAULT_SERVER);
            this.record(target, AccessLogDefinition.ACCESS_LOG_CAPABILITY.getCapabilityServiceName("some-server", "default-virtual-host"));
            this.record(target, ServerDefinition.SERVER_CAPABILITY.getCapabilityServiceName("undertow-server"));
            this.record(target, ServerDefinition.SERVER_CAPABILITY.getCapabilityServiceName("default-server"));
        }
        try {
            SSLContext sslContext = SSLContext.getDefault();

            SuspendController suspendController = new SuspendController();
            ServiceInstaller.builder(suspendController).provides(ServiceName.parse(SuspendableActivityRegistry.SERVICE_DESCRIPTOR.getName())).build().install(target);

            target.addService(Services.JBOSS_SERVICE_MODULE_LOADER).setInstance(new ServiceModuleLoader(null)).install();
            target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME).setInstance(new NamingStoreService()).install();
            target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME).setInstance(new NamingStoreService()).install();

            ServiceBuilder<?> builder1 = target.addService(IOServices.WORKER.append("default"));
            Consumer<XnioWorker> workerConsumer1 = builder1.provides(IOServices.WORKER.append("default"));
            builder1.setInstance(
                    new WorkerService(
                            workerConsumer1,
                            () -> Executors.newFixedThreadPool(1),
                            Xnio.getInstance().createWorkerBuilder().populateFromOptions(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2).getMap())));
            builder1.install();

            ServiceBuilder<?> builder2 = target.addService(IOServices.WORKER.append("non-default"));
            Consumer<XnioWorker> workerConsumer2 = builder2.provides(IOServices.WORKER.append("non-default"));
            builder2.setInstance(
                    new WorkerService(
                            workerConsumer2,
                            () -> Executors.newFixedThreadPool(1),
                            Xnio.getInstance().createWorkerBuilder().populateFromOptions(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2).getMap())));
            builder2.install();

            ProcessStateNotifier notifier = Mockito.mock(ProcessStateNotifier.class);
            Mockito.doReturn(ControlledProcessState.State.RUNNING).when(notifier).getCurrentState();
            ServiceInstaller.builder(notifier).provides(ServiceName.parse(ProcessStateNotifier.SERVICE_DESCRIPTOR.getName())).build().install(target);

            // ListenerRegistry.Listener listener = new ListenerRegistry.Listener("http", "default", "default",
            // InetSocketAddress.createUnresolved("localhost",8080));
            target.addService(ServiceName.parse(Capabilities.REF_HTTP_LISTENER_REGISTRY)).setInstance(new HttpListenerRegistryService()).install();
            final ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "temp");
            final ServiceBuilder<?> sb1 = target.addService(tmpDirPath);
            final Consumer<String> c = sb1.provides(tmpDirPath);
            sb1.setInstance(Service.newInstance(c, System.getProperty("java.io.tmpdir")));
            sb1.install();

            HttpAuthenticationFactory authenticationFactory = HttpAuthenticationFactory.builder()
                    .build();
            final ServiceBuilder<?> sb4 = target.addService(ServiceName.parse("org.wildfly.security.http-authentication-factory.factory"));
            final Consumer<HttpAuthenticationFactory> hafConsumer = sb4.provides(ServiceName.parse("org.wildfly.security.http-authentication-factory.factory"));
            sb4.setInstance(Service.newInstance(hafConsumer, authenticationFactory));
            sb4.install();

            ServiceName sslContextServiceName = ServiceName.parse("org.wildfly.security.ssl-context.TestContext");
            final ServiceBuilder<?> sb5 = target.addService(sslContextServiceName);
            final Consumer<SSLContext> scConsumer = sb5.provides(sslContextServiceName);
            sb5.setInstance(Service.newInstance(scConsumer, sslContext));
            sb5.install();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}