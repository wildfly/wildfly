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
import org.jboss.as.server.deployment.DelegatingSupplier;

/**
 * A simple EE-style namespace context selector which uses injected services for the contexts.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class InjectedEENamespaceContextSelector extends NamespaceContextSelector {

    private static final CompositeName EMPTY_NAME = new CompositeName();
    private final DelegatingSupplier<NamingStore> jbossContext = new DelegatingSupplier<>();
    private final DelegatingSupplier<NamingStore> globalContext = new DelegatingSupplier<>();
    private final DelegatingSupplier<NamingStore> appContext = new DelegatingSupplier<>();
    private final DelegatingSupplier<NamingStore> moduleContext = new DelegatingSupplier<>();
    private final DelegatingSupplier<NamingStore> compContext = new DelegatingSupplier<>();
    private final DelegatingSupplier<NamingStore> exportedContext = new DelegatingSupplier<>();

    public InjectedEENamespaceContextSelector() {
    }

    public DelegatingSupplier<NamingStore> getAppContextSupplier() {
        return appContext;
    }

    public DelegatingSupplier<NamingStore> getModuleContextSupplier() {
        return moduleContext;
    }

    public DelegatingSupplier<NamingStore> getCompContextSupplier() {
        return compContext;
    }

    public DelegatingSupplier<NamingStore> getJbossContextSupplier() {
        return jbossContext;
    }

    public DelegatingSupplier<NamingStore> getGlobalContextSupplier() {
        return globalContext;
    }

    public DelegatingSupplier<NamingStore> getExportedContextSupplier() {
        return exportedContext;
    }

    private NamingStore getNamingStore(final String identifier) {
        if (identifier.equals("jboss")) {
            return jbossContext.get();
        } else if (identifier.equals("global")) {
            return globalContext.get();
        } else if (identifier.equals("app")) {
            return appContext.get();
        } else if (identifier.equals("module")) {
            return moduleContext.get();
        } else if (identifier.equals("comp")) {
            return compContext.get();
        } else if (identifier.equals("jboss/exported")) {
            return exportedContext.get();
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
