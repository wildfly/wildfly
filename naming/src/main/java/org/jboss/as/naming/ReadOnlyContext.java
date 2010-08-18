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

import org.jboss.as.naming.util.NamingUtils;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * A context implementation that is read only.  No operations that change the state of the naming directory are
 * supported.
 *
 * @author John E. Bailey
 */
public class ReadOnlyContext implements Context {
    private final Context realContext;

    private final Hashtable<String, Object> environment;

    public ReadOnlyContext(Context realContext) throws NamingException {
        this.realContext = realContext;
        this.environment = (Hashtable<String, Object>) NamingUtils.clone(realContext.getEnvironment());
    }

    /** {@inheritDoc} */
    public Object lookup(Name name) throws NamingException {
        return realContext.lookup(name);
    }

    /** {@inheritDoc} */
    public Object lookup(String name) throws NamingException {
        return realContext.lookup(name);
    }

    /** Not supported */
    public void bind(Name name, Object obj) throws NamingException {
        throw new UnsupportedOperationException("Binding is not supported in a read-only context");
    }

    /** Not supported */
    public void bind(String name, Object obj) throws NamingException {
        throw new UnsupportedOperationException("Binding is not supported in a read-only context");
    }

    /** Not supported */
    public void rebind(Name name, Object obj) throws NamingException {
        throw new UnsupportedOperationException("Rebinding is not supported in a read-only context");
    }

    /** Not supported */
    public void rebind(String name, Object obj) throws NamingException {
        throw new UnsupportedOperationException("Rebinding is not supported in a read-only context");
    }

    /** Not supported */
    public void unbind(Name name) throws NamingException {
        throw new UnsupportedOperationException("Unbinding is not supported in a read-only context");
    }

    /** Not supported */
    public void unbind(String name) throws NamingException {
        throw new UnsupportedOperationException("Unbinding is not supported in a read-only context");
    }

    /** Not supported */
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new UnsupportedOperationException("Renaming is not supported in a read-only context");
    }

    /** Not supported */
    public void rename(String oldName, String newName) throws NamingException {
        throw new UnsupportedOperationException("Renaming is not supported in a read-only context");
    }

    /** {@inheritDoc} */
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return realContext.list(name);
    }

    /** {@inheritDoc} */
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return realContext.list(name);
    }

    /** {@inheritDoc} */
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return realContext.listBindings(name);
    }

    /** {@inheritDoc} */
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return realContext.listBindings(name);
    }

    /** Not supported */
    public void destroySubcontext(Name name) throws NamingException {
        throw new UnsupportedOperationException("Destroying sub-contexts is not supported in a read-only context");
    }

    /** Not supported */
    public void destroySubcontext(String name) throws NamingException {
        throw new UnsupportedOperationException("Destroying sub-contexts is not supported in a read-only context");
    }

    /** Not supported */
    public Context createSubcontext(Name name) throws NamingException {
        throw new UnsupportedOperationException("Creating sub-contexts is not supported in a read-only context");
    }

    /** Not supported */
    public Context createSubcontext(String name) throws NamingException {
        throw new UnsupportedOperationException("Creating sub-contexts is not supported in a read-only context");
    }

    /** {@inheritDoc} */
    public Object lookupLink(Name name) throws NamingException {
        return realContext.lookupLink(name);
    }

    /** {@inheritDoc} */
    public Object lookupLink(String name) throws NamingException {
        return realContext.lookupLink(name);
    }

    /** {@inheritDoc} */
    public NameParser getNameParser(Name name) throws NamingException {
        return realContext.getNameParser(name);
    }

    /** {@inheritDoc} */
    public NameParser getNameParser(String name) throws NamingException {
        return realContext.getNameParser(name);
    }

    /** {@inheritDoc} */
    public Name composeName(Name name, Name prefix) throws NamingException {
        return realContext.composeName(name, prefix);
    }

    /** {@inheritDoc} */
    public String composeName(String name, String prefix) throws NamingException {
        return composeName(name, prefix);
    }

    /** Not supported */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        throw new UnsupportedOperationException("Adding environment entries is not allowed in a read-only context");
    }

    /** Not supported */
    public Object removeFromEnvironment(String propName) throws NamingException {
        throw new UnsupportedOperationException("Removing environment entries is not allowed in a read-only context");
    }

    /** {@inheritDoc} */
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return environment;
    }

    /** Not supported */
    public void close() throws NamingException {
        throw new UnsupportedOperationException("Closing is not allowed in a read-only context");
    }

    /** {@inheritDoc} */
    public String getNameInNamespace() throws NamingException {
        return realContext.getNameInNamespace();
    }
}
