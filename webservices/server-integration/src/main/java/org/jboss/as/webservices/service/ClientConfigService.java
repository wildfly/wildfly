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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.ClientConfig;

import java.util.List;

/**
 * WS server config service.
 *
 * @author paul.robinson@jboss.com
 */
public final class ClientConfigService implements Service<List<ClientConfig>> {

    private InjectedValue<ServerConfig> serverConfig = new InjectedValue<ServerConfig>();

    private String configName;

    public ClientConfigService(String configName) {
        this.configName = configName;
    }

    @Override
    public List<ClientConfig> getValue() {
        return serverConfig.getValue().getClientConfigs();
    }

    @Override
    public void start(final StartContext context) throws StartException {

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setConfigName(configName);
        serverConfig.getValue().addClientConfig(clientConfig);
    }

    @Override
    public void stop(final StopContext context) {

        ClientConfig target = null;
        for (ClientConfig clConfig : serverConfig.getValue().getClientConfigs()) {
            if (clConfig.getConfigName().equals(configName)) {
                target = clConfig;
            }
        }
        if (target != null) {
            serverConfig.getValue().getClientConfigs().remove(target);
        }
    }

    public InjectedValue<ServerConfig> getServerConfig() {

        return serverConfig;
    }
}
