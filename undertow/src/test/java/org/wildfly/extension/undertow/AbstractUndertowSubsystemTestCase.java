/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.undertow;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.wildfly.extension.undertow.UndertowService.CAP_REF_BUFFER_POOL;
import static org.wildfly.extension.undertow.UndertowService.CAP_REF_IO_WORKER;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.net.ssl.SSLContext;

import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.junit.Assert;
import org.wildfly.extension.io.BufferPoolService;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.io.WorkerService;
import org.wildfly.extension.undertow.filters.FilterRef;
import org.wildfly.extension.undertow.filters.FilterService;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.credential.store.CredentialStore;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
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

    public static void testRuntime(KernelServices mainServices, final String virtualHostName, int flag) throws Exception {
        if (!mainServices.isSuccessfulBoot()) {
            Throwable t = mainServices.getBootError();
            Assert.fail("Boot unsuccessful: " + (t != null ? t.toString() : "no boot error provided"));
        }
        ServiceController<FilterService> connectionLimiter = (ServiceController<FilterService>) mainServices.getContainer()
                .getService(UndertowService.FILTER.append("limit-connections"));
        connectionLimiter.setMode(ServiceController.Mode.ACTIVE);
        FilterService connectionLimiterService = connectionLimiter.getService().getValue();
        HttpHandler result = connectionLimiterService.createHttpHandler(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", result);

        ServiceController<FilterService> headersFilter = (ServiceController<FilterService>) mainServices.getContainer()
                .getService(UndertowService.FILTER.append("headers"));
        headersFilter.setMode(ServiceController.Mode.ACTIVE);
        FilterService headersService = headersFilter.getService().getValue();
        HttpHandler headerHandler = headersService.createHttpHandler(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", headerHandler);

        final ServiceName hostServiceName = UndertowService.virtualHostName(virtualHostName, "other-host");
        ServiceController<Host> hostSC = (ServiceController<Host>) mainServices.getContainer().getService(hostServiceName);
        Assert.assertNotNull(hostSC);
        hostSC.setMode(ServiceController.Mode.ACTIVE);
        Host host = hostSC.getValue();
        Assert.assertEquals(1, host.getFilters().size());
        if (flag == 1) {
            Assert.assertEquals(3, host.getAllAliases().size());
            Assert.assertEquals("default-alias", new ArrayList<>(host.getAllAliases()).get(1));
        }

        final ServiceName locationServiceName = UndertowService.locationServiceName(virtualHostName, "default-virtual-host", "/");
        ServiceController<LocationService> locationSC = (ServiceController<LocationService>) mainServices.getContainer()
                .getService(locationServiceName);
        Assert.assertNotNull(locationSC);
        locationSC.setMode(ServiceController.Mode.ACTIVE);
        LocationService locationService = locationSC.getValue();
        Assert.assertNotNull(locationService);
        connectionLimiter.setMode(ServiceController.Mode.REMOVE);
        final ServiceName servletContainerServiceName = UndertowService.SERVLET_CONTAINER.append("myContainer");
        ServiceController<ServletContainerService> servletContainerService = (ServiceController<ServletContainerService>) mainServices
                .getContainer().getService(servletContainerServiceName);
        Assert.assertNotNull(servletContainerService);
        JSPConfig jspConfig = servletContainerService.getService().getValue().getJspConfig();
        Assert.assertNotNull(jspConfig);
        Assert.assertNotNull(jspConfig.createJSPServletInfo());

        final ServiceName filterRefName = UndertowService.filterRefName(virtualHostName, "other-host", "/", "static-gzip");

        ServiceController<FilterRef> gzipFilterController = (ServiceController<FilterRef>) mainServices.getContainer()
                .getService(filterRefName);
        gzipFilterController.setMode(ServiceController.Mode.ACTIVE);
        FilterRef gzipFilterRef = gzipFilterController.getService().getValue();
        HttpHandler gzipHandler = gzipFilterRef.createHttpHandler(new PathHandler());
        Assert.assertNotNull("handler should have been created", gzipHandler);
    }

    public static void testRuntimeOther(KernelServices mainServices) {
        ServiceController<Host> defaultHostSC = (ServiceController<Host>) mainServices.getContainer()
                .getService(UndertowService.DEFAULT_HOST);
        defaultHostSC.setMode(ServiceController.Mode.ACTIVE);
        Host defaultHost = defaultHostSC.getValue();
        Assert.assertNotNull("Default host should exist", defaultHost);

        ServiceController<Server> defaultServerSC = (ServiceController<Server>) mainServices.getContainer()
                .getService(UndertowService.DEFAULT_SERVER);
        defaultServerSC.setMode(ServiceController.Mode.ACTIVE);
        Server defaultServer = defaultServerSC.getValue();
        Assert.assertNotNull("Default host should exist", defaultServer);
    }

    public static void testRuntimeLast(KernelServices mainServices) {
        final ServiceName accessLogServiceName = UndertowService.accessLogServiceName("some-server", "default-virtual-host");
        ServiceController<AccessLogService> accessLogSC = (ServiceController<AccessLogService>) mainServices.getContainer()
                .getService(accessLogServiceName);
        Assert.assertNotNull(accessLogSC);
        accessLogSC.setMode(ServiceController.Mode.ACTIVE);
        AccessLogService accessLogService = accessLogSC.getValue();
        Assert.assertNotNull(accessLogService);
        Assert.assertFalse(accessLogService.isRotate());
    }


    static final AdditionalInitialization DEFAULT = new DefaultInitialization();
    static final AdditionalInitialization RUNTIME = new RuntimeInitialization();

    private static class DefaultInitialization extends AdditionalInitialization {
        protected final Map<String, Integer> sockets = new HashMap<>();

        {
            sockets.put("ajp", 8009);
            sockets.put("http", 8080);
            sockets.put("http-2", 8081);
            sockets.put("http-3", 8082);
            sockets.put("https-non-default", 8433);
            sockets.put("https-2", 8434);
            sockets.put("https-3", 8435);
            sockets.put("https-4", 8436);
            sockets.put("ajps", 8010);
            sockets.put("test3", 8012);
        }

        @Override
        protected ControllerInitializer createControllerInitializer() {
            return new ControllerInitializer() {
                @Override
                protected void initializeSocketBindingsOperations(List<ModelNode> ops) {
                    super.initializeSocketBindingsOperations(ops);
                    ModelNode op = new ModelNode();
                    op.get(OP).set(ADD);
                    op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME),
                            PathElement.pathElement(SOCKET_BINDING, "advertise-socket-binding")).toModelNode());
                    op.get(PORT).set(8011);
                    op.get(MULTICAST_ADDRESS).set("224.0.1.105");
                    op.get(MULTICAST_PORT).set("23364");
                    ops.add(op);

                }
            };
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }

        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            super.setupController(controllerInitializer);

            for (Map.Entry<String, Integer> entry : sockets.entrySet()) {
                controllerInitializer.addSocketBinding(entry.getKey(), entry.getValue());
            }

            controllerInitializer.addRemoteOutboundSocketBinding("ajp-remote", "localhost", 7777);
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                                                        ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            Map<String, Class> capabilities = new HashMap<>();
            capabilities.put(buildDynamicCapabilityName(CAP_REF_IO_WORKER,
                    ListenerResourceDefinition.WORKER.getDefaultValue().asString()), XnioWorker.class);
            capabilities.put(buildDynamicCapabilityName(CAP_REF_IO_WORKER, "non-default"),
                    XnioWorker.class);
            capabilities.put(buildDynamicCapabilityName(CAP_REF_BUFFER_POOL,
                    ListenerResourceDefinition.BUFFER_POOL.getDefaultValue().asString()), Pool.class);
            capabilities.put(buildDynamicCapabilityName(UndertowService.CAP_REF_HTTP_AUTHENITCATION_FACTORY, "elytron-factory"), HttpAuthenticationFactory.class);
            capabilities.put(buildDynamicCapabilityName(UndertowService.CAP_REF_HTTP_AUTHENITCATION_FACTORY, "factory"), HttpAuthenticationFactory.class);
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "TestContext"), SSLContext.class);
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "my-ssl-context"), SSLContext.class);
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.key-store", "my-key-store"), KeyStore.class);
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.credential-store", "my-credential-store"), CredentialStore.class);

            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "foo"), SSLContext.class);
            //capabilities.put(buildDynamicCapabilityName("org.wildfly.network.outbound-socket-binding","ajp-remote"), OutboundSocketBinding.class);


            registerServiceCapabilities(capabilityRegistry, capabilities);
            registerCapabilities(capabilityRegistry,
                    RuntimeCapability.Builder.of("org.wildfly.network.outbound-socket-binding", true, OutboundSocketBinding.class).build(),
                    RuntimeCapability.Builder.of("org.wildfly.security.ssl-context", true, SSLContext.class).build()
            );


        }
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return DEFAULT;
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

                target.addService(Services.JBOSS_SERVICE_MODULE_LOADER, new ServiceModuleLoader(null)).install();
                target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new NamingStoreService())
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();
                target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, new NamingStoreService())
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();

                target.addService(IOServices.WORKER.append("default"),
                        new WorkerService(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2).getMap()))
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();

                target.addService(IOServices.WORKER.append("non-default"),
                        new WorkerService(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2).getMap()))
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();

                target.addService(ControlledProcessStateService.SERVICE_NAME, new AbstractService<ControlledProcessStateService>() {
                }).install();

                target.addService(IOServices.BUFFER_POOL.append("default"), new BufferPoolService(2048, 2048, true))
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();
                // ListenerRegistry.Listener listener = new ListenerRegistry.Listener("http", "default", "default",
                // InetSocketAddress.createUnresolved("localhost",8080));
                target.addService(HttpListenerAdd.REGISTRY_SERVICE_NAME, new HttpListenerRegistryService())
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();
                SecurityRealmService srs = new SecurityRealmService("UndertowRealm", false);
                final ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "temp");
                target.addService(tmpDirPath, new ValueService<>(new ImmediateValue<>(System.getProperty("java.io.tmpdir"))))
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();
                srs.getSSLContextInjector().inject(sslContext);
                target.addService(SecurityRealm.ServiceUtil.createServiceName("UndertowRealm"),
                        srs)
                        .addDependency(tmpDirPath, String.class, srs.getTmpDirPathInjector())
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();
                SecurityRealmService other = new SecurityRealmService("other", false);
                target.addService(SecurityRealm.ServiceUtil.createServiceName("other"), other)
                        .addDependency(tmpDirPath, String.class, other.getTmpDirPathInjector())
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();

                HttpAuthenticationFactory authenticationFactory = HttpAuthenticationFactory.builder()
                        .build();
                target.addService(ServiceName.parse("org.wildfly.security.http-authentication-factory.factory"),
                        new ValueService<>(new ImmediateValue<>(authenticationFactory)))
                        .install();

                ServiceName sslContextServiceName = ServiceName.parse("org.wildfly.security.ssl-context.TestContext");
                target.addService(sslContextServiceName, new ValueService<>(new ImmediateValue<>(sslContext)))
                        .install();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        }
    }
}
