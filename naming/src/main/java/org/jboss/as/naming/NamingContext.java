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

package org.jboss.as.naming;

import static org.jboss.as.naming.logging.NamingLogger.ROOT_LOGGER;
import static org.jboss.as.naming.util.NamingUtils.isEmpty;
import static org.jboss.as.naming.util.NamingUtils.namingEnumeration;
import static org.jboss.as.naming.util.NamingUtils.namingException;
import static org.jboss.as.naming.util.NamingUtils.notAContextException;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ResolveResult;

import org.wildfly.naming.java.permission.JndiPermission;
import org.jboss.as.naming.context.ObjectFactoryBuilder;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.util.NameParser;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Naming context implementation which proxies calls to a {@code NamingStore} instance.  This context is
 * read-only.
 *
 * @author John E. Bailey
 */
public class NamingContext implements EventContext {

    /**
     * A {@link org.jboss.as.naming.NamingPermission} needed to set the active {@link org.jboss.as.naming.NamingStore}. The name of the permission is "{@code setActiveNamingStore}."
     */
    private static final NamingPermission SET_ACTIVE_NAMING_STORE = new NamingPermission("setActiveNamingStore");
    /*
     * The active naming store to use for any context created without a name store.
     */
    private static volatile NamingStore ACTIVE_NAMING_STORE = new InMemoryNamingStore();

    /**
     * Set the active naming store
     *
     * @param namingStore The naming store
     */
    public static void setActiveNamingStore(final NamingStore namingStore) {
        if(WildFlySecurityManager.isChecking()) {
            System.getSecurityManager().checkPermission(SET_ACTIVE_NAMING_STORE);
        }
        ACTIVE_NAMING_STORE = namingStore;
    }

    private static final String PACKAGE_PREFIXES = "org.jboss.as.naming.interfaces";

    static {
        try {
            NamingManager.setObjectFactoryBuilder(ObjectFactoryBuilder.INSTANCE);
        } catch(Throwable t) {
            ROOT_LOGGER.failedToSet(t, "ObjectFactoryBuilder");
        }
    }

    /**
     * Initialize the naming components required by {@link javax.naming.spi.NamingManager}.
     */
    public static void initializeNamingManager() {
        // Setup naming environment
        final String property = WildFlySecurityManager.getPropertyPrivileged(Context.URL_PKG_PREFIXES, null);
        if(property == null || property.isEmpty()) {
            WildFlySecurityManager.setPropertyPrivileged(Context.URL_PKG_PREFIXES, PACKAGE_PREFIXES);
        } else if(!Arrays.asList(property.split(":")).contains(PACKAGE_PREFIXES)) {
            WildFlySecurityManager.setPropertyPrivileged(Context.URL_PKG_PREFIXES, PACKAGE_PREFIXES + ":" + property);
        }
        try {
            //If we are reusing the JVM. e.g. in tests we should not set this again
            if (!NamingManager.hasInitialContextFactoryBuilder())
                NamingManager.setInitialContextFactoryBuilder(new InitialContextFactoryBuilder());
        } catch (NamingException e) {
            ROOT_LOGGER.failedToSet(e, "InitialContextFactoryBuilder");
        }
    }

    /* The naming store providing the back-end storage */
    private final NamingStore namingStore;

    /* The name prefix the context represents. */
    private final Name prefix;

    /* The environment configuration */
    private final Hashtable environment;

    /**
     * Create a new naming context with no prefix or naming store.  This will default to a prefix of "" and
     * the active naming store.
     *
     * @param environment The naming environment
     */
    public NamingContext(final Hashtable environment) {
        this(new CompositeName(), ACTIVE_NAMING_STORE, environment);
    }

    /**
     * Create a context with a prefix name.
     *
     * @param prefix The prefix for this context
     * @param environment The naming environment
     * @throws NamingException if an error occurs
     */
    public NamingContext(final Name prefix, final Hashtable environment) throws NamingException {
        this(prefix, ACTIVE_NAMING_STORE, environment);
    }

    /**
     * Create a new naming context with a prefix name and a NamingStore instance to use as a backing.
     *
     * @param prefix The prefix for this context
     * @param namingStore The NamingStore
     * @param environment The naming environment
     */
    public NamingContext(final Name prefix, final NamingStore namingStore, final Hashtable environment) {
        if(prefix == null) {
            throw NamingLogger.ROOT_LOGGER.nullVar("Naming prefix");
        }
        this.prefix = prefix;
        if(namingStore == null) {
            throw NamingLogger.ROOT_LOGGER.nullVar("NamingStore");
        }
        this.namingStore = namingStore;
        if(environment != null) {
            this.environment = new Hashtable(environment);
        } else {
            this.environment = new Hashtable();
        }
    }

