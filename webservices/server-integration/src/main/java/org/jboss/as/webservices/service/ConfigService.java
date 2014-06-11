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

import org.jboss.as.webservices.dmr.ListInjector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.AbstractCommonConfig;
import org.jboss.wsf.spi.metadata.config.ClientConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * A service for setting a ws client / endpoint config.
 *
 * @author paul.robinson@jboss.com
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class ConfigService implements Service<AbstractCommonConfig> {

    private final ServerConfig serverConfig;
    private final String configName;
    private final boolean client;
    private final List<UnifiedHandlerChainMetaData> preHandlerChains = new ArrayList<UnifiedHandlerChainMetaData>(1);
    private final List<UnifiedHandlerChainMetaData> postHandlerChains = new ArrayList<UnifiedHandlerChainMetaData>(1);
    private final List<PropertyService> properties = new ArrayList<PropertyService>(1);
    private volatile AbstractCommonConfig config;

    public ConfigService(ServerConfig serverConfig, String configName, boolean client) {
        this.configName = configName;
        this.client = client;
        this.serverConfig = serverConfig;
    }

    @Override
    public AbstractCommonConfig getValue() {
        return config;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        Map<String, String> props = null;
        if (!properties.isEmpty()) {
            props = new HashMap<String, String>(properties.size(), 1);
            for (PropertyService ps : properties) {
                props.put(ps.getPropName(), ps.getPropValue());
            }
        }
        if (client) {
            ClientConfig clientConfig = new ClientConfig(configName, preHandlerChains, postHandlerChains, props, null);
            serverConfig.registerClientConfig(clientConfig);
            config = clientConfig;
        } else {
            EndpointConfig endpointConfig = new EndpointConfig(configName, preHandlerChains, postHandlerChains, props, null);
            serverConfig.registerEndpointConfig(endpointConfig);
            config = endpointConfig;
        }
    }

    @Override
    public void stop(final StopContext context) {
        if (client) {
            serverConfig.unregisterClientConfig((ClientConfig)config);
        } else {
            serverConfig.unregisterEndpointConfig((EndpointConfig)config);
        }
    }

    public Injector<UnifiedHandlerChainMetaData> getPreHandlerChainsInjector() {
        return new ListInjector<UnifiedHandlerChainMetaData>(preHandlerChains);
    }

    public Injector<UnifiedHandlerChainMetaData> getPostHandlerChainsInjector() {
        return new ListInjector<UnifiedHandlerChainMetaData>(postHandlerChains);
    }

    public Injector<PropertyService> getPropertiesInjector() {
        return new ListInjector<PropertyService>(properties);
    }

}
