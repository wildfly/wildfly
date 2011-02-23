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
package org.jboss.as.ee.naming;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A {@link NamespaceContextSelector} that can resolve the comp, module and app contexts.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class NamespaceSelectorService implements Service<NamespaceContextSelector> {

    public static final ServiceName NAME = ServiceName.of("namespaceselector");

    private final InjectedValue<NamingStore> comp = new InjectedValue<NamingStore>();
    private final InjectedValue<NamingStore> module = new InjectedValue<NamingStore>();
    private final InjectedValue<NamingStore> app = new InjectedValue<NamingStore>();

    private volatile SimpleEENamespaceContextSelector selector;

    @Override
    public void start(StartContext context) throws StartException {
        NamingStore app = this.app.getOptionalValue();
        NamingStore module = this.module.getOptionalValue();
        NamingStore comp = this.comp.getOptionalValue();
        try {
            selector = new SimpleEENamespaceContextSelector(
                    app != null ? (Context) app.lookup(new CompositeName()) : null,
                    module != null ? (Context) module.lookup(new CompositeName()) : null,
                    comp != null ? (Context) module.lookup(new CompositeName()) : null);
        } catch (NamingException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        selector = null;
    }

    @Override
    public NamespaceContextSelector getValue() throws IllegalStateException, IllegalArgumentException {
        return selector;
    }

    public Injector<NamingStore> getComp() {
        return comp;
    }

    public Injector<NamingStore> getModule() {
        return module;
    }

    public Injector<NamingStore> getApp() {
        return app;
    }
}