    /**
     * Create a new naming context with the given namingStore and an empty name.
     *
     * @param namingStore the naming store to use
     * @param environment the environment to use
     */
    public NamingContext(final NamingStore namingStore, final Hashtable environment) {
        this(new CompositeName(), namingStore, environment);
    }

    /** {@inheritDoc} */
    public Object lookup(final Name name) throws NamingException {
        return lookup(name, true);
    }

    /** {@inheritDoc} */
    public Object lookup(final String name) throws NamingException {
        return lookup(name, true);
    }

    public Object lookup(final String name, boolean dereference) throws NamingException {
        return lookup(parseName(name), dereference);
    }

    public Object lookup(final Name name, boolean dereference) throws NamingException {
        check(name, JndiPermission.ACTION_LOOKUP);

        if (isEmpty(name)) {
            return new NamingContext(prefix, namingStore, environment);
        }

        final Name absoluteName = getAbsoluteName(name);

        Object result;
        try {
            result = namingStore.lookup(absoluteName,dereference);
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            if (continuationContext instanceof NamingContext) {
                result = ((NamingContext)continuationContext).lookup(cpe.getRemainingName(), dereference);
            } else {
                result = continuationContext.lookup(cpe.getRemainingName());
            }
        }

        if (result instanceof ResolveResult) {
            final ResolveResult resolveResult = (ResolveResult) result;
            final Object resolvedObject = resolveResult.getResolvedObj();

            Object context;
            if (resolvedObject instanceof Context){
                context = resolvedObject;
            } else if (resolvedObject instanceof LinkRef) {
                context = resolveLink(resolvedObject,dereference);
            } else {
                context = getObjectInstance(resolvedObject, absoluteName, environment);
            }
            if (!(context instanceof Context)) {
                throw notAContextException(absoluteName.getPrefix(absoluteName.size() - resolveResult.getRemainingName().size()));
            }
            final Context namingContext = (Context) context;
            if (namingContext instanceof NamingContext) {
                return ((NamingContext)namingContext).lookup(resolveResult.getRemainingName(), dereference);
            } else {
                return namingContext.lookup(resolveResult.getRemainingName());
            }
        } else if (result instanceof LinkRef) {
            result = resolveLink(result,dereference);
        } else if (result instanceof Reference) {
            result = getObjectInstance(result, absoluteName, environment);
            if (result instanceof LinkRef) {
                result = resolveLink(result,dereference);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    public void bind(final Name name, final Object object) throws NamingException {
        check(name, JndiPermission.ACTION_BIND);

        if(namingStore instanceof WritableNamingStore) {
            final Name absoluteName = getAbsoluteName(name);
            final Object value;
            if (object instanceof Referenceable) {
                value = ((Referenceable) object).getReference();
            } else {
                value = object;
            }
            if (System.getSecurityManager() == null) {
                getWritableNamingStore().bind(absoluteName, value);
            } else {
                // The permissions check has already happened for the binding further permissions should be allowed
                final NamingException e = AccessController.doPrivileged(new PrivilegedAction<NamingException>() {
                    @Override
                    public NamingException run() {
                        try {
                            getWritableNamingStore().bind(absoluteName, value);
                        } catch (NamingException e) {
                            return e;
                        }
                        return null;
                    }
                });
                // Check that a NamingException wasn't thrown during the bind
                if (e != null) {
                    throw e;
                }
            }
        } else {
            throw NamingLogger.ROOT_LOGGER.readOnlyNamingContext();
        }

    }

    /** {@inheritDoc} */
    public void bind(final String name, final Object obj) throws NamingException {
        bind(parseName(name), obj);
    }

    /** {@inheritDoc} */
    public void rebind(final Name name, Object object) throws NamingException {
        check(name, JndiPermission.ACTION_REBIND);

        if(namingStore instanceof WritableNamingStore) {
            final Name absoluteName = getAbsoluteName(name);
            if (object instanceof Referenceable) {
                object = ((Referenceable) object).getReference();
            }
            getWritableNamingStore().rebind(absoluteName, object);
        } else {
            throw NamingLogger.ROOT_LOGGER.readOnlyNamingContext();
        }
    }

    /** {@inheritDoc} */
    public void rebind(final String name, final Object object) throws NamingException {
        rebind(parseName(name), object);
    }

    /** {@inheritDoc} */
    public void unbind(final Name name) throws NamingException {
        check(name, JndiPermission.ACTION_UNBIND);

        if(namingStore instanceof WritableNamingStore) {
            final Name absoluteName = getAbsoluteName(name);
            getWritableNamingStore().unbind(absoluteName);
        } else {
            throw NamingLogger.ROOT_LOGGER.readOnlyNamingContext();
        }
    }

    /** {@inheritDoc} */
    public void unbind(final String name) throws NamingException {
        unbind(parseName(name));
    }

    /** {@inheritDoc} */
    public void rename(final Name oldName, final Name newName) throws NamingException {
        //check for appropriate permissions first so that no other info leaks from this context
        //in case of insufficient perms (like the fact if it is readonly or not)
        check(oldName, JndiPermission.ACTION_LOOKUP | JndiPermission.ACTION_UNBIND);
        check(newName, JndiPermission.ACTION_BIND);

        if (namingStore instanceof WritableNamingStore) {
            bind(newName, lookup(oldName));
            unbind(oldName);
        } else {
            throw NamingLogger.ROOT_LOGGER.readOnlyNamingContext();
        }
    }

    /** {@inheritDoc} */
    public void rename(final String oldName, final String newName) throws NamingException {
        rename(parseName(oldName), parseName(newName));
    }

    /** {@inheritDoc} */
    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        check(name, JndiPermission.ACTION_LIST);

        try {
            return namingEnumeration(namingStore.list(getAbsoluteName(name)));
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            return continuationContext.list(cpe.getRemainingName());
        }  catch (RequireResolveException r) {
            final Object o = lookup(r.getResolve());
            if (o instanceof Context) {
                return ((Context)o).list(name.getSuffix(r.getResolve().size()));
            }

            throw notAContextException(r.getResolve());
        }
    }

    /** {@inheritDoc} */
    public NamingEnumeration<NameClassPair> list(final String name) throws NamingException {
        return list(parseName(name));
    }

    /** {@inheritDoc} */
    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        check(name, JndiPermission.ACTION_LIST_BINDINGS);

        try {
            return namingEnumeration(namingStore.listBindings(getAbsoluteName(name)));
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            return continuationContext.listBindings(cpe.getRemainingName());
        } catch (RequireResolveException r) {
            final Object o = lookup(r.getResolve());
            if (o instanceof Context) {
                return ((Context)o).listBindings(name.getSuffix(r.getResolve().size()));
            }

            throw notAContextException(r.getResolve());
        }
    }

    /** {@inheritDoc} */
    public NamingEnumeration<Binding> listBindings(final String name) throws NamingException {
        return listBindings(parseName(name));
    }

    /** {@inheritDoc} */
    public void destroySubcontext(final Name name) throws NamingException {
        check(name, JndiPermission.ACTION_DESTROY_SUBCONTEXT);

        if(!(namingStore instanceof WritableNamingStore)) {
            throw NamingLogger.ROOT_LOGGER.readOnlyNamingContext();
        }
    }

    /** {@inheritDoc} */
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(parseName(name));
    }

