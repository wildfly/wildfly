/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.service;

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;

import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.as.webservices.util.WebAppController;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.management.ServerConfig;

/**
 * PortComponentLink service, retrieves a WebAppController instance to start/stop port component link servlet on demand; that is
 * required to allow remote port-component-link resolution as per JSR-109 requirements.
 *
 * @author alessio.soldano@jboss.com
 * @since 02-Dec-2011
 */
public final class PortComponentLinkService implements Service<WebAppController> {

    private final ServiceName name;
    private final InjectedValue<VirtualHost> hostInjector = new InjectedValue<VirtualHost>();
    private WebAppController pclwa;
    private final InjectedValue<ServerConfig> serverConfigInjectorValue = new InjectedValue<ServerConfig>();
    private static final String DEFAULT_HOST_NAME = "default-host";

    private PortComponentLinkService() {
        this.name = WSServices.PORT_COMPONENT_LINK_SERVICE;
    }

    @Override
    public WebAppController getValue() {
        return pclwa;
    }

    public ServiceName getName() {
        return name;
    }

    public InjectedValue<VirtualHost> getHostInjector() {
        return hostInjector;
    }

    @Override
    public void start(final StartContext ctx) throws StartException {
        ROOT_LOGGER.starting(name);
        String serverTempDir = serverConfigInjectorValue.getValue().getServerTempDir().getAbsolutePath();
        ClassLoader cl = ClassLoaderProvider.getDefaultProvider().getServerJAXRPCIntegrationClassLoader();
        pclwa = new WebAppController(hostInjector.getValue().getHost(), "org.jboss.ws.core.server.PortComponentLinkServlet",
                cl, "/jbossws", "/pclink", serverTempDir);
    }

    @Override
    public void stop(final StopContext ctx) {
        ROOT_LOGGER.stopping(name);
        pclwa = null;
    }

    public Injector<ServerConfig> getServerConfigInjector() {
        return serverConfigInjectorValue;
    }

    public static ServiceBuilder<WebAppController> createServiceBuilder(final ServiceTarget serviceTarget,
            final String hostName) {
        final PortComponentLinkService service = new PortComponentLinkService();
        final ServiceBuilder<WebAppController> builder = serviceTarget.addService(service.getName(), service);
        builder.addDependency(DependencyType.REQUIRED, WSServices.REGISTRY_SERVICE);
        builder.addDependency(DependencyType.REQUIRED, WSServices.CONFIG_SERVICE, ServerConfig.class,
                service.getServerConfigInjector());
        builder.addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName), VirtualHost.class,
                service.getHostInjector());
        return builder;
    }

    public static ServiceController<WebAppController> install(final ServiceTarget serviceTarget, final ServiceListener<Object>... listeners) {
        return install(serviceTarget, DEFAULT_HOST_NAME, listeners);
    }

    public static ServiceController<WebAppController> install(final ServiceTarget serviceTarget, final String hostName, final ServiceListener<Object>... listeners) {
        ServiceBuilder<WebAppController> builder = createServiceBuilder(serviceTarget, hostName);
        builder.addListener(listeners);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

}
