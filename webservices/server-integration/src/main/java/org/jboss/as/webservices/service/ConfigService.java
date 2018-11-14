/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.webservices.config.ServerConfigFactoryImpl;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.metadata.config.AbstractCommonConfig;
import org.jboss.wsf.spi.metadata.config.ClientConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * A service for setting a ws client / endpoint config.
 *
 * @author paul.robinson@jboss.com
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ConfigService implements Service {

    private final String configName;
    private final boolean client;
    private volatile AbstractCommonConfig config;
    private final Consumer<AbstractCommonConfig> configConsumer;
    private final List<Supplier<UnifiedHandlerChainMetaData>> preHandlerChainSuppliers;
    private final List<Supplier<UnifiedHandlerChainMetaData>> postHandlerChainSuppliers;
    private final List<Supplier<PropertyService>> propertySuppliers;

    public ConfigService(final String configName, final boolean client,
                         final Consumer<AbstractCommonConfig> configConsumer,
                         final List<Supplier<PropertyService>> propertySuppliers,
                         final List<Supplier<UnifiedHandlerChainMetaData>> preHandlerChainSuppliers,
                         final List<Supplier<UnifiedHandlerChainMetaData>> postHandlerChainSuppliers
    ) {
        this.configName = configName;
        this.client = client;
        this.configConsumer = configConsumer;
        this.propertySuppliers = propertySuppliers;
        this.preHandlerChainSuppliers = preHandlerChainSuppliers;
        this.postHandlerChainSuppliers = postHandlerChainSuppliers;
    }

    @Override
    public void start(final StartContext context) {
        Map<String, String> props = null;
        if (!propertySuppliers.isEmpty()) {
            props = new HashMap<>(propertySuppliers.size(), 1);
            for (final Supplier<PropertyService> propertySupplier : propertySuppliers) {
                props.put(propertySupplier.get().getPropName(), propertySupplier.get().getPropValue());
            }
        }
        List<UnifiedHandlerChainMetaData> preHandlerChains = new ArrayList<>();
        for (final Supplier<UnifiedHandlerChainMetaData> preHandlerChainSupplier : preHandlerChainSuppliers) {
            preHandlerChains.add(preHandlerChainSupplier.get());
        }
        List<UnifiedHandlerChainMetaData> postHandlerChains = new ArrayList<>();
        for (final Supplier<UnifiedHandlerChainMetaData> postHandlerChainSupplier : postHandlerChainSuppliers) {
            postHandlerChains.add(postHandlerChainSupplier.get());
        }
        if (client) {
            ClientConfig clientConfig = new ClientConfig(configName, preHandlerChains, postHandlerChains, props, null);
            ServerConfigFactoryImpl.getConfig().registerClientConfig(clientConfig);
            configConsumer.accept(config = clientConfig);
        } else {
            EndpointConfig endpointConfig = new EndpointConfig(configName, preHandlerChains, postHandlerChains, props, null);
            ServerConfigFactoryImpl.getConfig().registerEndpointConfig(endpointConfig);
            configConsumer.accept(config = endpointConfig);
        }
    }

    @Override
    public void stop(final StopContext context) {
        configConsumer.accept(null);
        if (client) {
            ServerConfigFactoryImpl.getConfig().unregisterClientConfig((ClientConfig)config);
        } else {
            ServerConfigFactoryImpl.getConfig().unregisterEndpointConfig((EndpointConfig)config);
        }
        config = null;
    }
}
