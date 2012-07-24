/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import javax.management.MBeanServer;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.as.webservices.util.WebAppController;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.EndpointRegistry;

/**
 * WS endpoint service; this is meant for setting the lazy deployment time info into the Endpoint (stuff coming from
 * dependencies upon other AS services that are started during the deployment)
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EndpointService implements Service<Endpoint> {

    private final EndpointBootstrap bootstrap;
    private final ServiceName name;

    private EndpointService(final EndpointBootstrap bootstrap, final ServiceName name) {
        this.name = name;
        this.bootstrap = bootstrap;
    }

    @Override
    public Endpoint getValue() {
        return bootstrap.getEndpoint();
    }

    @Override
    public void start(final StartContext context) throws StartException {
        ROOT_LOGGER.starting(name);
        bootstrap.start();
    }

    @Override
    public void stop(final StopContext context) {
        ROOT_LOGGER.stopping(name);
        bootstrap.stop();
    }

    public static ServiceName getServiceNameDeploymentPrefix(final DeploymentUnit unit) {
        if (unit.getParent() != null) {
            return WSServices.ENDPOINT_SERVICE.append(unit.getParent().getName()).append(unit.getName());
        } else {
            return WSServices.ENDPOINT_SERVICE.append(unit.getName());
        }
    }

    public static void install(final ServiceTarget serviceTarget, final EndpointBootstrap bootstrap, final ServiceName serviceName, final ServiceName secDomainCtxServiceName) {
        final InjectedValue<EndpointRegistry> endpointRegistryInjector = new InjectedValue<EndpointRegistry>();
        bootstrap.setEndpointRegistryValue(endpointRegistryInjector);
        final InjectedValue<MBeanServer> mBeanServerInjector = new InjectedValue<MBeanServer>();
        bootstrap.setmBeanServerValue(mBeanServerInjector);
        final InjectedValue<WebAppController> pclWebAppControllerInjector = new InjectedValue<WebAppController>();
        bootstrap.setPclWebAppControllerValue(pclWebAppControllerInjector);
        final InjectedValue<SecurityDomainContext> securityDomainContextInjector = new InjectedValue<SecurityDomainContext>();
        bootstrap.setSecurityDomainContextValue(securityDomainContextInjector);

        final EndpointService service = new EndpointService(bootstrap, serviceName);

        final ServiceBuilder<Endpoint> builder = serviceTarget.addService(serviceName, service);
        builder.addDependency(DependencyType.REQUIRED, secDomainCtxServiceName, SecurityDomainContext.class, securityDomainContextInjector);
        builder.addDependency(DependencyType.REQUIRED, WSServices.REGISTRY_SERVICE, EndpointRegistry.class, endpointRegistryInjector);
        builder.addDependency(DependencyType.REQUIRED, WSServices.PORT_COMPONENT_LINK_SERVICE, WebAppController.class, pclWebAppControllerInjector);
        builder.addDependency(DependencyType.OPTIONAL, WSServices.MBEAN_SERVICE, MBeanServer.class, mBeanServerInjector);
        builder.setInitialMode(Mode.ACTIVE);
        builder.install();
    }

}
