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
package org.jboss.as.naming.deployment;

import java.util.Set;

import org.jboss.as.naming.NamingLogger;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.util.collection.ConcurrentSet;

/**
 * A {@link Service} which on stop releases runtime installed {@link BinderService}s.
 *
 * @author Eduardo Martins
 *
 */
public class RuntimeBindReleaseService implements Service<Set<ServiceName>> {

    private Set<ServiceName> serviceNames;

    @Override
    public Set<ServiceName> getValue() throws IllegalStateException, IllegalArgumentException {
        synchronized (this) {
            if (serviceNames == null) {
                serviceNames = new ConcurrentSet<ServiceName>();
            }
            return serviceNames;
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        // nothing to do
    }

    @Override
    public void stop(StopContext context) {
        synchronized (this) {
            if(serviceNames != null) {
            ServiceContainer container = context.getController().getServiceContainer();
                for (ServiceName serviceName : serviceNames) {
                    try {
                        ((BinderService) container.getService(serviceName).getService()).release();
                    } catch (Throwable e) {
                        NamingLogger.ROOT_LOGGER.failedToReleaseBinderService(e);
                    }
                }
            }
        }
    }
}
