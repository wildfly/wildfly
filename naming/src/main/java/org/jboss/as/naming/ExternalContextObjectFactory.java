package org.jboss.as.naming;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An ObjectFactory that binds an arbitrary InitialContext into JNDI.
 */
public class ExternalContextObjectFactory implements ObjectFactory {

    private static final AtomicInteger PROXY_ID = new AtomicInteger();

    public static final String CACHE_CONTEXT = "cache-context";
    public static final String INITIAL_CONTEXT_CLASS = "initial-context-class";
    public static final String INITIAL_CONTEXT_MODULE = "initial-context-module";

    /**
     * If this property is set to {@code true} in the {@code Context} environment, objects will be looked up
     * by calling its {@link javax.naming.Context#lookup(String)} instead of {@link javax.naming.Context#lookup(javax.naming.Name)}.
     */
    private static final String LOOKUP_BY_STRING = "org.jboss.as.naming.lookup.by.string";


    private volatile Context cachedObject;

    @Override
    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        String cacheString = (String) environment.get(CACHE_CONTEXT);
        boolean cache = cacheString != null && cacheString.toLowerCase().equals("true");
        if (cache) {
            if (cachedObject == null) {
                synchronized (this) {
                    if (cachedObject == null) {
                        cachedObject = createContext(environment, true);
                    }
                }
            }
            return cachedObject;
        } else {
            return createContext(environment, false);
        }
    }

    private Context createContext(final Hashtable<?, ?> environment, boolean useProxy) throws NamingException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, ModuleLoadException {
        String initialContextClassName = (String) environment.get(INITIAL_CONTEXT_CLASS);
        String initialContextModule = (String) environment.get(INITIAL_CONTEXT_MODULE);
        final boolean useStringLokup = useStringLookup(environment);

        final Hashtable<?, ?> newEnvironment = new Hashtable<>(environment);
        newEnvironment.remove(CACHE_CONTEXT);
        newEnvironment.remove(INITIAL_CONTEXT_CLASS);
        newEnvironment.remove(INITIAL_CONTEXT_MODULE);
        newEnvironment.remove(LOOKUP_BY_STRING);

        ClassLoader loader;
        if (! WildFlySecurityManager.isChecking()) {
            loader = getClass().getClassLoader();
        } else {
            loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return getClass().getClassLoader();
                }
            });
        }
        Class initialContextClass = null;
        final Context loadedContext;
        if (initialContextModule == null) {
            initialContextClass = Class.forName(initialContextClassName);
            Constructor ctor = initialContextClass.getConstructor(Hashtable.class);
            loadedContext = (Context) ctor.newInstance(newEnvironment);
        } else {
            Module module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.fromString(initialContextModule));
            loader = module.getClassLoader();
            final ClassLoader currentClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
                initialContextClass = Class.forName(initialContextClassName, true, loader);
                Constructor ctor = initialContextClass.getConstructor(Hashtable.class);
                loadedContext = (Context) ctor.newInstance(newEnvironment);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(currentClassLoader);
            }
        }

        final Context context;
        if (useStringLokup) {
            context = new LookupByStringContext(loadedContext);
        } else {
            context = loadedContext;
        }

        if (!useProxy) {
            return context;
        }
        ProxyConfiguration config = new ProxyConfiguration();
        config.setClassLoader(loader);
        config.setSuperClass(initialContextClass);
        config.setProxyName(initialContextClassName + "$$$$Proxy" + PROXY_ID.incrementAndGet());
        config.setProtectionDomain(context.getClass().getProtectionDomain());
        ProxyFactory<?> factory = new ProxyFactory<Object>(config);
        return (Context) factory.newInstance(new CachedContext(context));
    }

    /**
     * A proxy implementation of Context that simply intercepts the
     * close() method and ignores it since the underlying Context
     * object is being maintained in memory.
     */
    static class CachedContext implements InvocationHandler {
        Context externalCtx;

        CachedContext(Context externalCtx) {
            this.externalCtx = externalCtx;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object value = null;
            if (method.getName().equals("close")) {
                // We just ignore the close method
            } else {
                try {
                    value = method.invoke(externalCtx, args);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }
            return value;
        }
    }

    /**
     * @return {@code true} if the environment contains a {@code LOOKUP_BY_STRING} property with a value corresponding to a {@code true} boolean, or {@code false} in any other case.
     * @param environment
     */
    private static boolean useStringLookup(Hashtable<?, ?> environment) {
        Object val = environment.get(LOOKUP_BY_STRING);
        if (val instanceof String) {
            return Boolean.valueOf((String) val);
        }
        return false;
    }

    /**
     * A wrapper around a {@code Context} that delegates {@link javax.naming.Name}-based lookup to
     * their corresponding {@code String}-based method.
     */
    private static class LookupByStringContext implements Context {
        private final Context context;

        LookupByStringContext(Context context) {
            this.context = context;
        }

        @Override
        public Object lookup(Name name) throws NamingException {
            return context.lookup(name.toString());
        }

        @Override
        public Object lookup(String name) throws NamingException {
            return context.lookup(name);
        }

        @Override
        public void bind(Name name, Object obj) throws NamingException {
            context.bind(name, obj);

        }

        @Override
        public void bind(String name, Object obj) throws NamingException {
            context.bind(name, obj);
        }

        @Override
        public void rebind(Name name, Object obj) throws NamingException {
            context.rebind(name, obj);
        }

        @Override
        public void rebind(String name, Object obj) throws NamingException {
            context.rebind(name, obj);
        }

        @Override
        public void unbind(Name name) throws NamingException {
            context.unbind(name);
        }

        @Override
        public void unbind(String name) throws NamingException {
            context.unbind(name);
        }

        @Override
        public void rename(Name oldName, Name newName) throws NamingException {
            context.rename(oldName, newName);
        }

        @Override
        public void rename(String oldName, String newName) throws NamingException {
            context.rename(oldName, newName);
        }

        @Override
        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            return context.list(name);
        }

        @Override
        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            return context.list(name);
        }

        @Override
        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            return context.listBindings(name);
        }

        @Override
        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
            return context.listBindings(name);
        }

        @Override
        public void destroySubcontext(Name name) throws NamingException {
            context.destroySubcontext(name);
        }

        @Override
        public void destroySubcontext(String name) throws NamingException {
            context.destroySubcontext(name);
        }

        @Override
        public Context createSubcontext(Name name) throws NamingException {
            return context.createSubcontext(name);
        }

        @Override
        public Context createSubcontext(String name) throws NamingException {
            return context.createSubcontext(name);
        }

        @Override
        public Object lookupLink(Name name) throws NamingException {
            return context.lookupLink(name.toString());
        }

        @Override
        public Object lookupLink(String name) throws NamingException {
            return context.lookupLink(name);
        }

        @Override
        public NameParser getNameParser(Name name) throws NamingException {
            return context.getNameParser(name.toString());
        }

        @Override
        public NameParser getNameParser(String name) throws NamingException {
            return context.getNameParser(name);
        }

        @Override
        public Name composeName(Name name, Name prefix) throws NamingException {
            return context.composeName(name, prefix);
        }

        @Override
        public String composeName(String name, String prefix) throws NamingException {
            return context.composeName(name, prefix);
        }

        @Override
        public Object addToEnvironment(String propName, Object propVal) throws NamingException {
            return context.addToEnvironment(propName, propVal);
        }

        @Override
        public Object removeFromEnvironment(String propName) throws NamingException {
            return context.removeFromEnvironment(propName);
        }

        @Override
        public Hashtable<?, ?> getEnvironment() throws NamingException {
            return context.getEnvironment();
        }

        @Override
        public void close() throws NamingException {
            context.close();
        }

        @Override
        public String getNameInNamespace() throws NamingException {
            return context.getNameInNamespace();
        }
    }
}
