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
import java.util.List;

import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.dmr.ListInjector;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.ClientConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * A service for triggering the XTS client config integration
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class XTSClientIntegrationService implements Service<Void> {

    private final InjectedValue<ServerConfig> serverConfig = new InjectedValue<ServerConfig>();
    private final List<UnifiedHandlerChainMetaData> postHandlerChains = new ArrayList<UnifiedHandlerChainMetaData>(1);

    private XTSClientIntegrationService() {
        //NOOP
    }

    @Override
    public void start(final StartContext context) throws StartException {
        ClientConfig wrapper = new ClientConfig(null, null, postHandlerChains, null, null);
        ((ServerConfigImpl)(serverConfig.getValue())).setClientConfigWrapper(wrapper, true);
    }

    @Override
    public void stop(final StopContext context) {
        ((ServerConfigImpl)(serverConfig.getValue())).setClientConfigWrapper(null, false);
    }

    public Injector<UnifiedHandlerChainMetaData> getPostHandlerChainsInjector() {
        return new ListInjector<UnifiedHandlerChainMetaData>(postHandlerChains);
    }

    public Injector<ServerConfig> getServerConfigInjector() {
        return serverConfig;
    }

    public static ServiceController<?> install(final ServiceTarget serviceTarget) {
        final XTSClientIntegrationService service = new XTSClientIntegrationService();
        final ServiceBuilder<?> builder = serviceTarget.addService(WSServices.XTS_CLIENT_INTEGRATION_SERVICE, service);
        builder.addDependency(ServiceName.JBOSS.append("xts").append("handlers"), UnifiedHandlerChainMetaData.class, service.getPostHandlerChainsInjector());
        builder.addDependency(WSServices.CONFIG_SERVICE, ServerConfig.class, service.getServerConfigInjector());
        //set passive initial mode, as this has to start only *if* the XTS service above is actually installed and started
        return builder.setInitialMode(ServiceController.Mode.PASSIVE).install();
    }

    @Override
    public Void getValue() {
        return null;
    }

}
