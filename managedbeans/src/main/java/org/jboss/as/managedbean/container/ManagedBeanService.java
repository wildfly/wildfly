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

package org.jboss.as.managedbean.container;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for managing the life-cycle of a managed bean container.  Once this service is started it will
 * register the managed bean container with the registry and in essence mark the managed bean in service.  It will
 * un-register the container from the registry when stopped.
 *
 * @author John E. Bailey
 */
public class ManagedBeanService<T> implements Service<ManagedBeanContainer<T>> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("managed", "bean");
    private static final Logger log = Logger.getLogger("org.jboss.as.managedbean");

    private final ManagedBeanContainer<T> container;

    public ManagedBeanService(final ManagedBeanContainer<T> container) {
        this.container = container;
    }

    /** {@inheritDoc} */
    public synchronized  void start(StartContext context) throws StartException {
        try {
            final ServiceName serviceName = context.getController().getName();
            log.infof("Starting managed bean %s", serviceName);
            ManagedBeanRegistry.register(serviceName.toString(), container);
        } catch (ManagedBeanRegistry.DuplicateMangedBeanException e) {
            throw new StartException("Failed to register with the managed bean registry");
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final ServiceName serviceName = context.getController().getName();
        log.infof("Stopping managed bean %s", serviceName);
        ManagedBeanRegistry.unregister(context.getController().getName().toString(), container);
    }

    /**
     * Get the container for this managed bean.
     *
     * @return The managed bean container
     */
    public ManagedBeanContainer<T> getValue() throws IllegalStateException {
        return container;
    }
}
