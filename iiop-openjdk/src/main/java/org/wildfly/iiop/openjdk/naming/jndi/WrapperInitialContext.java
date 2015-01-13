/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk.naming.jndi;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import com.sun.corba.se.impl.orbutil.ORBConstants;

/**
 * @author Stuart Douglas
 */
public class WrapperInitialContext implements Context {

    private final Hashtable<Object,Object> environment;

    @SuppressWarnings("unchecked")
    public WrapperInitialContext(final Hashtable<Object,Object> environment) {
        if (environment != null) {
            this.environment = (Hashtable<Object,Object>) environment.clone();
        } else {
            this.environment = null;
        }
    }

    @Override
    public Object lookup(final Name name) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(final String name) throws NamingException {
        try {
            final int index = name.indexOf('#');
            if (index != -1) {
                final String server = name.substring(0, index);
                final String lookup = name.substring(index + 1);
                @SuppressWarnings("unchecked")
                final Hashtable<Object,Object> environment = (Hashtable<Object,Object>) this.environment.clone();

                environment.put(Context.PROVIDER_URL, server);
                environment.put(ORBConstants.ORB_SERVER_ID_PROPERTY, "1");


                return CNCtxFactory.INSTANCE.getInitialContext(environment).lookup(lookup);
            } else {
                return CNCtxFactory.INSTANCE.getInitialContext(environment).lookup(name);
            }
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            throw new NamingException(e.getMessage());
        }
    }

    @Override
    public void bind(final Name name, final Object obj) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).bind(name, obj);
    }

    @Override
    public void bind(final String name, final Object obj) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).bind(name, obj);
    }

    @Override
    public void rebind(final Name name, final Object obj) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).rebind(name, obj);
    }

    @Override
    public void rebind(final String name, final Object obj) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).rebind(name, obj);
    }

    @Override
    public void unbind(final Name name) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).unbind(name);
    }

    @Override
    public void unbind(final String name) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).unbind(name);
    }

    @Override
    public void rename(final Name oldName, final Name newName) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).rename(oldName, newName);
    }

    @Override
    public void rename(final String oldName, final String newName) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).rename(oldName, newName);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).list(name);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(final String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).listBindings(name);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(final String name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).listBindings(name);
    }

    @Override
    public void destroySubcontext(final Name name) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).destroySubcontext(name);
    }

    @Override
    public void destroySubcontext(final String name) throws NamingException {
        CNCtxFactory.INSTANCE.getInitialContext(environment).destroySubcontext(name);
    }

    @Override
    public Context createSubcontext(final Name name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).createSubcontext(name);
    }

    @Override
    public Context createSubcontext(final String name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).createSubcontext(name);
    }

    @Override
    public Object lookupLink(final Name name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).lookupLink(name);
    }

    @Override
    public Object lookupLink(final String name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).lookupLink(name);
    }

    @Override
    public NameParser getNameParser(final Name name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).getNameParser(name);
    }

    @Override
    public NameParser getNameParser(final String name) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).getNameParser(name);
    }

    @Override
    public Name composeName(final Name name, final Name prefix) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).composeName(name, prefix);
    }

    @Override
    public String composeName(final String name, final String prefix) throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).composeName(name, prefix);
    }

    @Override
    public Object addToEnvironment(final String propName, final Object propVal) throws NamingException {
        return environment.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(final String propName) throws NamingException {
        return environment.remove(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return environment;
    }

    @Override
    public void close() throws NamingException {

    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return CNCtxFactory.INSTANCE.getInitialContext(environment).getNameInNamespace();
    }
}
