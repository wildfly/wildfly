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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.host.WebHost;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.publish.EndpointPublisherHelper;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
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
public final class EndpointPublishService implements Service {

    private final ServiceName name;
    private volatile Context wsctx;
    private final DeploymentUnit deploymentUnit;
    private final Consumer<Context> wsctxConsumer;
    private final Supplier<WebHost> hostSupplier;

    private EndpointPublishService(final ServiceName name, final DeploymentUnit deploymentUnit,
                                   final Consumer<Context> wsctxConsumer, final Supplier<WebHost> hostSupplier) {
        this.name = name;
        this.deploymentUnit = deploymentUnit;
        this.wsctxConsumer = wsctxConsumer;
        this.hostSupplier = hostSupplier;
    }

    @Override
    public void start(final StartContext ctx) throws StartException {
        WSLogger.ROOT_LOGGER.starting(name);
        try {
            wsctxConsumer.accept(wsctx = EndpointPublisherHelper.doPublishStep(hostSupplier.get(), ctx.getChildTarget(), deploymentUnit));
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(final StopContext ctx) {
        WSLogger.ROOT_LOGGER.stopping(name);
        wsctxConsumer.accept(null);
        List<Endpoint> eps = wsctx.getEndpoints();
        if (eps == null || eps.isEmpty()) {
            return;
        }
        try {
            EndpointPublisherHelper.undoPublishStep(hostSupplier.get(), wsctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static ServiceBuilder createServiceBuilder(final ServiceTarget serviceTarget, final String context,
            final ClassLoader loader, final String hostName, final Map<String, String> urlPatternToClassName,
            JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd) {
        return createServiceBuilder(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd, jbwsmd, null, null);
    }

    public static ServiceBuilder createServiceBuilder(final ServiceTarget serviceTarget, final String context,
            final ClassLoader loader, final String hostName, final Map<String, String> urlPatternToClassName,
            JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd, Map<Class<?>, Object> deploymentAttachments,
            CapabilityServiceSupport capabilityServiceSupport) {
        final DeploymentUnit unit = EndpointDeployService.install(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd,
                                                                  jbwsmd, deploymentAttachments, capabilityServiceSupport);
        final ServiceName serviceName = WSServices.ENDPOINT_PUBLISH_SERVICE.append(context);
        final ServiceBuilder<?> builder = serviceTarget.addService(serviceName);
        builder.requires(WSServices.CONFIG_SERVICE);
        for (ServiceName epServiceName : EndpointService.getServiceNamesFromDeploymentUnit(unit)) {
            builder.requires(epServiceName);
        }
        final Consumer<Context> contextConsumer = builder.provides(serviceName);
        final Supplier<WebHost> hostSupplier = builder.requires(WebHost.SERVICE_NAME.append(hostName));
        builder.setInstance(new EndpointPublishService(serviceName, unit, contextConsumer, hostSupplier));
        return builder;
    }

    public static void install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String,String> urlPatternToClassName) {
        install(serviceTarget, context, loader, hostName, urlPatternToClassName, null, null, null);
    }

    public static void install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String,String> urlPatternToClassName, JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd) {
        ServiceBuilder builder = createServiceBuilder(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd, jbwsmd);
        builder.install();
    }

}
