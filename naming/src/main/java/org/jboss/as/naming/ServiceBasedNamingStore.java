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

package org.jboss.as.naming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.event.NamingListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author John Bailey
 */
public class ServiceBasedNamingStore implements NamingStore {
    private final Name EMPTY_NAME = new CompositeName();

    private final ServiceRegistry serviceRegistry;
    private final ServiceName serviceNameBase;

    private ConcurrentSkipListSet<ServiceName> boundServices = new ConcurrentSkipListSet<ServiceName>();

    public ServiceBasedNamingStore(final ServiceRegistry serviceRegistry, final ServiceName serviceNameBase) {
        this.serviceRegistry = serviceRegistry;
        this.serviceNameBase = serviceNameBase;
    }

    public Object lookup(final Name name) throws NamingException {
        if(name.isEmpty()) {
            return new NamingContext(EMPTY_NAME, this, null);
        }
        final ServiceName lookupName = buildServiceName(name);
        final Object obj = lookup(lookupName);
        if (obj == null) {
            final ServiceName ceiling = boundServices.ceiling(lookupName);
            if (ceiling != null && lookupName.isParentOf(ceiling)) {
                return new NamingContext((Name)name.clone(), this, null);
            }
            throw new NameNotFoundException(name.toString() + " -- " + lookupName);
        }
        return obj;
    }

    private Object lookup(final ServiceName lookupName) {
        final ServiceController<?> controller = serviceRegistry.getService(lookupName);
        if (controller != null) {
            final Object object = controller.getValue();
            if (object instanceof ManagedReferenceFactory) {
                return ManagedReferenceFactory.class.cast(object).getReference().getInstance();
            }
            return object;
        }
        return null;
    }

    public List<NameClassPair> list(final Name name) throws NamingException {
        final ServiceName lookupName = buildServiceName(name);
        final List<ServiceName> children = listChildren(lookupName);
        final String[] lookupParts = lookupName.toArray();
        final Set<String> childContexts = new HashSet<String>();
        final List<NameClassPair> results = new ArrayList<NameClassPair>();
        for (ServiceName child : children) {
            final String[] childParts = child.toArray();
            if (childParts.length > lookupParts.length + 1) {
                childContexts.add(childParts[lookupParts.length]);
            } else {
                final Object binding = lookup(child);
                results.add(new NameClassPair(childParts[childParts.length - 1], binding.getClass().getName()));
            }
        }
        for(String contextName : childContexts) {
            results.add(new NameClassPair(contextName, Context.class.getName()));
        }
        return results;
    }

    public List<Binding> listBindings(final Name name) throws NamingException {
        final ServiceName lookupName = buildServiceName(name);
        final List<ServiceName> children = listChildren(lookupName);
        final String[] lookupParts = lookupName.toArray();
        final Set<String> childContexts = new HashSet<String>();
        final List<Binding> results = new ArrayList<Binding>();
        for (ServiceName child : children) {
            final String[] childParts = child.toArray();
            if (childParts.length > lookupParts.length + 1) {
                childContexts.add(childParts[lookupParts.length]);
            } else {
                final Object binding = lookup(child);
                results.add(new Binding(childParts[childParts.length - 1], binding));
            }
        }
        for(String contextName : childContexts) {
            results.add(new Binding(contextName, new NamingContext(((Name)name.clone()).add(contextName), this, null)));
        }
        return results;
    }

    private List<ServiceName> listChildren(final ServiceName name) throws NamingException {
        final ConcurrentSkipListSet<ServiceName> boundServices = this.boundServices;
        if (boundServices.contains(name)) {
            throw new NamingException("Unable to list a non Context binding.");
        }
        final NavigableSet<ServiceName> tail = boundServices.tailSet(name);
        final List<ServiceName> children = new ArrayList<ServiceName>();
        for (ServiceName next : tail) {
            if (name.isParentOf(next)) {
                children.add(next);
            } else {
                break;
            }
        }
        return children;
    }

    public void close() throws NamingException {
        boundServices.clear();
    }

    public void addNamingListener(Name target, int scope, NamingListener listener) {
    }

    public void removeNamingListener(NamingListener listener) {
    }

    public void add(final ServiceName serviceName) {
        final ConcurrentSkipListSet<ServiceName> boundServices = this.boundServices;
        if (boundServices.contains(serviceName)) {
            throw new IllegalArgumentException("Service with name [" + serviceName + "] already bound.");
        }
        boundServices.add(serviceName);
    }

    public void remove(final ServiceName serviceName) {
        boundServices.remove(serviceName);
    }

    private ServiceName buildServiceName(final Name name) {
        final Enumeration<String> parts = name.getAll();
        ServiceName current = serviceNameBase;
        while (parts.hasMoreElements()) {
            final String currentPart = parts.nextElement();
            if (!currentPart.isEmpty()) {
                current = current.append(currentPart);
            }
        }
        return current;
    }
}
