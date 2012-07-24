/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.webservices.deployers;

import static org.jboss.ws.common.integration.WSHelper.getOptionalAttachment;
import static org.jboss.ws.common.integration.WSHelper.getRequiredAttachment;

import javax.management.MBeanServer;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.service.EndpointBootstrap;
import org.jboss.as.webservices.service.EndpointService;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.jboss.ws.common.deployment.EndpointLifecycleDeploymentAspect;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.LifecycleHandler;
import org.jboss.wsf.spi.management.EndpointRegistry;
import org.jboss.wsf.spi.management.EndpointRegistryFactory;

/**
 * Creates Endpoint Service instance when starting the Endpoint
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EndpointServiceDeploymentAspect extends EndpointLifecycleDeploymentAspect {

    @Override
    public void start(final Deployment dep) {
        final ServiceTarget target = getOptionalAttachment(dep, ServiceTarget.class);
        final ServiceName securityDomainServiceName = SecurityDomainService.SERVICE_NAME.append(ASHelper.getDeploymentSecurityDomainName(dep));
        if (target != null) {
            //Starting endpoints during AS7 boot -> ServiceTarget available -> we install new AS7 services
            final DeploymentUnit unit = getRequiredAttachment(dep, DeploymentUnit.class);
            for (final Endpoint ep : dep.getService().getEndpoints()) {
                final ServiceName serviceName = EndpointService.getServiceNameDeploymentPrefix(unit).append(ep.getShortName());
                EndpointService.install(target, new EndpointBootstrap(ep), serviceName, securityDomainServiceName);
                getLifecycleHandler(ep, true).start(ep);
            }
        } else {
            //Starting endpoints through JBossWS SPI EndpointPublisher -> no ServiceTarget available -> manual Endpoint boot
            //We try looking up required service dependencies in the current MSC ServiceRegistry, which should just be fine here
            //(this and corresponding stop method are run inside user deployments code)

            //get endpoint registry using configured factory, which in turn should get the current AS7 endpoint registry if available
            final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
            final EndpointRegistry epRegistry = spiProvider.getSPI(EndpointRegistryFactory.class).getEndpointRegistry();
            //get optional mbean server, if available
            final Value<MBeanServer> mBeanServerValue = getMSCService(WSServices.MBEAN_SERVICE, MBeanServer.class);
            final Value<SecurityDomainContext> securityDomainContextValue = getMSCService(securityDomainServiceName, SecurityDomainContext.class);

            for (final Endpoint ep : dep.getService().getEndpoints()) {
                EndpointBootstrap bootstrap = new EndpointBootstrap(ep);
                bootstrap.setEndpointRegistryValue(new ImmediateValue<EndpointRegistry>(epRegistry));
                bootstrap.setmBeanServerValue(mBeanServerValue);
                bootstrap.setSecurityDomainContextValue(securityDomainContextValue);
                bootstrap.start();
                ep.addAttachment(EndpointBootstrap.class, bootstrap);
                getLifecycleHandler(ep, true).start(ep);
            }
        }
    }

    @Override
    public void stop(final Deployment dep) {
        for (final Endpoint ep : dep.getService().getEndpoints()) {
            EndpointBootstrap bootstrap = ep.getAttachment(EndpointBootstrap.class);
            if (bootstrap != null) {
                bootstrap.stop();
            }
            LifecycleHandler lifecycleHandler = getLifecycleHandler(ep, false);
            if (lifecycleHandler != null)
               lifecycleHandler.stop(ep);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Value<T> getMSCService(final ServiceName serviceName, final Class<T> clazz) {
        return (ServiceController<T>)WSServices.getContainerRegistry().getService(serviceName);
    }
}
