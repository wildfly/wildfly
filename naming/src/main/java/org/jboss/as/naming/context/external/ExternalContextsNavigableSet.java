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
package org.jboss.as.naming.context.external;

import org.jboss.msc.service.ServiceName;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * An {@link ExternalContexts} implementation using a {@link java.util.NavigableSet} to store the service names of the existent external contexts.
 * @author Eduardo Martins
 */
public class ExternalContextsNavigableSet implements ExternalContexts {

    /**
     *
     */
    private final ConcurrentSkipListSet<ServiceName> externalContexts;

    /**
     *
     */
    public ExternalContextsNavigableSet() {
        externalContexts = new ConcurrentSkipListSet<>();
    }

    @Override
    public void addExternalContext(ServiceName serviceName) {
        externalContexts.add(serviceName);
    }

    @Override
    public boolean removeExternalContext(ServiceName serviceName) {
        return externalContexts.remove(serviceName);
    }

    @Override
    public ServiceName getParentExternalContext(ServiceName serviceName) {
        final ServiceName lower = externalContexts.lower(serviceName);
        return lower != null && lower.isParentOf(serviceName) ? lower : null;
    }
}
