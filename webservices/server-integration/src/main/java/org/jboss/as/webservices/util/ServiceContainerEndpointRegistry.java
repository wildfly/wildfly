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
package org.jboss.as.webservices.util;

import java.security.AccessController;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.ws.common.ObjectNameFactory;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.EndpointRegistry;
import org.jboss.wsf.spi.management.EndpointResolver;
/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 *
 */
public class ServiceContainerEndpointRegistry implements EndpointRegistry {
    final String endpointPrefix = WSServices.ENDPOINT_SERVICE.getCanonicalName() + ".context";

    @Override
    public Set<ObjectName> getEndpoints() {
        Set<ObjectName> endpoints = new CopyOnWriteArraySet<ObjectName>();
        for (ServiceName sname : currentServiceContainer().getServiceNames()) {
            if (sname.getCanonicalName().startsWith(endpointPrefix)) {
                String contextPath = sname.getParent().getSimpleName().substring(8);
                String endpointName = sname.getSimpleName();
                final StringBuilder name = new StringBuilder(Endpoint.SEPID_DOMAIN + ":");
                name.append(Endpoint.SEPID_PROPERTY_CONTEXT + "=").append(contextPath).append(",");
                name.append(Endpoint.SEPID_PROPERTY_ENDPOINT + "=").append(endpointName);
                endpoints.add(ObjectNameFactory.create(name.toString()));
            }
        }
        return endpoints;
    }

    @Override
    public Endpoint getEndpoint(ObjectName epName) {
        String context = epName.getKeyProperty(Endpoint.SEPID_PROPERTY_CONTEXT);
        String name = epName.getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
        ServiceName epSerivceName = WSServices.ENDPOINT_SERVICE.append("context=" + context).append(name);
        return ASHelper.getMSCService(epSerivceName, Endpoint.class);

    }

    @Override
    public Endpoint resolve(EndpointResolver resolver) {
        return resolver.query(getRegisteredEndpoints().iterator());
    }

    @Override
    public boolean isRegistered(ObjectName epName) {
        if (getEndpoint(epName) != null) {
            return true;
        }
        return false;
    }

    public void register(Endpoint endpoint) {
        // TODO:Remove this interface

    }

    public void unregister(Endpoint endpoint) {
        // TODO:Remove this interface
    }

    private Set<Endpoint> getRegisteredEndpoints() {
        Set<Endpoint> endpoints = new CopyOnWriteArraySet<Endpoint>();
        for (ServiceName sname : currentServiceContainer().getServiceNames()) {
            if (sname.getCanonicalName().startsWith(endpointPrefix)) {
                Endpoint ep = ASHelper.getMSCService(sname, Endpoint.class);
                if (ep != null) { //JBWS-3719
                    endpoints.add(ep);
                }
            }
        }
        return endpoints;
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }

}
