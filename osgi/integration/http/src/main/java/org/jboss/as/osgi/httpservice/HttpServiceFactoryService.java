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

package org.jboss.as.osgi.httpservice;

import java.util.Hashtable;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.as.web.host.WebDeploymentController;
import org.jboss.as.web.host.WebHost;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * A service that installs a {@link ServiceFactory} for {@link HttpService}
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 19-Jul-2012
 */
final class HttpServiceFactoryService implements Service<WebDeploymentController> {

    static final ServiceName JBOSS_WEB_HTTPSERVICE_FACTORY = CommonWebServer.SERVICE_NAME.append("httpservice", "factory");
    // [TODO] Make these configurable
    static final String VIRTUAL_HOST = "default-host";
    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<PathManager> injectedPathManager = new InjectedValue<PathManager>();
    private final InjectedValue<WebHost> injectedVirtualHost = new InjectedValue<WebHost>();
    private final InjectedValue<HttpManagement> injectedHttpManagement = new InjectedValue<HttpManagement>();
    private final InjectedValue<CommonWebServer> injectedWebServer = new InjectedValue<CommonWebServer>();
    private ServiceRegistration<?> registration;

    private HttpServiceFactoryService() {
    }

    static ServiceController<WebDeploymentController> addService(ServiceTarget serviceTarget) {
        HttpServiceFactoryService service = new HttpServiceFactoryService();
        ServiceBuilder<WebDeploymentController> builder = serviceTarget.addService(JBOSS_WEB_HTTPSERVICE_FACTORY, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment);
        builder.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.injectedPathManager);
        builder.addDependency(WebHost.SERVICE_NAME.append(VIRTUAL_HOST), WebHost.class, service.injectedVirtualHost);
        builder.addDependency(CommonWebServer.SERVICE_NAME, CommonWebServer.class, service.injectedWebServer);
        builder.addDependency(UndertowHttpManagementService.SERVICE_NAME, HttpManagement.class, service.injectedHttpManagement);
        builder.addDependency(Services.FRAMEWORK_CREATE, BundleContext.class, service.injectedSystemContext);
        return builder.install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        ServerEnvironment serverEnvironment = injectedServerEnvironment.getValue();
        WebHost virtualHost = injectedVirtualHost.getValue();
        BundleContext syscontext = injectedSystemContext.getValue();
        CommonWebServer webServer = injectedWebServer.getValue();

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("provider", getClass().getPackage().getName());

        ServiceFactory<HttpService> serviceFactory = new HttpServiceFactory(webServer, virtualHost, serverEnvironment);
        registration = syscontext.registerService(HttpService.class.getName(), serviceFactory, props);
    }

    @Override
    public void stop(StopContext stopContext) {
        registration.unregister();
    }

    @Override
    public WebDeploymentController getValue() throws IllegalStateException {
        return null;
    }
}
