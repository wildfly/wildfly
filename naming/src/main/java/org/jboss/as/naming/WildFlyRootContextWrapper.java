/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import org.wildfly.naming.client.WildFlyRootContext;
import org.wildfly.naming.client.util.FastHashtable;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

class WildFlyRootContextWrapper implements DirContext {

    private static final NameParser NAME_PARSER = CompositeName::new;

    private final WildFlyRootContext delegate;

    private final List<LookupInterceptor> interceptors;

    public WildFlyRootContextWrapper(final Hashtable<?, ?> environment, List<LookupInterceptor> interceptors) throws NamingException {
        this.delegate = new WildFlyRootContext(new FastHashtable<>((Map<String, Object>) (Map) environment));
        this.interceptors = interceptors;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        aroundLookup(name);
        return delegate.lookup(name);
    }

    @Override
    public Object lookup(String name) throws NamingException {
        aroundLookup(NAME_PARSER.parse(name));
        return delegate.lookup(name);
    }


    @Override
    public Attributes getAttributes(Name name) throws NamingException {
        return delegate.getAttributes(name);
    }

    @Override
    public Attributes getAttributes(String nameString) throws NamingException {
        return delegate.getAttributes(nameString);
    }

    @Override
    public Attributes getAttributes(Name name, String[] strings) throws NamingException {
        return delegate.getAttributes(name, strings);
    }

    @Override
    public Attributes getAttributes(String nameString, String[] strings) throws NamingException {
        return delegate.getAttributes(nameString, strings);
    }

    @Override
    public void modifyAttributes(Name name, int i, Attributes attributes) throws NamingException {
        delegate.modifyAttributes(name, i, attributes);
    }

    @Override
    public void modifyAttributes(String nameString, int i, Attributes attributes) throws NamingException {
        delegate.modifyAttributes(nameString, i, attributes);
    }

    @Override
    public void modifyAttributes(Name name, ModificationItem[] modificationItems) throws NamingException {
        delegate.modifyAttributes(name, modificationItems);
    }

    @Override
    public void modifyAttributes(String nameString, ModificationItem[] modificationItems) throws NamingException {
        delegate.modifyAttributes(nameString, modificationItems);
    }

    @Override
    public void bind(Name name, Object object, Attributes attributes) throws NamingException {
        delegate.bind(name, object, attributes);
    }

    @Override
    public void bind(String nameString, Object object, Attributes attributes) throws NamingException {
        delegate.bind(nameString, object, attributes);
    }

    @Override
    public void rebind(Name name, Object object, Attributes attributes) throws NamingException {
        delegate.rebind(name, object, attributes);
    }

    @Override
    public void rebind(String nameString, Object object, Attributes attributes) throws NamingException {
        delegate.rebind(nameString, object, attributes);
    }

    @Override
    public DirContext createSubcontext(Name name, Attributes attributes) throws NamingException {
        return delegate.createSubcontext(name, attributes);
    }

    @Override
    public DirContext createSubcontext(String nameString, Attributes attributes) throws NamingException {
        return delegate.createSubcontext(nameString, attributes);
    }

    @Override
    public DirContext getSchema(Name name) throws NamingException {
        return delegate.getSchema(name);
    }

    @Override
    public DirContext getSchema(String nameString) throws NamingException {
        return delegate.getSchema(nameString);
    }

    @Override
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return delegate.getSchemaClassDefinition(name);
    }

    @Override
    public DirContext getSchemaClassDefinition(String nameString) throws NamingException {
        return delegate.getSchemaClassDefinition(nameString);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes attributes, String[] strings) throws NamingException {
        return delegate.search(name, attributes, strings);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String nameString, Attributes attributes, String[] strings) throws NamingException {
        return delegate.search(nameString, attributes, strings);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes attributes) throws NamingException {
        return delegate.search(name, attributes);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String nameString, Attributes attributes) throws NamingException {
        return delegate.search(nameString, attributes);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String s, SearchControls searchControls) throws NamingException {
        return delegate.search(name, s, searchControls);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String nameString, String s, SearchControls searchControls) throws NamingException {
        return delegate.search(nameString, s, searchControls);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String s, Object[] objects, SearchControls searchControls) throws NamingException {
        return delegate.search(name, s, objects, searchControls);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String nameString, String s, Object[] objects, SearchControls searchControls) throws NamingException {
        return delegate.search(nameString, s, objects, searchControls);
    }

    @Override
    public void bind(Name name, Object o) throws NamingException {
        delegate.bind(name, o);
    }

    @Override
    public void bind(String nameString, Object o) throws NamingException {
        delegate.bind(nameString, o);
    }

    @Override
    public void rebind(Name name, Object o) throws NamingException {
        delegate.rebind(name, o);
    }

    @Override
    public void rebind(String nameString, Object o) throws NamingException {
        delegate.rebind(nameString, o);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        delegate.unbind(name);
    }

    @Override
    public void unbind(String nameString) throws NamingException {
        delegate.unbind(nameString);
    }

    @Override
    public void rename(Name name, Name name1) throws NamingException {
        delegate.rename(name, name1);
    }

    @Override
    public void rename(String nameString, String name1) throws NamingException {
        delegate.rename(nameString, name1);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return delegate.list(name);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String nameString) throws NamingException {
        return delegate.list(nameString);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return delegate.listBindings(name);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String nameString) throws NamingException {
        return delegate.listBindings(nameString);
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        delegate.destroySubcontext(name);
    }

    @Override
    public void destroySubcontext(String nameString) throws NamingException {
        delegate.destroySubcontext(nameString);
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return delegate.createSubcontext(name);
    }

    @Override
    public Context createSubcontext(String nameString) throws NamingException {
        return delegate.createSubcontext(nameString);
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        aroundLookup(name);
        return delegate.lookupLink(name);
    }

    @Override
    public Object lookupLink(String nameString) throws NamingException {
        Name name = NAME_PARSER.parse(nameString);
        return lookupLink(name);
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return delegate.getNameParser(name);
    }

    @Override
    public NameParser getNameParser(String nameString) throws NamingException {
        return delegate.getNameParser(nameString);
    }

    @Override
    public Name composeName(Name name, Name name1) throws NamingException {
        return delegate.composeName(name, name1);
    }

    @Override
    public String composeName(String nameString, String name1) throws NamingException {
        return delegate.composeName(nameString, name1);
    }

    @Override
    public Object addToEnvironment(String nameString, Object object) throws NamingException {
        return delegate.addToEnvironment(nameString, object);
    }

    @Override
    public Object removeFromEnvironment(String nameString) throws NamingException {
        return delegate.removeFromEnvironment(nameString);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return delegate.getEnvironment();
    }

    @Override
    public void close() throws NamingException {
        delegate.close();
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return delegate.getNameInNamespace();
    }

    private final void aroundLookup(Name name) throws NamingException {
        for (LookupInterceptor interceptor : interceptors) {
            interceptor.aroundLookup(name);
        }
    }
}
