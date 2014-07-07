/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.io.IOException;
import java.util.List;

import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
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

/**
 * This is the barebone test example that tests subsystem
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class UndertowSubsystem11TestCase extends AbstractSubsystemBaseTest {

    public UndertowSubsystem11TestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("undertow-1.1.xml");
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }

    @Test
    public void testRuntime() throws Exception {
        System.setProperty("server.data.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
        KernelServicesBuilder builder = createKernelServicesBuilder(new RuntimeInitialization())
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

        ServiceController<Host> defaultHostSC = (ServiceController<Host>) mainServices.getContainer().getService(UndertowService.DEFAULT_HOST);
        defaultHostSC.setMode(ServiceController.Mode.ACTIVE);
        Host defaultHost = defaultHostSC.getValue();
        Assert.assertNotNull("Default host should exist", defaultHost);

        ServiceController<Server> defaultServerSC = (ServiceController<Server>) mainServices.getContainer().getService(UndertowService.DEFAULT_SERVER);
        defaultServerSC.setMode(ServiceController.Mode.ACTIVE);
        Server defaultServer = defaultServerSC.getValue();
        Assert.assertNotNull("Default host should exist", defaultServer);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    private static class RuntimeInitialization extends AdditionalInitialization {

        protected ControllerInitializer createControllerInitializer() {
            ControllerInitializer ci = new ControllerInitializer() {

                @Override
                protected void initializeSocketBindingsOperations(List<ModelNode> ops) {

                    super.initializeSocketBindingsOperations(ops);

                    final String[] names = {"ajp", "http", "http-2", "https-non-default", "https-2", "ajps"};
                    final int[] ports = {8009, 8080, 8081, 8433, 8434, 8010};
                    for (int i = 0; i < names.length; i++) {
                        final ModelNode op = new ModelNode();
                        op.get(OP).set(ADD);
                        op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME),
                                PathElement.pathElement(SOCKET_BINDING, names[i])).toModelNode());
                        op.get(PORT).set(ports[i]);
                        ops.add(op);
                    }
                }
            };

            // Adding a socket-binding is what triggers ControllerInitializer to set up the interface
            // and socket-binding-group stuff we depend on TODO something less hacky
            ci.addSocketBinding("make-framework-happy", 59999);
            return ci;
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