    /** {@inheritDoc} */
    public Context createSubcontext(Name name) throws NamingException {
        check(name, JndiPermission.ACTION_CREATE_SUBCONTEXT);

        if(namingStore instanceof WritableNamingStore) {
            final Name absoluteName = getAbsoluteName(name);
            return getWritableNamingStore().createSubcontext(absoluteName);
        } else {
            throw NamingLogger.ROOT_LOGGER.readOnlyNamingContext();
        }
    }

    /** {@inheritDoc} */
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(parseName(name));
    }

    /** {@inheritDoc} */
    public Object lookupLink(Name name) throws NamingException {
        check(name, JndiPermission.ACTION_LOOKUP);

        if (name.isEmpty()) {
            return lookup(name);
        }
        try {
            final Name absoluteName = getAbsoluteName(name);
            Object link = namingStore.lookup(absoluteName);
            if (!(link instanceof LinkRef) && link instanceof Reference) {
                link = getObjectInstance(link, name, null);
            }
            return link;
        } catch (Exception e) {
            throw namingException(NamingLogger.ROOT_LOGGER.cannotLookupLink(), e, name);
        }
    }

    /** {@inheritDoc} */
    public Object lookupLink(String name) throws NamingException {
        return lookupLink(parseName(name));
    }

    /** {@inheritDoc} */
    public NameParser getNameParser(Name name) throws NamingException {
        return NameParser.INSTANCE;
    }

    /** {@inheritDoc} */
    public NameParser getNameParser(String name) throws NamingException {
        return NameParser.INSTANCE;
    }

    /** {@inheritDoc} */
    public Name composeName(Name name, Name prefix) throws NamingException {
        final Name result = (Name) prefix.clone();
        if (name instanceof CompositeName) {
            if (name.size() == 1) {
                // name could be a nested name
                final String firstComponent = name.get(0);
                result.addAll(parseName(firstComponent));
            } else {
                result.addAll(name);
            }
        } else {
            result.addAll(new CompositeName(name.toString()));
        }
        return result;
    }

    /** {@inheritDoc} */
    public String composeName(String name, String prefix) throws NamingException {
        return composeName(parseName(name), parseName(prefix)).toString();
    }

    /** {@inheritDoc} */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        final Object existing = environment.get(propName);
        environment.put(propName, propVal);
        return existing;
    }

    /** {@inheritDoc} */
    public Object removeFromEnvironment(String propName) throws NamingException {
        return environment.remove(propName);
    }

    /** {@inheritDoc} */
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return environment;
    }

    /** {@inheritDoc} */
    public void close() throws NamingException {
        // NO-OP
    }

    /** {@inheritDoc} */
    public String getNameInNamespace() throws NamingException {
        return prefix.toString();
    }

    /** {@inheritDoc} */
    public void addNamingListener(final Name target, final int scope, final NamingListener listener) throws NamingException {
        check(target, JndiPermission.ACTION_ADD_NAMING_LISTENER);
        namingStore.addNamingListener(target, scope, listener);
    }

    /** {@inheritDoc} */
    public void addNamingListener(final String target, final int scope, final NamingListener listener) throws NamingException {
        addNamingListener(parseName(target), scope, listener);
    }

    /** {@inheritDoc} */
    public void removeNamingListener(final NamingListener listener) throws NamingException {
        namingStore.removeNamingListener(listener);
    }

    /** {@inheritDoc} */
    public boolean targetMustExist() throws NamingException {
        return false;
    }

    private Name parseName(final String name) throws NamingException {
        return getNameParser(name).parse(name);
    }

    private Name getAbsoluteName(final Name name) throws NamingException {
        if(name.isEmpty()) {
            return composeName(name, prefix);
        }
        final String firstComponent = name.get(0);
        if(firstComponent.startsWith("java:")) {
            final String cleaned = firstComponent.substring(5);
            final Name suffix = name.getSuffix(1);
            if(cleaned.isEmpty()) {
                return suffix;
            }
            return suffix.add(0, cleaned);
        } else if(firstComponent.isEmpty()) {
            return name.getSuffix(1);
        } else {
            return composeName(name, prefix);
        }
    }

    private Object getObjectInstance(final Object object, final Name name, final Hashtable<?, ?> environment) throws NamingException {
        try {
            final ObjectFactoryBuilder factoryBuilder = ObjectFactoryBuilder.INSTANCE;
            final ObjectFactory objectFactory = factoryBuilder.createObjectFactory(object, environment);
            return objectFactory == null ? null : objectFactory.getObjectInstance(object, name, this, environment);
        } catch(NamingException e) {
            throw e;
        } catch(Throwable t) {
            throw NamingLogger.ROOT_LOGGER.cannotDeferenceObject(t);
        }
    }

    private Object resolveLink(Object result, boolean dereference) throws NamingException {
        final Object linkResult;
        try {
            final LinkRef linkRef = (LinkRef) result;
            final String referenceName = linkRef.getLinkName();
            if (referenceName.startsWith("./")) {
                linkResult = lookup(parseName(referenceName.substring(2)) ,dereference);
            } else {
                linkResult = new InitialContext().lookup(referenceName);
            }
        } catch (Throwable t) {
            throw NamingLogger.ROOT_LOGGER.cannotDeferenceObject(t);
        }
        return linkResult;
    }

    private void check(Name name, int actions) throws NamingException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null && WildFlySecurityManager.isChecking()) {
            // build absolute name (including store's base name)
            Name absoluteName = (Name) namingStore.getBaseName().clone();
            if (name.isEmpty()) {
                absoluteName.addAll(prefix);
            } else {
                final String firstComponent = name.get(0);
                if (firstComponent.startsWith("java:")) {
                    absoluteName = name;
                } else if (firstComponent.isEmpty()) {
                    absoluteName.addAll(name.getSuffix(1));
                } else {
                    absoluteName.addAll(prefix);
                    if(name instanceof CompositeName) {
                        if (name.size() == 1) {
                            // name could be a nested name
                            absoluteName.addAll(parseName(firstComponent));
                        } else {
                            absoluteName.addAll(name);
                        }
                    } else {
                        absoluteName.addAll(new CompositeName(name.toString()));
                    }
                }
            }
            sm.checkPermission(new JndiPermission(absoluteName.toString(), actions));
        }
    }

    Name getPrefix() {
        return prefix;
    }

    NamingStore getNamingStore() {
        return namingStore;
    }

    WritableNamingStore getWritableNamingStore() {
        return WritableNamingStore.class.cast(getNamingStore());
    }
}
