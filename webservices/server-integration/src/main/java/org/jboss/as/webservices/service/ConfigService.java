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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.AbstractCommonConfig;
import org.jboss.wsf.spi.metadata.config.ClientConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;

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
        if (client) {
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.setConfigName(configName);
            serverConfig.addClientConfig(clientConfig);
            config = clientConfig;
        } else {
            EndpointConfig endpointConfig = new EndpointConfig();
            endpointConfig.setConfigName(configName);
            serverConfig.addEndpointConfig(endpointConfig);
            config = endpointConfig;
        }
    }

    @Override
    public void stop(final StopContext context) {
        if (client) {
            serverConfig.getClientConfigs().remove(config);
        } else {
            serverConfig.getEndpointConfigs().remove(config);
        }
    }
}
