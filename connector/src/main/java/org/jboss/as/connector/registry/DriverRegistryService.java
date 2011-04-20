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

package org.jboss.as.connector.registry;

import org.jboss.as.connector.ConnectorServices;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The JDBC driver registry service
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public final class DriverRegistryService implements Service<DriverRegistry> {

    private static final Logger log = Logger.getLogger("org.jboss.as.deployment.connector.registry");

    private DriverRegistry value;

    /**
     * Create an instance
     */
    public DriverRegistryService() {
        this.value = new DriverRegistryImpl();
    }

    @Override
    public DriverRegistry getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting sevice %s", ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE);
    }

    @Override
    public void stop(StopContext context) {
        log.debugf("Stopping sevice %s", ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE);
    }
}
