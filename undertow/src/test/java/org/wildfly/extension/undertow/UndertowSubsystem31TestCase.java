/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.io.BufferPoolService;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.io.WorkerService;
import org.wildfly.extension.undertow.filters.FilterRef;
import org.wildfly.extension.undertow.filters.FilterService;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.XnioWorker;

/**
 * This is the barebone test example that tests subsystem
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class UndertowSubsystem31TestCase extends AbstractSubsystemBaseTest {

    public UndertowSubsystem31TestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("undertow-3.1.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-undertow_3_1.xsd";
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        properties.put("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
        properties.put("server.data.dir", System.getProperty("java.io.tmpdir"));
        return properties;
    }

    @Test
    public void testRuntime() throws Exception {
        System.setProperty("server.data.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
        KernelServicesBuilder builder = createKernelServicesBuilder(RUNTIME)
                .setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Assert.fail(mainServices.getBootError().toString());
        }
        ServiceController<FilterService> connectionLimiter = (ServiceController<FilterService>) mainServices.getContainer().getService(UndertowService.FILTER.append("limit-connections"));
        connectionLimiter.setMode(ServiceController.Mode.ACTIVE);
        FilterService connectionLimiterService = connectionLimiter.getService().getValue();
        HttpHandler result = connectionLimiterService.createHttpHandler(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", result);


        ServiceController<FilterService> headersFilter = (ServiceController<FilterService>) mainServices.getContainer().getService(UndertowService.FILTER.append("headers"));
        headersFilter.setMode(ServiceController.Mode.ACTIVE);
        FilterService headersService = headersFilter.getService().getValue();
        HttpHandler headerHandler = headersService.createHttpHandler(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", headerHandler);

        final ServiceName hostServiceName = UndertowService.virtualHostName("some-server", "other-host");
        ServiceController<Host> hostSC = (ServiceController<Host>) mainServices.getContainer().getService(hostServiceName);
        Assert.assertNotNull(hostSC);
        hostSC.setMode(ServiceController.Mode.ACTIVE);
        Host host = hostSC.getValue();
        Assert.assertEquals(1, host.getFilters().size());
        Assert.assertEquals(3, host.getAllAliases().size());
        Assert.assertEquals("default-alias", new ArrayList<>(host.getAllAliases()).get(1));


        final ServiceName locationServiceName = UndertowService.locationServiceName("some-server", "default-host", "/");
        ServiceController<LocationService> locationSC = (ServiceController<LocationService>) mainServices.getContainer().getService(locationServiceName);
        Assert.assertNotNull(locationSC);
        locationSC.setMode(ServiceController.Mode.ACTIVE);
        LocationService locationService = locationSC.getValue();
        Assert.assertNotNull(locationService);
        connectionLimiter.setMode(ServiceController.Mode.REMOVE);
        final ServiceName jspServiceName = UndertowService.SERVLET_CONTAINER.append("myContainer");
        ServiceController<ServletContainerService> jspServiceServiceController = (ServiceController<ServletContainerService>) mainServices.getContainer().getService(jspServiceName);
        Assert.assertNotNull(jspServiceServiceController);
        JSPConfig jspConfig = jspServiceServiceController.getService().getValue().getJspConfig();
        Assert.assertNotNull(jspConfig);
        Assert.assertNotNull(jspConfig.createJSPServletInfo());

        final ServiceName filterRefName = UndertowService.filterRefName("some-server", "other-host", "/", "static-gzip");

        ServiceController<FilterRef> gzipFilterController = (ServiceController<FilterRef>) mainServices.getContainer().getService(filterRefName);
        gzipFilterController.setMode(ServiceController.Mode.ACTIVE);
        FilterRef gzipFilterRef = gzipFilterController.getService().getValue();
        HttpHandler gzipHandler = gzipFilterRef.createHttpHandler(new PathHandler());
        Assert.assertNotNull("handler should have been created", gzipHandler);

        //testCustomFilters(mainServices);

        ServiceController<Host> defaultHostSC = (ServiceController<Host>) mainServices.getContainer().getService(UndertowService.DEFAULT_HOST);
        defaultHostSC.setMode(ServiceController.Mode.ACTIVE);
        Host defaultHost = defaultHostSC.getValue();
        Assert.assertNotNull("Default host should exist", defaultHost);

        ServiceController<Server> defaultServerSC = (ServiceController<Server>) mainServices.getContainer().getService(UndertowService.DEFAULT_SERVER);
        defaultServerSC.setMode(ServiceController.Mode.ACTIVE);
        Server defaultServer = defaultServerSC.getValue();
        Assert.assertNotNull("Default host should exist", defaultServer);


        final ServiceName accessLogServiceName = UndertowService.accessLogServiceName("some-server", "default-host");
        ServiceController<AccessLogService> accessLogSC = (ServiceController<AccessLogService>) mainServices.getContainer().getService(accessLogServiceName);
        Assert.assertNotNull(accessLogSC);
        accessLogSC.setMode(ServiceController.Mode.ACTIVE);
        AccessLogService accessLogService = accessLogSC.getValue();
        Assert.assertNotNull(accessLogService);
        Assert.assertFalse(accessLogService.isRotate());

    }

    private void testCustomFilters(KernelServices mainServices) {
        ServiceController<FilterService> customFilter = (ServiceController<FilterService>) mainServices.getContainer().getService(UndertowService.FILTER.append("custom-filter"));
        customFilter.setMode(ServiceController.Mode.ACTIVE);
        FilterService connectionLimiterService = customFilter.getService().getValue();
        HttpHandler result = connectionLimiterService.createHttpHandler(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", result);

    }

    static final AdditionalInitialization DEFAULT = new DefaultInitialization();
    static final AdditionalInitialization RUNTIME = new RuntimeInitialization();

    private static class DefaultInitialization extends AdditionalInitialization {
        protected final Map<String, Integer> sockets = new HashMap<>();

        {
            sockets.put("ajp", 8009);
            sockets.put("http", 8080);
            sockets.put("http-2", 8081);
            sockets.put("https-non-default", 8433);
            sockets.put("https-2", 8434);
            sockets.put("ajps", 8010);
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            Map<String, Class> capabilities = new HashMap<>();
            capabilities.put(buildDynamicCapabilityName(ListenerResourceDefinition.IO_WORKER_CAPABILITY, ListenerResourceDefinition.WORKER.getDefaultValue().asString()), XnioWorker.class);
            capabilities.put(buildDynamicCapabilityName(ListenerResourceDefinition.IO_WORKER_CAPABILITY, "non-default"), XnioWorker.class);
            capabilities.put(buildDynamicCapabilityName(ListenerResourceDefinition.IO_BUFFER_POOL_CAPABILITY, ListenerResourceDefinition.BUFFER_POOL.getDefaultValue().asString()), Pool.class);
            for (String entry : sockets.keySet()) {
                capabilities.put(buildDynamicCapabilityName(ListenerResourceDefinition.SOCKET_CAPABILITY, entry), SocketBinding.class);
            }
            registerServiceCapabilities(capabilityRegistry, capabilities);

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
        protected void setupController(ControllerInitializer controllerInitializer) {
            super.setupController(controllerInitializer);

            for (Map.Entry<String, Integer> entry : sockets.entrySet()) {
                controllerInitializer.addSocketBinding(entry.getKey(), entry.getValue());
            }
        }

        @Override
        protected void addExtraServices(ServiceTarget target) {
            super.addExtraServices(target);
            target.addService(Services.JBOSS_SERVICE_MODULE_LOADER, new ServiceModuleLoader(null)).install();
            target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new NamingStoreService())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, new NamingStoreService())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            target.addService(IOServices.WORKER.append("default"), new WorkerService(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2).getMap()))
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            target.addService(IOServices.WORKER.append("non-default"), new WorkerService(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2).getMap()))
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            target.addService(IOServices.BUFFER_POOL.append("default"), new BufferPoolService(2048, 2048, true))
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            //ListenerRegistry.Listener listener = new ListenerRegistry.Listener("http", "default", "default", InetSocketAddress.createUnresolved("localhost",8080));
            target.addService(HttpListenerAdd.REGISTRY_SERVICE_NAME, new HttpListenerRegistryService())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            target.addService(SecurityRealm.ServiceUtil.createServiceName("UndertowRealm"), new SecurityRealmService("UndertowRealm", false))
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            target.addService(SecurityRealm.ServiceUtil.createServiceName("other"), new SecurityRealmService("other", false))
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

        }
    }
}
