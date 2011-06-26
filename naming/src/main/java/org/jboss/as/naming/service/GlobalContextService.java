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

package org.jboss.as.naming.service;

import org.jboss.as.naming.InMemoryNamingStore;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.context.GlobalNamespaceObjectFactory;
import org.jboss.as.naming.util.NameParser;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;

/**
 * Service responsible for managing the creation and life-cycle of a global naming context.
 * <p>
 * Contexts created by this service use a separate in-memory store
 *
 * @author John E. Bailey
 * @author Stuart Douglas
 * @author Jason T. Greene
 */
public class GlobalContextService implements Service<NamingStore> {
    private InMemoryNamingStore store;
    private final String name;
    private final InjectedValue<NamingStore> javaContext = new InjectedValue<NamingStore>();

    public GlobalContextService(String name) {
        this.name = name;
    }

    /**
     * Creates a sub-context in the parent context with the provided name.
     *
     * @param context The start context
     * @throws StartException If any problems occur creating the context
     */
    public synchronized void start(final StartContext context) throws StartException {
        store = new InMemoryNamingStore();

        try {
            final Reference globalReference = GlobalNamespaceObjectFactory.createReference(name, (Context) store.lookup(new CompositeName()));
            final NamingStore javaContext =  this.javaContext.getValue();
            javaContext.rebind(NameParser.INSTANCE.parse(name), globalReference, Reference.class);
        } catch (NamingException e) {
            throw new StartException("Failed to bind EE context: java:" + name, e);
        }
    }

    /**
     * Unbinds the context from the parent.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        final NamingStore javaContext = this.javaContext.getValue();
        try {
            javaContext.unbind(NameParser.INSTANCE.parse(name));
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to unbind EE context: java:" + name, e);
        } finally {
            try {
                store.close();
                store = null;
            } catch (NamingException e) {
                throw new IllegalStateException("Failed to destroy root context", e);
            }
        }
    }

    /**
     * Get the context value.
     *
     * @return The naming store
     * @throws IllegalStateException
     */
    public synchronized NamingStore getValue() throws IllegalStateException {
        return store;
    }

    public Injector<NamingStore> getJavaContextInjector() {
        return javaContext;
    }
}
