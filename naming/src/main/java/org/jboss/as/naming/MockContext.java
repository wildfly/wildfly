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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import static org.jboss.as.naming.util.NamingUtils.nameNotFoundException;
import org.jboss.as.naming.util.ThreadLocalStack;

/**
 * {@link Context} impl used to support legacy integrations that expect a writable context.  Capable of trapping all
 * the bindings created by the integrations for proper service based naming.
 *
 * @author John Bailey
 */
public class MockContext implements Context {

    private final NamingContext readContext;
    private final Name prefix;

    public MockContext(final NamingContext readContext, final Name prefix) {
        this.readContext = readContext;
        this.prefix = prefix;
    }

    /* Write methods */

    public void bind(final Name name, final Object value) throws NamingException {
        final String bindName = getAbsoluteName(name).toString();
        final BindingTrap trap = bindingTraps.peek();
        if (trap != null) {
            trap.bindings.put(bindName, value);
        } else {
            throw new IllegalStateException("Nothing available to bind to.");
        }
    }

    public void bind(final String name, final Object value) throws NamingException {
        bind(parseName(name), value);
    }

    public void rebind(final Name name, final Object value) throws NamingException {
        bind(name, value);
    }

    public void rebind(final String name, final Object value) throws NamingException {
        rebind(parseName(name), value);
    }

    public void unbind(final Name name) throws NamingException {
        // No-op
    }

    public void unbind(final String name) throws NamingException {
        unbind(parseName(name));
    }

    public void rename(final Name oldName, final Name newName) throws NamingException {
        final String absolute = getAbsoluteName(oldName).toString();
        final String newAbsolute = getAbsoluteName(newName).toString();

        final BindingTrap trap = bindingTraps.peek();
        if (trap != null) {
            final Object value = trap.bindings.remove(absolute);
            if(value == null) {
                throw nameNotFoundException(oldName.toString(), prefix);
            }
            trap.bindings.put(newAbsolute, value);
        } else {
            throw new IllegalStateException("Nothing available to bind to.");
        }
    }

    public void rename(final String s, final String s1) throws NamingException {
        rebind(parseName(s), parseName(s1));
    }

    public void destroySubcontext(final Name name) throws NamingException {
        // No-op
    }

    public void destroySubcontext(final String s) throws NamingException {
        // No-op
    }

    public Context createSubcontext(final Name name) throws NamingException {
        final Name prefix = composeName(name, this.prefix);
        final NamingContext context = new NamingContext(prefix, readContext.getNamingStore(), null);
        return new MockContext(context, prefix);
    }

    public Context createSubcontext(final String name) throws NamingException {
        return createSubcontext(parseName(name));
    }

    /* Delegating methods */

    public Object lookup(final Name name) throws NamingException {
        final Object value = readContext.lookup(name);
        if (value instanceof NamingContext) {
            final NamingContext context = NamingContext.class.cast(value);
            final Name prefix = context.getPrefix();
            return new MockContext(context, prefix);
        }
        return value;
    }

    public Object lookup(final String name) throws NamingException {
        return lookup(parseName(name));
    }

    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        return readContext.list(name);
    }

    public NamingEnumeration<NameClassPair> list(final String s) throws NamingException {
        return readContext.list(s);
    }

    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        return readContext.listBindings(name);
    }

    public NamingEnumeration<Binding> listBindings(final String s) throws NamingException {
        return readContext.listBindings(s);
    }

    public Object lookupLink(final Name name) throws NamingException {
        return readContext.lookupLink(name);
    }

    public Object lookupLink(final String s) throws NamingException {
        return readContext.lookupLink(s);
    }

    public NameParser getNameParser(final Name name) throws NamingException {
        return readContext.getNameParser(name);
    }

    public NameParser getNameParser(final String name) throws NamingException {
        return readContext.getNameParser(name);
    }

    public Name composeName(final Name name, final Name name1) throws NamingException {
        return readContext.composeName(name, name1);
    }

    public String composeName(final String s, final String s1) throws NamingException {
        return readContext.composeName(s, s1);
    }

    public Object addToEnvironment(final String s, final Object o) throws NamingException {
        return null;
    }

    public Object removeFromEnvironment(final String s) throws NamingException {
        return null;
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return readContext.getEnvironment();
    }

    public void close() throws NamingException {
    }

    public String getNameInNamespace() throws NamingException {
        return prefix.toString();
    }

    /* Helper methods */

    protected Name parseName(final String name) throws NamingException {
        return getNameParser(name).parse(name);
    }

    protected Name getAbsoluteName(final Name name) throws NamingException {
        if (name.isEmpty()) {
            return composeName(name, prefix);
        }
        final String firstComponent = name.get(0);
        if (firstComponent.startsWith("java:")) {
            final String cleaned = firstComponent.substring(5);
            final Name suffix = name.getSuffix(1);
            if (cleaned.isEmpty()) {
                return suffix;
            }
            return suffix.add(0, cleaned);
        } else if (firstComponent.isEmpty()) {
            return name.getSuffix(1);
        } else {
            return composeName(name, prefix);
        }
    }

    public static ThreadLocalStack<BindingTrap> bindingTraps = new ThreadLocalStack<BindingTrap>();

    public static void pushBindingTrap() {
        final BindingTrap trap = new BindingTrap();
        bindingTraps.push(trap);
    }

    public static Map<String, Object> popTrappedBindings() {
        final BindingTrap trap = bindingTraps.pop();
        if (trap != null) {
            return trap.bindings;
        }
        return null;
    }

    public static class BindingTrap {
        private final Map<String, Object> bindings = new HashMap<String, Object>();
    }
}
