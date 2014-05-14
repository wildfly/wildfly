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
package org.jboss.as.naming;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;

import org.jboss.as.naming.context.ModularReference;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StabilityMonitor;

/**
 * Abstract object factory that allows for the creation of service references. Object factories that subclass
 * {@link ServiceReferenceObjectFactory} can get access to the value of the service described by the reference.
 * <p/>
 * If the factory state is no {@link State#UP} then the factory will block. If the state is {@link State#START_FAILED} or
 * {@link State#REMOVED} (or the state transactions to one of these states while blocking) an exception is thrown.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ServiceReferenceObjectFactory implements ServiceAwareObjectFactory {

    private volatile ServiceRegistry serviceRegistry;

    /**
     * Create a reference to a sub class of {@link ServiceReferenceObjectFactory} that injects the value of the given service.
     */
    public static Reference createReference(final ServiceName service, Class<? extends ServiceReferenceObjectFactory> factory) {
        return ModularReference.create(Context.class, new ServiceNameRefAdr("srof", service), factory);
    }

    @Override
    public void injectServiceRegistry(ServiceRegistry registry) {
        this.serviceRegistry = registry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        final Reference reference = (Reference) obj;
        final ServiceNameRefAdr nameAdr = (ServiceNameRefAdr) reference.get("srof");
        if (nameAdr == null) {
            throw NamingLogger.ROOT_LOGGER.invalidContextReference("srof");
        }
        final ServiceName serviceName = (ServiceName)nameAdr.getContent();
        final ServiceController<?> controller;
        try {
            controller = serviceRegistry.getRequiredService(serviceName);
        } catch (ServiceNotFoundException e) {
            throw NamingLogger.ROOT_LOGGER.cannotResolveService(serviceName);
        }

        final StabilityMonitor monitor = new StabilityMonitor();
        monitor.addController(controller);
        try {
            monitor.awaitStability();
        } catch (InterruptedException e) {
            throw NamingLogger.ROOT_LOGGER.threadInterrupt(serviceName);
        } finally {
            monitor.removeController(controller);
        }
        switch (controller.getState()) {
            case UP:
                return getObjectInstance(controller.getValue(), obj, name, nameCtx, environment);
            case START_FAILED:
                throw NamingLogger.ROOT_LOGGER.cannotResolveService(serviceName, getClass().getName(), "START_FAILED");
            case REMOVED:
                throw NamingLogger.ROOT_LOGGER.cannotResolveService(serviceName, getClass().getName(), "START_FAILED");
        }
        // we should never get here, as the listener should not notify unless the state was one of the above
        throw NamingLogger.ROOT_LOGGER.cannotResolveServiceBug(serviceName, getClass().getName(), controller.getState().toString());
    }

    /**
     * Handles the service reference. The parameters are the same as
     * {@link javax.naming.spi.ObjectFactory#getObjectInstance(Object, Name, Context, Hashtable)}, but with the addition of the service value as
     * the first parameter.
     */
    public Object getObjectInstance(Object serviceValue, Object obj, Name name, Context nameCtx,
                                             Hashtable<?, ?> environment) throws Exception {
        return serviceValue;
    }

    private static final class ServiceNameRefAdr extends RefAddr {
        private static final long serialVersionUID = 3677121114687908679L;

        private final ServiceName serviceName;

        private ServiceNameRefAdr(String s, ServiceName serviceName) {
            super(s);
            this.serviceName = serviceName;
        }

        public Object getContent() {
            return serviceName;
        }
    }
}
