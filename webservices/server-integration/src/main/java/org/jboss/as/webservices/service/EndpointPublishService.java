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

import java.util.Map;

import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.as.webservices.publish.EndpointPublisherImpl;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
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
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;
import org.jboss.wsf.spi.publish.Context;

/**
 * WS endpoint publish service, allows for publishing a WS endpoint on AS 7
 *
 * @author alessio.soldano@jboss.com
 * @since 12-Jul-2011
 */
public final class EndpointPublishService implements Service<Context> {

    private final ServiceName name;
    private volatile Context wsctx;

    private final ClassLoader loader;
    private final String context;
    private final Map<String,String> urlPatternToClassName;
    private final JBossWebMetaData jbwmd;
    private final WebservicesMetaData wsmd;

    private final InjectedValue<VirtualHost> hostInjector = new InjectedValue<VirtualHost>();

    private EndpointPublishService(final String context, final ClassLoader loader,
            final Map<String,String> urlPatternToClassName, JBossWebMetaData jbwmd, WebservicesMetaData wsmd) {
        this.name = WSServices.ENDPOINT_PUBLISH_SERVICE.append(context);
        this.loader = loader;
        this.context = context;
        this.urlPatternToClassName = urlPatternToClassName;
        this.jbwmd = jbwmd;
        this.wsmd = wsmd;
    }

    @Override
    public Context getValue() {
        return wsctx;
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
        try {
            EndpointPublisherImpl publisher = new EndpointPublisherImpl(hostInjector.getValue().getHost());
            wsctx = publisher.publish(ctx.getChildTarget(), context, loader, urlPatternToClassName, jbwmd, wsmd);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(final StopContext ctx) {
        ROOT_LOGGER.stopping(name);
        try {
            EndpointPublisherImpl publisher = new EndpointPublisherImpl(hostInjector.getValue().getHost());
            publisher.destroy(wsctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ServiceBuilder<Context> createServiceBuilder(final ServiceTarget serviceTarget, final String context,
            final ClassLoader loader, final String hostName, final Map<String,String> urlPatternToClassName) {
        return createServiceBuilder(serviceTarget, context, loader, hostName, urlPatternToClassName, null, null);
    }

    public static ServiceBuilder<Context> createServiceBuilder(final ServiceTarget serviceTarget, final String context,
            final ClassLoader loader, final String hostName, final Map<String, String> urlPatternToClassName,
            JBossWebMetaData jbwmd, WebservicesMetaData wsmd) {
        final EndpointPublishService service = new EndpointPublishService(context, loader, urlPatternToClassName, jbwmd, wsmd);
        final ServiceBuilder<Context> builder = serviceTarget.addService(service.getName(), service);
        builder.addDependency(DependencyType.REQUIRED, WSServices.CONFIG_SERVICE);
        builder.addDependency(DependencyType.REQUIRED, WSServices.REGISTRY_SERVICE);
        builder.addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName), VirtualHost.class,
                service.getHostInjector());
        return builder;
    }

    public static void install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String,String> urlPatternToClassName) {
        install(serviceTarget, context, loader, hostName, urlPatternToClassName, null, null);
    }

    public static void install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String,String> urlPatternToClassName, JBossWebMetaData jbwmd, WebservicesMetaData wsmd) {
        ServiceBuilder<Context> builder = createServiceBuilder(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd);
        builder.setInitialMode(Mode.ACTIVE);
        builder.install();
    }

}
