/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        try {
            final ServiceController<?> controller = serviceRegistry.getRequiredService(serviceName);
            return getObjectInstance(controller.awaitValue(), obj, name, nameCtx, environment);
        } catch (ServiceNotFoundException e) {
            throw NamingLogger.ROOT_LOGGER.cannotResolveService(serviceName);
        } catch (InterruptedException e) {
            throw NamingLogger.ROOT_LOGGER.threadInterrupt(serviceName);
        } catch (Throwable t) {
            throw NamingLogger.ROOT_LOGGER.cannotResolveService(serviceName, getClass().getName(), "START_FAILED");
        }
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
