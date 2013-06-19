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
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.mod_cluster.ContainerEventHandlerAdapterBuilder;
import org.wildfly.extension.mod_cluster.ContainerEventHandlerService;

public class CatalinaEventHandlerAdapterBuilder implements ContainerEventHandlerAdapterBuilder {
    public static final ServiceName SERVICE_NAME = ContainerEventHandlerService.SERVICE_NAME.append("web");

    @Override
    public ServiceBuilder<?> build(ServiceTarget target, String connectorName) {
        InjectedValue<ContainerEventHandler> eventHandler = new InjectedValue<>();
        InjectedValue<WebServer> webServer = new InjectedValue<>();
        InjectedValue<Connector> connector = new InjectedValue<>();
        return target.addService(SERVICE_NAME, new CatalinaEventHandlerAdapterService(eventHandler, webServer, connector))
                .addDependency(ContainerEventHandlerService.SERVICE_NAME, ContainerEventHandler.class, eventHandler)
                .addDependency(WebSubsystemServices.JBOSS_WEB_CONNECTOR.append(connectorName), Connector.class, connector)
                .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, webServer)
        ;
    }
}
