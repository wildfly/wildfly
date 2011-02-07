/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import javax.management.MBeanServer;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.framework.management.DefaultEndpointRegistry;
import org.jboss.wsf.framework.management.ManagedEndpointRegistry;
import org.jboss.wsf.spi.management.EndpointRegistry;

/**
 * The service for the endpoint registry
 *
 * @author alessio.soldano@jboss.com
 * @since 10-Nov-2010
 *
 */
public final class EndpointRegistryService implements Service<EndpointRegistry> {
    private static final Logger log = Logger.getLogger(EndpointRegistryService.class);

    private EndpointRegistry registry;
    private InjectedValue<MBeanServer> mbeanServer;

    public EndpointRegistryService() {
        super();
    }

    public EndpointRegistryService(InjectedValue<MBeanServer> mbeanServer) {
        super();
        this.mbeanServer = mbeanServer;
    }

    @Override
    public EndpointRegistry getValue() throws IllegalStateException {
        return registry;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting EndpointRegistryService");
        if (mbeanServer != null) {
            registry = new ManagedEndpointRegistry();
            ((ManagedEndpointRegistry) registry).setMbeanServer(mbeanServer.getValue());
        } else {
            registry = new DefaultEndpointRegistry();
        }
    }

    @Override
    public void stop(StopContext context) {
        log.debugf("Stopping EndpointRegistryService");
    }

}
