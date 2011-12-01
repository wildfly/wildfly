package org.jboss.as.jacorb.naming.jndi;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * @author Stuart Douglas
 */
public class WrapperInitialContext implements Context {

    private final Hashtable environment;

    public WrapperInitialContext(final Hashtable environment) {
        if (environment != null) {
            this.environment = (Hashtable) environment.clone();
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
                final Hashtable environment = (Hashtable) this.environment.clone();
                environment.put(Context.PROVIDER_URL, server);
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
