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

import org.jboss.as.naming.context.NamespaceContextSelector;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author John Bailey
 */
public class InitialContext extends NamingContext {


    /**
     * Map of any additional naming schemes
     */
    private static volatile Map<String, ObjectFactory> urlContextFactories = Collections.emptyMap();

    public static synchronized void addUrlContextFactory(final String scheme, ObjectFactory factory) {
        Map<String, ObjectFactory> factories = new HashMap<String, ObjectFactory>(urlContextFactories);
        factories.put(scheme, factory);
        urlContextFactories = Collections.unmodifiableMap(factories);
    }

    public InitialContext(Hashtable<String, Object> environment) {
        super(environment);
    }

    public Object lookup(final Name name) throws NamingException {
        final ParsedName parsedName = parse(name);
        if (parsedName.namespace() == null) {
            //TODO: this is a bit of a hack, there should be a better way to handle this
            if (!parsedName.remaining().isEmpty()) {
                final String firstPart = parsedName.remaining().get(0);
                int index = firstPart.indexOf(':');
                if (index != -1) {
                    final String scheme = firstPart.substring(0, index);
                    ObjectFactory factory = urlContextFactories.get(scheme);
                    if (factory != null) {
                        try {
                            return ((Context)factory.getObjectInstance(null, name, this, getEnvironment())).lookup(name);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        if (parsedName.namespace() == null || parsedName.namespace().equals("")) {
            return super.lookup(parsedName.remaining());
        }
        final NamespaceContextSelector selector = NamespaceContextSelector.getCurrentSelector();
        if (selector == null) {
            throw new NameNotFoundException(name.toString());
        }
        final Context namespaceContext = selector.getContext(parsedName.namespace());
        if (namespaceContext == null) {
            throw new NameNotFoundException(name.toString());
        }
        return namespaceContext.lookup(parsedName.remaining());
    }

    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        final ParsedName parsedName = parse(name);
        if (parsedName.namespace() == null || parsedName.namespace().equals("")) {
            return super.listBindings(parsedName.remaining());
        }
        final NamespaceContextSelector selector = NamespaceContextSelector.getCurrentSelector();
        if (selector == null) {
            throw new NameNotFoundException(name.toString());
        }
        final Context namespaceContext = selector.getContext(parsedName.namespace());
        if (namespaceContext == null) {
            throw new NameNotFoundException(name.toString());
        }
        return namespaceContext.listBindings(parsedName.remaining());
    }

    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        final ParsedName parsedName = parse(name);
        if (parsedName.namespace() == null || parsedName.namespace().equals("")) {
            return super.list(parsedName.remaining());
        }
        final NamespaceContextSelector selector = NamespaceContextSelector.getCurrentSelector();
        if (selector == null) {
            throw new NameNotFoundException(name.toString());
        }
        final Context namespaceContext = selector.getContext(parsedName.namespace());
        if (namespaceContext == null) {
            throw new NameNotFoundException(name.toString());
        }
        return namespaceContext.list(parsedName.remaining());
    }

    private interface ParsedName {
        String namespace();

        Name remaining();
    }

    private ParsedName parse(final Name name) throws NamingException {
        final Name remaining;
        final String namespace;
        if (name.isEmpty()) {
            namespace = null;
            remaining = name;
        } else {
            final String first = name.get(0);
            if (first.startsWith("java:")) {
                final String theRest = first.substring(5);
                if (theRest.startsWith("/")) {
                    namespace = null;
                    remaining = getNameParser(theRest).parse(theRest);
                } else {
                    namespace = theRest;
                    remaining = name.getSuffix(1);
                }
            } else {
                namespace = null;
                remaining = name;
            }
        }

        return new ParsedName() {
            public String namespace() {
                return namespace;
            }

            public Name remaining() {
                return remaining;
            }
        };
    }
}
