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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.event.NamingListener;
import javax.naming.spi.ResolveResult;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author John Bailey
 * @author Jason T. Greene
 * @author Eduardo Martins
 */
public class ServiceBasedNamingStore implements NamingStore {
    private final Name EMPTY_NAME = new CompositeName();
    private Name baseName;
    private final ServiceRegistry serviceRegistry;
    private final ServiceName serviceNameBase;

    private ConcurrentSkipListSet<ServiceName> boundServices = new ConcurrentSkipListSet<ServiceName>();

    public ServiceBasedNamingStore(final ServiceRegistry serviceRegistry, final ServiceName serviceNameBase) {
        this.serviceRegistry = serviceRegistry;
        this.serviceNameBase = serviceNameBase;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name, true);
    }

    public Object lookup(final Name name, boolean dereference) throws NamingException {
        if (name.isEmpty()) {
            return new NamingContext(EMPTY_NAME, this, null);
        }
        final ServiceName lookupName = buildServiceName(name);
        Object obj = lookup(name.toString(), lookupName, dereference);
        if (obj == null) {
            final ServiceName lower = boundServices.lower(lookupName);
            if (lower != null && lower.isParentOf(lookupName)) {
                // Parent might be a reference or a link
                obj = lookup(name.toString(), lower, dereference);
                //if the lower is a context that has been explicitly bound then
                //we do not return a resolve result, as this will result in an
                //infinite loop
                if (!(obj instanceof NamingContext)) {
                    checkReferenceForContinuation(name, obj);
                    return new ResolveResult(obj, suffix(lower, lookupName));
                }
            }

            final ServiceName ceiling = boundServices.ceiling(lookupName);
            if (ceiling != null && lookupName.isParentOf(ceiling)) {
                if (lookupName.equals(ceiling)) {
                    //the binder service returned null
                    return null;
                }
                return new NamingContext((Name) name.clone(), this, null);
            }
            throw new NameNotFoundException(name.toString() + " -- " + lookupName);
        }

        return obj;
    }

    private void checkReferenceForContinuation(final Name name, final Object object) throws CannotProceedException {
        if (object instanceof Reference) {
            if (((Reference) object).get("nns") != null) {
                throw cannotProceedException(object, name);
            }
        }
    }

    private static CannotProceedException cannotProceedException(final Object resolvedObject, final Name remainingName) {
        final CannotProceedException cpe = new CannotProceedException();
        cpe.setResolvedObj(resolvedObject);
        cpe.setRemainingName(remainingName);
        return cpe;
    }

    private Object lookup(final String name, final ServiceName lookupName, boolean dereference) throws NamingException {
        try {
            final ServiceController<?> controller = serviceRegistry.getService(lookupName);
            if (controller != null) {
                final Object object = controller.getValue();
                if (dereference && object instanceof ManagedReferenceFactory) {
                    if(WildFlySecurityManager.isChecking()) {
                        //WFLY-3487 JNDI lookups should be executed in a clean access control context
                        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            @Override
                            public Object run() {
                                final ManagedReference managedReference = ManagedReferenceFactory.class.cast(object).getReference();
                                return managedReference != null ? managedReference.getInstance() : null;
                            }
                        });
                    } else {
                        final ManagedReference managedReference = ManagedReferenceFactory.class.cast(object).getReference();
                        return managedReference != null ? managedReference.getInstance() : null;
                    }
                } else {
                    return object;
                }
            } else {
                return null;
            }
        } catch (IllegalStateException e) {
            NameNotFoundException n = new NameNotFoundException(name);
            n.initCause(e);
            throw n;
        } catch (SecurityException ex) {
            throw ex;
        } catch (Throwable t) {
            throw NamingLogger.ROOT_LOGGER.lookupError(t, name);
        }
    }

    public List<NameClassPair> list(final Name name) throws NamingException {
        final ServiceName lookupName = buildServiceName(name);
        final ServiceName floor = boundServices.floor(lookupName);
        boolean isContextBinding = false;
        if (floor != null && floor.isParentOf(lookupName)) {
            // Parent might be a reference or a link
            Object obj = lookup(name.toString(), floor, true);
            if (obj instanceof NamingContext) {
                isContextBinding = true;
            } else if (obj != null) {
                throw new RequireResolveException(convert(floor));
            }
        }

        final List<ServiceName> children = listChildren(lookupName, isContextBinding);
        final String[] lookupParts = lookupName.toArray();
        final Set<String> childContexts = new HashSet<String>();
        final List<NameClassPair> results = new ArrayList<NameClassPair>();
        for (ServiceName child : children) {
            final String[] childParts = child.toArray();
            if (childParts.length > lookupParts.length + 1) {
                childContexts.add(childParts[lookupParts.length]);
            } else {
                final Object binding = lookup(name.toString(), child, false);
                final String bindingType;
                if (binding instanceof ContextListManagedReferenceFactory) {
                    bindingType = ContextListManagedReferenceFactory.class.cast(binding)
                            .getInstanceClassName();
                } else {
                    if (binding instanceof ManagedReferenceFactory) {
                        bindingType = ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
                    } else {
                        bindingType = binding.getClass().getName();
                    }
                }
                results.add(new NameClassPair(childParts[childParts.length - 1],bindingType));
            }
        }
        for (String contextName : childContexts) {
            results.add(new NameClassPair(contextName, Context.class.getName()));
        }
        return results;
    }

    public List<Binding> listBindings(final Name name) throws NamingException {
        final ServiceName lookupName = buildServiceName(name);
        final ServiceName floor = boundServices.floor(lookupName);
        boolean isContextBinding = false;
        if (floor != null && floor.isParentOf(lookupName)) {
            // Parent might be a reference or a link
            Object obj = lookup(name.toString(), floor, true);
            if (obj instanceof NamingContext) {
                isContextBinding = true;
            } else if (obj != null) {
                throw new RequireResolveException(convert(floor));
            }
        }
        final List<ServiceName> children = listChildren(lookupName, isContextBinding);
        final String[] lookupParts = lookupName.toArray();
        final Set<String> childContexts = new HashSet<String>();
        final List<Binding> results = new ArrayList<Binding>();
        for (ServiceName child : children) {
            final String[] childParts = child.toArray();
            if (childParts.length > lookupParts.length + 1) {
                childContexts.add(childParts[lookupParts.length]);
            } else {
                final Object binding = lookup(name.toString(), child, true);
                results.add(new Binding(childParts[childParts.length - 1], binding));
            }
        }
        for (String contextName : childContexts) {
            results.add(new Binding(contextName, new NamingContext(((Name) name.clone()).add(contextName), this, null)));
        }
        return results;
    }

    private List<ServiceName> listChildren(final ServiceName name, boolean isContextBinding) throws NamingException {
        final ConcurrentSkipListSet<ServiceName> boundServices = this.boundServices;
        if (!isContextBinding && boundServices.contains(name)) {
            throw NamingLogger.ROOT_LOGGER.cannotListNonContextBinding();
        }
        final NavigableSet<ServiceName> tail = boundServices.tailSet(name);
        final List<ServiceName> children = new ArrayList<ServiceName>();
        for (ServiceName next : tail) {
            if (name.isParentOf(next)) {
                if (!name.equals(next)) {
                    children.add(next);
                }
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
            throw NamingLogger.ROOT_LOGGER.serviceAlreadyBound(serviceName);
        }
        boundServices.add(serviceName);
    }

    public void remove(final ServiceName serviceName) {
        boundServices.remove(serviceName);
    }

    protected ServiceName buildServiceName(final Name name) {
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

    private Name convert(ServiceName serviceName) {
        String[] c = serviceName.toArray();
        CompositeName name = new CompositeName();
        int baseIndex = serviceNameBase.toArray().length;
        for (int i = baseIndex; i < c.length; i++) {
            try {
                name.add(c[i]);
            } catch (InvalidNameException e) {
                throw new IllegalStateException(e);
            }
        }

        return name;
    }

    private Name suffix(ServiceName parent, ServiceName child) {
        String[] p = parent.toArray();
        String[] c = child.toArray();

        CompositeName name = new CompositeName();
        for (int i = p.length; i < c.length; i++) {
            try {
                name.add(c[i]);
            } catch (InvalidNameException e) {
                throw new IllegalStateException(e);
            }
        }

        return name;
    }

    protected ServiceName getServiceNameBase() {
        return serviceNameBase;
    }

    protected ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    public Name getBaseName() throws NamingException {
        if (baseName == null) {
            baseName = setBaseName();
        }
        return baseName;
    }

    private Name setBaseName() throws NamingException {
        if (serviceNameBase.equals(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME)
                || ContextNames.EXPORTED_CONTEXT_SERVICE_NAME.isParentOf(serviceNameBase)) {
            return new CompositeName("java:jboss/exported");
        }
        if (serviceNameBase.equals(ContextNames.JBOSS_CONTEXT_SERVICE_NAME)
                || ContextNames.JBOSS_CONTEXT_SERVICE_NAME.isParentOf(serviceNameBase)) {
            return new CompositeName("java:jboss");
        }
        if (serviceNameBase.equals(ContextNames.APPLICATION_CONTEXT_SERVICE_NAME)
                || ContextNames.APPLICATION_CONTEXT_SERVICE_NAME.isParentOf(serviceNameBase)) {
            return new CompositeName("java:app");
        }
        if (serviceNameBase.equals(ContextNames.MODULE_CONTEXT_SERVICE_NAME)
                || ContextNames.MODULE_CONTEXT_SERVICE_NAME.isParentOf(serviceNameBase)) {
            return new CompositeName("java:module");
        }
        if (serviceNameBase.equals(ContextNames.COMPONENT_CONTEXT_SERVICE_NAME)
                || ContextNames.COMPONENT_CONTEXT_SERVICE_NAME.isParentOf(serviceNameBase)) {
            return new CompositeName("java:comp");
        }
        if (serviceNameBase.equals(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME)
                || ContextNames.GLOBAL_CONTEXT_SERVICE_NAME.isParentOf(serviceNameBase)) {
            return new CompositeName("java:global");
        }
        if (serviceNameBase.equals(ContextNames.JAVA_CONTEXT_SERVICE_NAME)
                || ContextNames.JAVA_CONTEXT_SERVICE_NAME.isParentOf(serviceNameBase)) {
            return new CompositeName("java:");
        }
        return new CompositeName();
    }
}
