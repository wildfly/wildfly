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

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.ClientConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * A service for triggering the XTS client config integration
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class XTSClientIntegrationService implements Service {

    private final Supplier<ServerConfig> serverConfig;
    private final Supplier<UnifiedHandlerChainMetaData> postHandlerChains;

    private XTSClientIntegrationService(final Supplier<ServerConfig> serverConfig, final Supplier<UnifiedHandlerChainMetaData> postHandlerChains) {
        this.serverConfig = serverConfig;
        this.postHandlerChains = postHandlerChains;
    }

    @Override
    public void start(final StartContext context) {
        final List<UnifiedHandlerChainMetaData> postHandlerChainsList = new ArrayList<>();
        postHandlerChainsList.add(postHandlerChains.get());
        ClientConfig wrapper = new ClientConfig(null, null, postHandlerChainsList, null, null);
        ((ServerConfigImpl)(serverConfig.get())).setClientConfigWrapper(wrapper, true);
    }

    @Override
    public void stop(final StopContext context) {
        ((ServerConfigImpl)(serverConfig.get())).setClientConfigWrapper(null, false);
    }

    public static ServiceController<?> install(final ServiceTarget serviceTarget) {
        final ServiceBuilder<?> builder = serviceTarget.addService(WSServices.XTS_CLIENT_INTEGRATION_SERVICE);
        final Supplier<ServerConfig> serverConfig = builder.requires(WSServices.CONFIG_SERVICE);
        final Supplier<UnifiedHandlerChainMetaData> postHandlerChains = builder.requires(ServiceName.JBOSS.append("xts").append("handlers"));
        builder.setInstance(new XTSClientIntegrationService(serverConfig, postHandlerChains));
        //set passive initial mode, as this has to start only *if* the XTS service above is actually installed and started
        return builder.setInitialMode(ServiceController.Mode.PASSIVE).install();
    }

}
