/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.management.MBeanServer;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.management.ServerConfig;
import org.wildfly.extension.undertow.UndertowService;

/**
 * WS server config service.
 *
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public final class ServerConfigService implements Service {

    private static final ServiceName MBEAN_SERVER_NAME = ServiceName.JBOSS.append("mbean", "server");
    private final ServerConfigImpl serverConfig;
    private final Consumer<ServerConfig> serverConfigConsumer;
    private final Supplier<MBeanServer> mBeanServerSupplier;
    private final Supplier<UndertowService> undertowServiceSupplier;
    private final Supplier<ServerEnvironment> serverEnvironmentSupplier;

    private ServerConfigService(final ServerConfigImpl serverConfig,
                                final Consumer<ServerConfig> serverConfigConsumer,
                                final Supplier<MBeanServer> mBeanServerSupplier,
                                final Supplier<UndertowService> undertowServiceSupplier,
                                final Supplier<ServerEnvironment> serverEnvironmentSupplier) {
        this.serverConfig = serverConfig;
        this.serverConfigConsumer = serverConfigConsumer;
        this.mBeanServerSupplier = mBeanServerSupplier;
        this.undertowServiceSupplier = undertowServiceSupplier;
        this.serverEnvironmentSupplier = serverEnvironmentSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        try {
            if (mBeanServerSupplier != null) serverConfig.setMbeanServer(mBeanServerSupplier.get());
            if (undertowServiceSupplier != null) serverConfig.setUndertowService(undertowServiceSupplier.get());
            serverConfig.setServerEnvironment(serverEnvironmentSupplier.get());
            serverConfig.create();
            serverConfigConsumer.accept(serverConfig);
        } catch (final Exception e) {
            WSLogger.ROOT_LOGGER.configServiceCreationFailed();
            throw new StartException(e);
        }
    }

    @Override
    public void stop(final StopContext context) {
        try {
            serverConfig.destroy();
            if (mBeanServerSupplier != null) serverConfig.setMbeanServer(null);
            if (undertowServiceSupplier != null) serverConfig.setUndertowService(null);
        } catch (final Exception e) {
            WSLogger.ROOT_LOGGER.configServiceDestroyFailed();
        }
    }

    public static ServiceController<?> install(final ServiceTarget serviceTarget, final ServerConfigImpl serverConfig,
            final List<ServiceName> dependencies, final boolean jmxSubsystemAvailable, final boolean requireUndertow) {
        final ServiceBuilder<?> builder = serviceTarget.addService(WSServices.CONFIG_SERVICE);
        final Consumer<ServerConfig> serverConfigConsumer = builder.provides(WSServices.CONFIG_SERVICE);
        final Supplier<MBeanServer> mBeanServerSupplier = jmxSubsystemAvailable ? builder.requires(MBEAN_SERVER_NAME) : null;
        final Supplier<UndertowService> undertowServiceSupplier = requireUndertow ? builder.requires(UndertowService.UNDERTOW) : null;
        final Supplier<ServerEnvironment> serverEnvironmentSupplier = builder.requires(ServerEnvironmentService.SERVICE_NAME);
        for (final ServiceName dep : dependencies) {
            builder.requires(dep);
        }
        builder.setInstance(new ServerConfigService(serverConfig, serverConfigConsumer, mBeanServerSupplier, undertowServiceSupplier, serverEnvironmentSupplier));
        return builder.install();
    }

}
