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

package org.jboss.as.deployment.test;

import javax.naming.Binding;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * Mock JNDI context.
 *
 * @author John E. Bailey
 */
public class MockContext implements Context {
    private static final Map<Name, Object> entries = new HashMap<Name, Object>();

    private static final Properties parseSyntax = new Properties();

    static {
        parseSyntax.setProperty("jndi.syntax.direction", "left_to_right");
        parseSyntax.setProperty("jndi.syntax.ignorecase", "false");
        parseSyntax.setProperty("jndi.syntax.separator", "/");
    }

    private static final NameParser nameParser = new NameParser() {
        public Name parse(String name) throws NamingException {
            return new CompoundName(name, parseSyntax);
        }
    };

    private final Name prefix;

    public MockContext() throws NamingException {
        this(nameParser.parse(""));
    }

    public MockContext(Name prefix) {
        this.prefix = prefix;
    }

    public MockContext(String prefix) throws NamingException {
        this(nameParser.parse(prefix));
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty())
            return new MockContext(prefix);

        final Name absoluteName = getAbsoluteName(name);
        final Object result = entries.get(absoluteName);
        if(result == null)
            throw new NameNotFoundException(absoluteName.toString());
        return result;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        return lookup(parseName(name));
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        final Name absoluteName = getAbsoluteName(name);
        entries.put(absoluteName, obj);
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        bind(parseName(name), obj);
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        bind(name, obj);
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        bind(name, obj);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        final Name absoluteName = getAbsoluteName(name);
        entries.remove(absoluteName);
    }

    @Override
    public void unbind(String name) throws NamingException {
        unbind(parseName(name));
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        final Name absoluteOld = getAbsoluteName(oldName);
        final Name absoluteNew = getAbsoluteName(newName);
        final Object existing = entries.remove(absoluteOld);
        entries.put(absoluteNew, existing);
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        rename(parseName(oldName), parseName(newName));
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return null;
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        final Name absoluteName = getAbsoluteName(name);
        entries.remove(absoluteName);
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(parseName(name));
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        final Name absoluteName = getAbsoluteName(name);
        final Context context = new MockContext(absoluteName);
        entries.put(absoluteName, context);
        return context;
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(getNameParser(name).parse(name));
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        return null;
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return null;
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return nameParser;
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return nameParser;
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        final Name result = (Name) (prefix.clone());
        result.addAll(name);
        return result;
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return composeName(parseName(name), parseName(prefix)).toString();
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return null;
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return null;
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return null;
    }

    @Override
    public void close() throws NamingException {
        entries.clear();
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return null;
    }

    private Name parseName(final String name) throws NamingException {
        return getNameParser(name).parse(name);
    }

    private Name getAbsoluteName(Name name) throws NamingException {
        if (name.isEmpty())
            return composeName(name, prefix);
        else if (name.get(0).toString().equals(""))
            return name.getSuffix(1);
        else 
            return composeName(name, prefix);
    }
}
