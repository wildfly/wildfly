/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow;

import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import io.undertow.predicate.Predicates;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.junit.Assert;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.io.WorkerService;
import org.wildfly.extension.undertow.filters.FilterRef;
import org.wildfly.extension.undertow.filters.FilterService;
import org.wildfly.extension.undertow.filters.ModClusterService;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

public abstract class AbstractUndertowSubsystemTestCase extends AbstractSubsystemBaseTest {
    public AbstractUndertowSubsystemTestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    public static void setProperty() {
        System.setProperty("server.data.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
    }

    @Override
      protected Properties getResolvedProperties() {
          Properties properties = new Properties();
          properties.put("jboss.home.dir", System.getProperty("java.io.tmpdir"));
          properties.put("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
          properties.put("server.data.dir", System.getProperty("java.io.tmpdir"));
          return properties;
      }

    public static void testRuntime(KernelServices mainServices, final String virtualHostName, int schemaVersion) throws Exception {
        if (!mainServices.isSuccessfulBoot()) {
            Throwable t = mainServices.getBootError();
            Assert.fail("Boot unsuccessful: " + (t != null ? t.toString() : "no boot error provided"));
        }
        ServiceController connectionLimiter = mainServices.getContainer()
                .getService(UndertowService.FILTER.append("limit-connections"));
        connectionLimiter.setMode(ServiceController.Mode.ACTIVE);
        FilterService connectionLimiterService = (FilterService) awaitServiceValue(connectionLimiter);
        HttpHandler result = connectionLimiterService.createHttpHandler(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", result);

        ServiceController headersFilter = mainServices.getContainer().getService(UndertowService.FILTER.append("headers"));
        headersFilter.setMode(ServiceController.Mode.ACTIVE);
        FilterService headersService = (FilterService) awaitServiceValue(headersFilter);
        HttpHandler headerHandler = headersService.createHttpHandler(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", headerHandler);

        if (schemaVersion >= 3) {
            ServiceController modClusterServiceServiceController = mainServices.getContainer()
                    .getService(UndertowService.FILTER.append("mod-cluster"));
            modClusterServiceServiceController.setMode(ServiceController.Mode.ACTIVE);
            ModClusterService modClusterService = (ModClusterService) awaitServiceValue(modClusterServiceServiceController);
            Assert.assertNotNull(modClusterService);
            Assert.assertNotNull(modClusterService.getModCluster());

            HttpHandler modClusterHandler = modClusterService.createHttpHandler(Predicates.truePredicate(), new PathHandler());
            Assert.assertNotNull("handler should have been created", modClusterHandler);
        }


        final ServiceName undertowServiceName = UndertowRootDefinition.UNDERTOW_CAPABILITY.getCapabilityServiceName();
        ServiceController<UndertowService> undertowServiceService = (ServiceController<UndertowService>) mainServices.getContainer().getService(undertowServiceName);
        Assert.assertNotNull(undertowServiceService);
        undertowServiceService.setMode(ServiceController.Mode.ACTIVE);
        final UndertowService undertowService = undertowServiceService.getValue();
        Assert.assertEquals("some-id", undertowService.getInstanceId());
        if (schemaVersion > 1)
            Assert.assertTrue(undertowService.isStatisticsEnabled());
        else
            Assert.assertFalse(undertowService.isStatisticsEnabled());
        if (schemaVersion > 1)
            Assert.assertEquals("some-server", undertowService.getDefaultServer());
        Assert.assertEquals("myContainer", undertowService.getDefaultContainer());
        Assert.assertEquals("default-virtual-host", undertowService.getDefaultVirtualHost());
        Assert.assertEquals(1, undertowService.getServers().size());
        final Server server = undertowService.getServers().iterator().next();
        Assert.assertEquals("other-host", server.getDefaultHost());
        Assert.assertEquals(2, server.getHosts().size());
        if (schemaVersion > 1)
            Assert.assertEquals("some-server", server.getName());


        final ServiceName hostServiceName = HostDefinition.HOST_CAPABILITY.getCapabilityServiceName(virtualHostName, "other-host");
        ServiceController hostSC = mainServices.getContainer().getService(hostServiceName);
        Assert.assertNotNull(hostSC);
        hostSC.setMode(ServiceController.Mode.ACTIVE);
        Host host = (Host) awaitServiceValue(hostSC);
        if (schemaVersion >=3) {
            Assert.assertEquals(3, host.getAllAliases().size());
            Assert.assertTrue(host.getAllAliases().contains("default-alias"));
        }

        final ServiceName locationServiceName = UndertowService.locationServiceName(virtualHostName, "default-virtual-host", "/");
        ServiceController locationSC = mainServices.getContainer().getService(locationServiceName);
        Assert.assertNotNull(locationSC);
        locationSC.setMode(ServiceController.Mode.ACTIVE);
        LocationService locationService = (LocationService)locationSC.getValue();
        Assert.assertNotNull(locationService);
        connectionLimiter.setMode(ServiceController.Mode.REMOVE);
        final ServiceName servletContainerServiceName = UndertowService.SERVLET_CONTAINER.append("myContainer");
        ServiceController servletContainerService = mainServices.getContainer().getService(servletContainerServiceName);
        Assert.assertNotNull(servletContainerService);
        JSPConfig jspConfig = ((ServletContainerService) awaitServiceValue(servletContainerService)).getJspConfig();
        Assert.assertNotNull(jspConfig);
        Assert.assertNotNull(jspConfig.createJSPServletInfo());

        final ServiceName filterRefName = UndertowService.filterRefName(virtualHostName, "other-host", "/", "static-gzip");

        ServiceController gzipFilterController = mainServices.getContainer().getService(filterRefName);
        gzipFilterController.setMode(ServiceController.Mode.ACTIVE);
        FilterRef gzipFilterRef = (FilterRef) awaitServiceValue(gzipFilterController);
        HttpHandler gzipHandler = gzipFilterRef.createHttpHandler(new PathHandler());
        Assert.assertNotNull("handler should have been created", gzipHandler);
        Assert.assertEquals(1, host.getFilters().size());

        ModelNode op = Util.createOperation("write-attribute",
                PathAddress.pathAddress(UndertowExtension.SUBSYSTEM_PATH)
                        .append("servlet-container", "myContainer")
                        .append("setting", "websockets")
        );
        op.get("name").set("buffer-pool");
        op.get("value").set("default");

        ModelNode res = ModelTestUtils.checkOutcome(mainServices.executeOperation(op));
        Assert.assertNotNull(res);

        // WFLY-14648 Check expression in enabled attribute is resolved.
        op = Util.createOperation("write-attribute",
                PathAddress.pathAddress(UndertowExtension.SUBSYSTEM_PATH)
                        .append("server", virtualHostName)
                        .append("http-listener", "default")
        );
        op.get("name").set("enabled");
        op.get("value").set("${env.val:true}");

        res = ModelTestUtils.checkOutcome(mainServices.executeOperation(op));
        Assert.assertNotNull(res);
    }

    public static void testRuntimeOther(KernelServices mainServices) {
        ServiceController defaultHostSC = mainServices.getContainer().getService(UndertowService.DEFAULT_HOST);
        defaultHostSC.setMode(ServiceController.Mode.ACTIVE);
        Host defaultHost = (Host)defaultHostSC.getValue();
        Assert.assertNotNull("Default host should exist", defaultHost);

        ServiceController defaultServerSC = mainServices.getContainer().getService(UndertowService.DEFAULT_SERVER);
        defaultServerSC.setMode(ServiceController.Mode.ACTIVE);
        Server defaultServer = (Server)defaultServerSC.getValue();
        Assert.assertNotNull("Default host should exist", defaultServer);
    }

    public static void testRuntimeLast(KernelServices mainServices) {
        final ServiceName accessLogServiceName = UndertowService.accessLogServiceName("some-server", "default-virtual-host");
        ServiceController accessLogSC = mainServices.getContainer().getService(accessLogServiceName);
        Assert.assertNotNull(accessLogSC);
        accessLogSC.setMode(ServiceController.Mode.ACTIVE);
        AccessLogService accessLogService = (AccessLogService)accessLogSC.getValue();
        Assert.assertNotNull(accessLogService);
        Assert.assertFalse(accessLogService.isRotate());
    }


    static final AdditionalInitialization DEFAULT = new DefaultInitialization();
    static final AdditionalInitialization RUNTIME = new RuntimeInitialization();

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return DEFAULT;
    }

    private static Object awaitServiceValue(ServiceController controller) throws InterruptedException {
        try {
            return controller.awaitValue(2, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            controller.getServiceContainer().dumpServices();
            Assert.fail(String.format("ServiceController %s did not provide a value within 2 minutes; mode is %s and state is %s", controller.getName(), controller.getMode(), controller.getState()));
            throw new IllegalStateException("unreachable");
        }
    }

    private static class RuntimeInitialization extends DefaultInitialization {
        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

        @Override
        protected void addExtraServices(ServiceTarget target) {
            super.addExtraServices(target);
            try {
                SSLContext sslContext = SSLContext.getDefault();

                target.addService(ServiceName.parse(Capabilities.REF_SUSPEND_CONTROLLER)).setInstance(new SuspendController()).install();
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

                target.addService(ControlledProcessStateService.SERVICE_NAME).setInstance(new NullService()).install();

                final ServiceBuilder<?> sb0 = target.addService(ServiceName.parse(Capabilities.CAPABILITY_BYTE_BUFFER_POOL + ".default"));
                final Consumer<DefaultByteBufferPool> dbbpConsumer = sb0.provides(ServiceName.parse(Capabilities.CAPABILITY_BYTE_BUFFER_POOL + ".default"));
                sb0.setInstance(Service.newInstance(dbbpConsumer, new DefaultByteBufferPool(true, 2048)));
                sb0.install();

                // ListenerRegistry.Listener listener = new ListenerRegistry.Listener("http", "default", "default",
                // InetSocketAddress.createUnresolved("localhost",8080));
                target.addService(ServiceName.parse(Capabilities.REF_HTTP_LISTENER_REGISTRY)).setInstance(new HttpListenerRegistryService()).install();
                final ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "temp");
                final ServiceBuilder<?> sb1 = target.addService(tmpDirPath);
                final Consumer<String> c = sb1.provides(tmpDirPath);
                sb1.setInstance(Service.newInstance(c, System.getProperty("java.io.tmpdir")));
                sb1.install();

                final ServiceBuilder<?> sb2 = target.addService(SecurityRealm.ServiceUtil.createServiceName("UndertowRealm"));
                final Consumer<SecurityRealm> securityRealmConsumer = sb2.provides(SecurityRealm.ServiceUtil.createServiceName("UndertowRealm"));
                final Supplier<SSLContext> sslContextSupplier = () -> sslContext;
                final Supplier<String> tmpDirPathSupplier = sb2.requires(tmpDirPath);
                sb2.setInstance(new SecurityRealmService(securityRealmConsumer, null, null, null, sslContextSupplier, tmpDirPathSupplier, null, "UndertowRealm", false));
                sb2.install();

                final ServiceBuilder<?> sb3 = target.addService(SecurityRealm.ServiceUtil.createServiceName("other"));
                final Consumer<SecurityRealm> srConsumer = sb3.provides(SecurityRealm.ServiceUtil.createServiceName("other"));
                final Supplier<String> tdpSupplier = sb3.requires(tmpDirPath);
                sb3.setInstance(new SecurityRealmService(srConsumer, null, null, null, null, tdpSupplier, null, "other", false));
                sb3.install();

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

    private static final class NullService implements org.jboss.msc.service.Service<ControlledProcessStateService> {
        @Override
        public void start(StartContext context) {

        }

        @Override
        public void stop(StopContext context) {

        }

        @Override
        public ControlledProcessStateService getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }
}
