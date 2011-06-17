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

package org.jboss.as.naming.service;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.context.NamespaceObjectFactory;
import org.jboss.as.naming.util.NameParser;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author John Bailey
 */
public class EEContextService implements Service<Void> {
    private final InjectedValue<NamingStore> javaContext = new InjectedValue<NamingStore>();
    private final String name;

    public EEContextService(String name) {
        this.name = name;
    }

    public synchronized void start(StartContext startContext) throws StartException {
        final Reference appReference = NamespaceObjectFactory.createReference(name);
        final NamingStore javaContext = this.javaContext.getValue();
        try {
            javaContext.rebind(NameParser.INSTANCE.parse(name), appReference, Reference.class);
        } catch (NamingException e) {
            throw new StartException("Failed to bind EE context: java:" + name, e);
        }
    }

    public synchronized void stop(StopContext stopContext) {
        final NamingStore javaContext = this.javaContext.getValue();
        try {
            javaContext.unbind(NameParser.INSTANCE.parse(name));
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to unbind EE context: java:" + name, e);
        }
    }

    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public Injector<NamingStore> getJavaContextInjector() {
        return javaContext;
    }
}
