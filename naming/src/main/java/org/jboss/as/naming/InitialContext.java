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

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.spi.ObjectFactory;

import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.naming.logging.NamingLogger;
import org.wildfly.naming.client.WildFlyInitialContextFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Eduardo Martins
 * @author John Bailey
 */
public class InitialContext extends InitialLdapContext {

    /**
     * Map of any additional naming schemes
     */
    private static volatile Map<String, ObjectFactory> urlContextFactories = Collections.emptyMap();

    private final WildFlyInitialContextFactory delegate = new WildFlyInitialContextFactory();

    /**
     * Add an ObjectFactory to handle requests for a specific URL scheme.
     * @param scheme The URL scheme to handle.
     * @param factory The ObjectFactory that can handle the requests.
     */
    public static synchronized void addUrlContextFactory(final String scheme, ObjectFactory factory) {
        Map<String, ObjectFactory> factories = new HashMap<String, ObjectFactory>(urlContextFactories);
        factories.put(scheme, factory);
        urlContextFactories = Collections.unmodifiableMap(factories);
    }

    /**
     * Remove an ObjectFactory from the map of registered ones. To make sure that not anybody can remove an
     * ObjectFactory both the scheme as well as the actual object factory itself need to be supplied. So you
     * can only remove the factory if you have the factory object.
     * @param scheme The URL scheme for which the handler is registered.
     * @param factory The factory object associated with the scheme
     * @throws IllegalArgumentException if the requested scheme/factory combination is not registered.
     */
    public static synchronized void removeUrlContextFactory(final String scheme, ObjectFactory factory) {
        Map<String, ObjectFactory> factories = new HashMap<String, ObjectFactory>(urlContextFactories);

        ObjectFactory f = factories.get(scheme);
        if (f == factory) {
            factories.remove(scheme);
            urlContextFactories = Collections.unmodifiableMap(factories);
            return;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public InitialContext(Hashtable<?,?> environment) throws NamingException {
        super(environment, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void init(Hashtable<?,?> environment) throws NamingException {
        // the jdk initial context already worked out the env, no need to do it again
        myProps = (Hashtable<Object, Object>) environment;
        if (myProps != null && myProps.get(Context.INITIAL_CONTEXT_FACTORY) != null) {
            // user has specified initial context factory; try to get it
            getDefaultInitCtx();
        }
    }

    @Override
    protected Context getDefaultInitCtx() throws NamingException {
        if (!gotDefault) {
            // if there is an initial context factory prop in the env use it to create the default ctx
            final String factoryClassName = myProps != null ? (String) myProps.get(Context.INITIAL_CONTEXT_FACTORY) : null;
            if(factoryClassName == null || InitialContextFactory.class.getName().equals(factoryClassName)) {
                defaultInitCtx = new DefaultInitialContext(myProps);
            } else {
                final ClassLoader classLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                    final Class<?> factoryClass = Class.forName(factoryClassName, true, classLoader);
                    defaultInitCtx = ((javax.naming.spi.InitialContextFactory)factoryClass.newInstance()).getInitialContext(myProps);
                } catch (NamingException e) {
                    throw e;
                } catch (Exception e) {
                    throw NamingLogger.ROOT_LOGGER.failedToInstantiate(e, "InitialContextFactory", factoryClassName, classLoader);
                }
            }
            gotDefault = true;
        }
        return defaultInitCtx;
    }

    @Override
    protected Context getURLOrDefaultInitCtx(String name) throws NamingException {
        String scheme = getURLScheme(name);
        if (scheme != null && !scheme.equals("java")) {
            ObjectFactory factory = urlContextFactories.get(scheme);
            if (factory != null) {
                try {
                    return (Context) factory.getObjectInstance(null, null, null, myProps);
                }catch(NamingException e) {
                    throw e;
                } catch (Exception e) {
                    NamingException n = new NamingException(e.getMessage());
                    n.initCause(e);
                    throw n;
                }
            } else {
                Context ctx = delegate.getInitialContext(myProps);
                if(ctx!=null) {
                    return ctx;
                }
            }
        }
        return getDefaultInitCtx();
    }

    @Override
    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException {
        if (name.size() > 0) {
            return getURLOrDefaultInitCtx(name.get(0));
        }
        return getDefaultInitCtx();
    }

    public static String getURLScheme(String str) {
        int colon_posn = str.indexOf(':');
        int slash_posn = str.indexOf('/');

        if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn))
            return str.substring(0, colon_posn);
        return null;
    }

    private interface ParsedName {
        String namespace();
        Name remaining();
    }

    static class DefaultInitialContext extends NamingContext {

        DefaultInitialContext(Hashtable<Object,Object> environment) {
            super(environment);
        }

        private Context findContext(final Name name, final ParsedName parsedName) throws NamingException {
            if (parsedName.namespace() == null || parsedName.namespace().equals("")) {
                return null;
            }
            final NamespaceContextSelector selector = NamespaceContextSelector.getCurrentSelector();
            if (selector == null) {
                throw new NameNotFoundException(name.toString());
            }
            final Context namespaceContext = selector.getContext(parsedName.namespace());
            if (namespaceContext == null) {
                throw new NameNotFoundException(name.toString());
            }
            return namespaceContext;
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
                    } else if (theRest.equals("jboss") && name.size() > 1 && name.get(1).equals("exported")) {
                        namespace = "jboss/exported";
                        remaining = name.getSuffix(2);
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

        public Object lookup(final Name name, boolean dereference) throws NamingException {
            final ParsedName parsedName = parse(name);
            final Context namespaceContext = findContext(name, parsedName);
            if (namespaceContext == null)
                return super.lookup(parsedName.remaining(),dereference);
            else
                return namespaceContext.lookup(parsedName.remaining());
        }

        public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
            final ParsedName parsedName = parse(name);
            final Context namespaceContext = findContext(name, parsedName);
            if (namespaceContext == null)
                return super.listBindings(parsedName.remaining());
            else
                return namespaceContext.listBindings(parsedName.remaining());
        }

        public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
            final ParsedName parsedName = parse(name);
            final Context namespaceContext = findContext(name, parsedName);
            if (namespaceContext == null)
                return super.list(parsedName.remaining());
            else
                return namespaceContext.list(parsedName.remaining());
        }

        public void bind(Name name, Object object) throws NamingException {
            final ParsedName parsedName = parse(name);
            final Context namespaceContext = findContext(name, parsedName);
            if (namespaceContext == null)
                super.bind(parsedName.remaining(), object);
            else
                namespaceContext.bind(parsedName.remaining(), object);
        }

        public void rebind(Name name, Object object) throws NamingException {
            final ParsedName parsedName = parse(name);
            final Context namespaceContext = findContext(name, parsedName);
            if (namespaceContext == null)
                super.rebind(parsedName.remaining(), object);
            else
                namespaceContext.rebind(parsedName.remaining(), object);
        }

        public void unbind(Name name) throws NamingException {
            final ParsedName parsedName = parse(name);
            final Context namespaceContext = findContext(name, parsedName);
            if (namespaceContext == null)
                super.unbind(parsedName.remaining());
            else
                namespaceContext.unbind(parsedName.remaining());
        }

        public void destroySubcontext(Name name) throws NamingException {
            final ParsedName parsedName = parse(name);
            final Context namespaceContext = findContext(name, parsedName);
            if (namespaceContext == null)
                super.destroySubcontext(parsedName.remaining());
            else
                namespaceContext.destroySubcontext(parsedName.remaining());
        }

        public Context createSubcontext(Name name) throws NamingException {
            final ParsedName parsedName = parse(name);
            final Context namespaceContext = findContext(name, parsedName);
            if (namespaceContext == null)
                return super.createSubcontext(parsedName.remaining());
            else
                return namespaceContext.createSubcontext(parsedName.remaining());
        }
    }
}
