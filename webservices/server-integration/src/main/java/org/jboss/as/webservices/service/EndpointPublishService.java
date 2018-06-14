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

import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.host.WebHost;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.publish.EndpointPublisherHelper;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;
import org.jboss.wsf.spi.publish.Context;

/**
 * WS endpoint publish service, allows for publishing a WS endpoint on AS 7
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EndpointPublishService implements Service<Context> {

    private final ServiceName name;
    private volatile Context wsctx;
    private final DeploymentUnit deploymentUnit;

    private final InjectedValue<WebHost> hostInjector = new InjectedValue<WebHost>();

    private EndpointPublishService(final String context, final DeploymentUnit deploymentUnit) {
        this.name = WSServices.ENDPOINT_PUBLISH_SERVICE.append(context);
        this.deploymentUnit = deploymentUnit;
    }

    @Override
    public Context getValue() {
        return wsctx;
    }

    public ServiceName getName() {
        return name;
    }

    public InjectedValue<WebHost> getHostInjector() {
        return hostInjector;
    }

    @Override
    public void start(final StartContext ctx) throws StartException {
        WSLogger.ROOT_LOGGER.starting(name);
        try {
            wsctx = EndpointPublisherHelper.doPublishStep(hostInjector.getValue(), ctx.getChildTarget(), deploymentUnit);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(final StopContext ctx) {
        WSLogger.ROOT_LOGGER.stopping(name);
        List<Endpoint> eps = wsctx.getEndpoints();
        if (eps == null || eps.isEmpty()) {
            return;
        }
        try {
            EndpointPublisherHelper.undoPublishStep(hostInjector.getValue(), wsctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ServiceBuilder<Context> createServiceBuilder(final ServiceTarget serviceTarget, final String context,
            final ClassLoader loader, final String hostName, final Map<String,String> urlPatternToClassName) {
        return createServiceBuilder(serviceTarget, context, loader, hostName, urlPatternToClassName, null, null, null);
    }

    public static ServiceBuilder<Context> createServiceBuilder(final ServiceTarget serviceTarget, final String context,
            final ClassLoader loader, final String hostName, final Map<String, String> urlPatternToClassName,
            JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd) {
        return createServiceBuilder(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd, jbwsmd, null);
    }

    public static ServiceBuilder<Context> createServiceBuilder(final ServiceTarget serviceTarget, final String context,
            final ClassLoader loader, final String hostName, final Map<String, String> urlPatternToClassName,
            JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd, Map<Class<?>, Object> deploymentAttachments) {
        final DeploymentUnit unit = EndpointDeployService.install(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd, jbwsmd, deploymentAttachments);
        final EndpointPublishService service = new EndpointPublishService(context, unit);
        final ServiceBuilder<Context> builder = serviceTarget.addService(service.getName(), service);
        builder.addDependency(WSServices.CONFIG_SERVICE);
        builder.addDependency(WebHost.SERVICE_NAME.append(hostName), WebHost.class, service.getHostInjector());
        for (ServiceName epServiceName : EndpointService.getServiceNamesFromDeploymentUnit(unit)) {
            builder.addDependency(epServiceName);
        }
        return builder;
    }

    public static void install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String,String> urlPatternToClassName) {
        install(serviceTarget, context, loader, hostName, urlPatternToClassName, null, null, null);
    }

    public static void install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String,String> urlPatternToClassName, JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd) {
        ServiceBuilder<Context> builder = createServiceBuilder(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd, jbwsmd);
        builder.setInitialMode(Mode.ACTIVE);
        builder.install();
    }

}
