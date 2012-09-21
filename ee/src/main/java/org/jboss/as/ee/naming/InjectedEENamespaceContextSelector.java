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

package org.jboss.as.ee.naming;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * A simple EE-style namespace context selector which uses injected services for the contexts.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class InjectedEENamespaceContextSelector extends NamespaceContextSelector {

    private static final CompositeName EMPTY_NAME = new CompositeName();
    private final InjectedValue<NamingStore> jbossContext = new InjectedValue<NamingStore>();
    private final InjectedValue<NamingStore> globalContext = new InjectedValue<NamingStore>();
    private final InjectedValue<NamingStore> appContext = new InjectedValue<NamingStore>();
    private final InjectedValue<NamingStore> moduleContext = new InjectedValue<NamingStore>();
    private final InjectedValue<NamingStore> compContext = new InjectedValue<NamingStore>();
    private final InjectedValue<NamingStore> exportedContext = new InjectedValue<NamingStore>();

    public InjectedEENamespaceContextSelector() {
    }

    public Injector<NamingStore> getAppContextInjector() {
        return appContext;
    }

    public Injector<NamingStore> getModuleContextInjector() {
        return moduleContext;
    }

    public Injector<NamingStore> getCompContextInjector() {
        return compContext;
    }

    public Injector<NamingStore> getJbossContextInjector() {
        return jbossContext;
    }

    public Injector<NamingStore> getGlobalContextInjector() {
        return globalContext;
    }

    public Injector<NamingStore> getExportedContextInjector() {
        return exportedContext;
    }

    private NamingStore getNamingStore(final String identifier) {
        if (identifier.equals("jboss")) {
            return jbossContext.getOptionalValue();
        } else if (identifier.equals("global")) {
            return globalContext.getOptionalValue();
        } else if (identifier.equals("app")) {
            return appContext.getOptionalValue();
        } else if (identifier.equals("module")) {
            return moduleContext.getOptionalValue();
        } else if (identifier.equals("comp")) {
            return compContext.getOptionalValue();
        } else if (identifier.equals("jboss/exported")) {
            return exportedContext.getOptionalValue();
        } else {
            return null;
        }
    }

    public Context getContext(final String identifier) {
        NamingStore namingStore = getNamingStore(identifier);
        if (namingStore != null) {
            try {
                return (Context) namingStore.lookup(EMPTY_NAME);
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
        } else {
            return null;
        }
    }
}
