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

package org.wildfly.mod_cluster.web;

import org.apache.catalina.connector.Connector;
import org.jboss.as.web.WebServer;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.container.catalina.CatalinaFactory;
import org.jboss.modcluster.container.catalina.ProxyConnectorProvider;
import org.jboss.modcluster.container.catalina.ServerProvider;
import org.jboss.modcluster.container.catalina.ServiceLoaderCatalinaFactory;
import org.jboss.modcluster.container.catalina.SimpleProxyConnectorProvider;
import org.jboss.modcluster.container.catalina.SimpleServerProvider;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

public class CatalinaEventHandlerAdapterService implements Service<CatalinaEventHandlerAdapter> {

    private final Value<Connector> connector;
    private final Value<WebServer> webServer;
    private final Value<ContainerEventHandler> eventHandler;
    private volatile CatalinaEventHandlerAdapter adapter;

    public CatalinaEventHandlerAdapterService(Value<ContainerEventHandler> eventHandler, Value<WebServer> webServer, Value<Connector> connector) {
        this.eventHandler = eventHandler;
        this.webServer = webServer;
        this.connector = connector;
    }

    @Override
    public CatalinaEventHandlerAdapter getValue() {
        return this.adapter;
    }

    @Override
    public void start(StartContext context) throws StartException {
        WebServer webServer = this.webServer.getValue();
        ServerProvider serverProvider = new SimpleServerProvider(webServer.getServer());
        ProxyConnectorProvider connectorProvider = new SimpleProxyConnectorProvider(this.connector.getValue());
        CatalinaFactory factory = new ServiceLoaderCatalinaFactory(connectorProvider);

        this.adapter = new CatalinaEventHandlerAdapter(this.eventHandler.getValue(), serverProvider, factory);
        this.adapter.start();
    }

    @Override
    public void stop(StopContext context) {
        this.adapter.stop();
    }
}
