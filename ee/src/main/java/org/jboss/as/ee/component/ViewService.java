/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A view service which creates a view for a component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ViewService implements Service<ComponentView> {

    private final InjectedValue<AbstractComponent> componentInjector = new InjectedValue<AbstractComponent>();
    private final Class<?> viewClass;
    private final ProxyFactory<?> proxyFactory;
    private volatile ComponentView instance;

    /**
     * Construct a new instance.
     *
     * @param viewClass the view class for this view service
     * @param proxyFactory the proxy factory for the view class
     */
    public ViewService(final Class<?> viewClass, final ProxyFactory<?> proxyFactory) {
        this.viewClass = viewClass;
        this.proxyFactory = proxyFactory;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        final AbstractComponent component = componentInjector.getValue();
        instance = new ComponentView(component, viewClass, proxyFactory);
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        instance = null;
    }

    /** {@inheritDoc} */
    public ComponentView getValue() throws IllegalStateException, IllegalArgumentException {
        return instance;
    }

    /**
     * Get the component injector.
     *
     * @return the component injector
     */
    public Injector<AbstractComponent> getComponentInjector() {
        return componentInjector;
    }
}
