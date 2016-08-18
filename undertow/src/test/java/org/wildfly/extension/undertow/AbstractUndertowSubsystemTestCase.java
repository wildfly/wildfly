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

import java.util.ArrayList;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.wildfly.extension.undertow.filters.FilterRef;
import org.wildfly.extension.undertow.filters.FilterService;

import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;

public abstract class AbstractUndertowSubsystemTestCase extends AbstractSubsystemBaseTest{
    public AbstractUndertowSubsystemTestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    public static void setProperty() {
        System.setProperty("server.data.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
    }

    public static void testRuntime(KernelServices mainServices, final String virtualHostName, int flag) throws Exception {
        if (!mainServices.isSuccessfulBoot()) {
            Assert.fail(mainServices.getBootError().toString());
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

        final ServiceName locationServiceName = UndertowService.locationServiceName(virtualHostName, "default-host", "/");
        ServiceController<LocationService> locationSC = (ServiceController<LocationService>) mainServices.getContainer()
                .getService(locationServiceName);
        Assert.assertNotNull(locationSC);
        locationSC.setMode(ServiceController.Mode.ACTIVE);
        LocationService locationService = locationSC.getValue();
        Assert.assertNotNull(locationService);
        connectionLimiter.setMode(ServiceController.Mode.REMOVE);
        final ServiceName jspServiceName = UndertowService.SERVLET_CONTAINER.append("myContainer");
        ServiceController<ServletContainerService> jspServiceServiceController = (ServiceController<ServletContainerService>) mainServices
                .getContainer().getService(jspServiceName);
        Assert.assertNotNull(jspServiceServiceController);
        JSPConfig jspConfig = jspServiceServiceController.getService().getValue().getJspConfig();
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
        final ServiceName accessLogServiceName = UndertowService.accessLogServiceName("some-server", "default-host");
        ServiceController<AccessLogService> accessLogSC = (ServiceController<AccessLogService>) mainServices.getContainer()
                .getService(accessLogServiceName);
        Assert.assertNotNull(accessLogSC);
        accessLogSC.setMode(ServiceController.Mode.ACTIVE);
        AccessLogService accessLogService = accessLogSC.getValue();
        Assert.assertNotNull(accessLogService);
        Assert.assertFalse(accessLogService.isRotate());
    }
}
