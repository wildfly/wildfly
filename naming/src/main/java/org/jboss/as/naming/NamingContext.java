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

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ResolveResult;
import java.util.Hashtable;

/**
 * Naming context implementation which proxies calls to a {@code NamingStore} instance.
 * 
 * @author John E. Bailey
 */
public class NamingContext implements Context {
    /*
     * The active naming store to use for any context created without a name store.
     */
    private static NamingStore ACTIVE_NAMING_STORE = new InMemoryNamingStore();

    /**
     * Set the active naming store
     *
     * @param namingStore The naming store
     */
    public static void setActiveNamingStore(final NamingStore namingStore) {
        ACTIVE_NAMING_STORE = namingStore;
    }

    /* The name parser */
    private static final NameParser nameParser = new NameParser();

    /* The naming store providing the back-end storage */
    private final NamingStore namingStore;

    /* The name prefix the context represents. */
    private final Name prefix;

    /* The environment configuration */
    private final Hashtable<String, Object> environment;

    /**
     * Create a new naming context with no prefix or naming store.  This will default to a prefix of "" and
     * the active naming store.
     *
     * @param environment The naming environment
     */
    public NamingContext(final Hashtable<String, Object> environment) {
        this(new CompositeName(), ACTIVE_NAMING_STORE, environment);
    }

    /**
     * Create a context with a prefix name.
     *
     * @param prefix The prefix for this context
     * @param environment The naming environment
     */
    public NamingContext(final Name prefix, final Hashtable<String, Object> environment) {
        this(prefix, ACTIVE_NAMING_STORE, environment);
    }

    /**
     * Create a new naming context with a prefix name and a NamingStore instance to use as a backing.
     *
     * @param prefix The prefix for this context
     * @param namingStore The NamingStore
     * @param environment The naming environment
     */
    public NamingContext(final Name prefix, final NamingStore namingStore, final Hashtable<String, Object> environment) {
        if(prefix == null) {
            throw new IllegalArgumentException("Naming prefix can not be null");
        }
        this.prefix = prefix;
        if(namingStore == null) {
            throw new IllegalArgumentException("NamingStore can not be null");
        }
        this.namingStore = namingStore;
        this.environment = environment;
    }

    /** {@inheritDoc} */
    public Object lookup(final Name name) throws NamingException {
        if (name.isEmpty())
            return new NamingContext(prefix, namingStore, environment);

        final Name absoluteName = getAbsoluteName(name);
        Object result = null;
        try {
            result = namingStore.lookup(absoluteName);
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            result = continuationContext.lookup(cpe.getRemainingName());
        }

        if (result instanceof ResolveResult) {
            final ResolveResult resolveResult = ResolveResult.class.cast(result);
            final Object resolvedObject = resolveResult.getResolvedObj();

            Object context;
            if (resolvedObject instanceof LinkRef) {
                context = resolveLink(resolvedObject);
            } else {
                context = getObjectInstance(resolvedObject, absoluteName, environment);
            }
            if ((context instanceof Context) == false) {
                throw new NotContextException(context + " is not a Context");
            }
            final Context namingContext = Context.class.cast(context);
            return namingContext.lookup(resolveResult.getRemainingName());
        } else if (result instanceof LinkRef) {
            result = resolveLink(result);
        } else if (result instanceof Reference) {
            result = getObjectInstance(result, absoluteName, environment);
            if (result instanceof LinkRef) {
                result = resolveLink(result);
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    public Object lookup(final String name) throws NamingException {
        return lookup(parseName(name));
    }

    /** {@inheritDoc} */
    public void bind(final Name name, Object object) throws NamingException {
        final Name absoluteName = getAbsoluteName(name);

        object = NamingManager.getStateToBind(object, absoluteName, this, environment);

        if(object instanceof Referenceable) {
            object = Referenceable.class.cast(object).getReference();
        }
        String className = object.getClass().getName();
        if(object instanceof Reference) {
            className = Reference.class.cast(object).getClassName();
        }
        try {
            namingStore.bind(absoluteName, object, className);
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            continuationContext.bind(cpe.getRemainingName(), object);
        }
    }

    /** {@inheritDoc} */
    public void bind(final String name, final Object obj) throws NamingException {
        bind(parseName(name), obj);
    }

    /** {@inheritDoc} */
    public void rebind(final Name name, Object object) throws NamingException {
        final Name absoluteName = getAbsoluteName(name);
        object = NamingManager.getStateToBind(object, absoluteName, this, environment);

        if(object instanceof Referenceable) {
            object = Referenceable.class.cast(object).getReference();
        }
        String className = object.getClass().getName();
        if(object instanceof Reference) {
            className = Reference.class.cast(object).getClassName();
        }
        try {
            namingStore.rebind(absoluteName, object, className);
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            continuationContext.rebind(cpe.getRemainingName(), object);
        }
    }

    /** {@inheritDoc} */
    public void rebind(final String name, final Object obj) throws NamingException {
        rebind(parseName(name), obj);
    }

    /** {@inheritDoc} */
    public void unbind(final Name name) throws NamingException {
        try {
            namingStore.unbind(getAbsoluteName(name));
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            continuationContext.unbind(cpe.getRemainingName());
        }
    }

    /** {@inheritDoc} */
    public void unbind(final String name) throws NamingException {
        unbind(parseName(name));
    }

    /** {@inheritDoc} */
    public void rename(final Name oldName, final Name newName) throws NamingException {
        bind(newName, lookup(oldName));
        unbind(oldName);

    }

    /** {@inheritDoc} */
    public void rename(final String oldName, final String newName) throws NamingException {
        rename(parseName(oldName), parseName(newName));
    }

    /** {@inheritDoc} */
    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        try {
            return new NamingEnumerationImpl<NameClassPair>(namingStore.list(getAbsoluteName(name)));
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            return continuationContext.list(cpe.getRemainingName());
        }
    }

    /** {@inheritDoc} */
    public NamingEnumeration<NameClassPair> list(final String name) throws NamingException {
        return list(parseName(name));
    }

    /** {@inheritDoc} */
    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        try {
            return new NamingEnumerationImpl<Binding>(namingStore.listBindings(getAbsoluteName(name)));
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            return continuationContext.listBindings(cpe.getRemainingName());
        }
    }

    /** {@inheritDoc} */
    public NamingEnumeration<Binding> listBindings(final String name) throws NamingException {
        return listBindings(parseName(name));
    }

    /** {@inheritDoc} */
    public void destroySubcontext(final Name name) throws NamingException {
        final Name absoluteName = getAbsoluteName(name);
        if (!list(name).hasMore()) {
            unbind(name);
        }
        else
            throw new ContextNotEmptyException(absoluteName.toString());
    }

    /** {@inheritDoc} */
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(parseName(name));
    }

