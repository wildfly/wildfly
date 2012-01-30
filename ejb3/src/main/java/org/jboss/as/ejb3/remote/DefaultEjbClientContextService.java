/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.remote;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service that manages an EJBClientContext
 *
 * @author Stuart Douglas
 */
public class DefaultEjbClientContextService implements Service<EJBClientContext> {

    /**
     * The base service name for these services
     */
    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbClientContext");

    /**
     * The default service name. There will always be a service registered under this name
     */
    public static final ServiceName DEFAULT_SERVICE_NAME = BASE_SERVICE_NAME.append("default");

    /**
     * The recievers to add to the context
     */
    private final List<InjectedValue<EJBReceiver>> ejbReceivers = new ArrayList<InjectedValue<EJBReceiver>>();

    private final InjectedValue<TCCLBasedEJBClientContextSelector> tcclEJBClientContextSelector = new InjectedValue<TCCLBasedEJBClientContextSelector>();

    /**
     * The client context
     */
    private volatile EJBClientContext context;

    private ContextSelector<EJBClientContext> previousSelector;

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final EJBClientContext clientContext = EJBClientContext.create();
        for (final InjectedValue<EJBReceiver> receiver : ejbReceivers) {
            clientContext.registerEJBReceiver(receiver.getValue());
        }
        this.context = clientContext;
        // setup the client context selector
        // TODO: We set this up here, for now. But we need to rethink about how we are going to
        // handle manual overrides of EJB client context selector by user code on the server side.
        // Setting this up via interceptor isn't a good idea too since that will end up overriding
        // the selector which the user code might have set intentionally. So let this be here for now
        previousSelector = AccessController.doPrivileged(new SetSelectorAction(this.tcclEJBClientContextSelector.getValue()));
    }

    @Override
    public synchronized void stop(final StopContext context) {
        this.context = null;
        if (this.previousSelector != null) {
            AccessController.doPrivileged(new SetSelectorAction(previousSelector));
        }
    }

    @Override
    public EJBClientContext getValue() throws IllegalStateException, IllegalArgumentException {
        return context;
    }

    public void addReceiver(final ServiceBuilder<EJBClientContext> serviceBuilder, final ServiceName serviceName) {
        final InjectedValue<EJBReceiver> value = new InjectedValue<EJBReceiver>();
        serviceBuilder.addDependency(serviceName, EJBReceiver.class, value);
        ejbReceivers.add(value);
    }

    public void addReceiver(final InjectedValue<EJBReceiver> value) {
        ejbReceivers.add(value);
    }

    public Injector<TCCLBasedEJBClientContextSelector> getTCCLBasedEJBClientContextSelectorInjector() {
        return this.tcclEJBClientContextSelector;
    }

    private static final class SetSelectorAction implements PrivilegedAction<ContextSelector<EJBClientContext>> {

        private final ContextSelector<EJBClientContext> selector;

        private SetSelectorAction(final ContextSelector<EJBClientContext> selector) {
            this.selector = selector;
        }

        @Override
        public ContextSelector<EJBClientContext> run() {
            return EJBClientContext.setSelector(selector);
        }
    }
}