    /** {@inheritDoc} */
    public Context createSubcontext(Name name) throws NamingException {
        try {
            return namingStore.createSubcontext(getAbsoluteName(name));
        } catch(CannotProceedException cpe) {
            final Context continuationContext = NamingManager.getContinuationContext(cpe);
            return continuationContext.createSubcontext(cpe.getRemainingName());
        }
    }

    /** {@inheritDoc} */
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(getNameParser(name).parse(name));
    }

    /** {@inheritDoc} */
    public Object lookupLink(Name name) throws NamingException {
        if (name.isEmpty())
            return lookup(name);
        try {
            final Name absoluteName = getAbsoluteName(name);
            Object link = namingStore.lookup(absoluteName);
            if (!(link instanceof LinkRef) && link instanceof Reference)
                link = getObjectInstance(link, name, null);
            return link;
        }
        catch (Exception e) {
            NamingException ex = new NamingException("Could not lookup link");
            ex.setRemainingName(name);
            ex.setRootCause(e);
            throw ex;
        }
    }

    /** {@inheritDoc} */
    public Object lookupLink(String name) throws NamingException {
        return lookup(parseName(name));
    }

    /** {@inheritDoc} */
    public NameParser getNameParser(Name name) throws NamingException {
        return nameParser;
    }

    /** {@inheritDoc} */
    public NameParser getNameParser(String name) throws NamingException {
        return nameParser;
    }

    /** {@inheritDoc} */
    public Name composeName(Name name, Name prefix) throws NamingException {
        final Name result = (Name) (prefix.clone());
        result.addAll(name);
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
        namingStore.close();
    }

    /** {@inheritDoc} */
    public String getNameInNamespace() throws NamingException {
        return prefix.toString();
    }

    private Name parseName(final String name) throws NamingException {
        return getNameParser(name).parse(name);
    }

    private Name getAbsoluteName(final Name name) throws NamingException {
        if(name.isEmpty()) {
            return composeName(name, prefix);
        } else if("".equals(name.get(0)) || "java:".equals(name.get(0))) {
            return name.getSuffix(1);
        }
        else
            return composeName(name, prefix);
    }

    private Object getObjectInstance(final Object object, final Name name, final Hashtable environment) throws NamingException {
        try {
            return NamingManager.getObjectInstance(object, name, this, environment);
        } catch(NamingException e) {
            throw e;
        } catch(Exception e) {
            final NamingException namingException = new NamingException("Could not dereference object");
            namingException.setRootCause(e);
            throw namingException;
        }
    }

    private Object resolveLink(Object result) throws NamingException {
        final Object linkResult;
        try {
            final LinkRef linkRef = LinkRef.class.cast(result);
            String referenceName = linkRef.getLinkName();
            if (referenceName.startsWith("./"))
                linkResult = lookup(referenceName.substring(2));
            else
                linkResult = new InitialContext().lookup(referenceName);
        }
        catch (Exception e) {
            NamingException ex = new NamingException("Could not dereference object");
            ex.setRootCause(e);
            throw ex;
        }
        return linkResult;
    }
}