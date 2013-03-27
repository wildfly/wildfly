package org.jboss.as.naming;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * An ObjectFactory that binds an arbitrary InitialContext into JNDI.
 */
public class ExternalContextObjectFactory implements ObjectFactory {

    private static final AtomicInteger PROXY_ID = new AtomicInteger();

    public static final String CACHE_CONTEXT = "cache-context";
    public static final String INITIAL_CONTEXT_CLASS = "initial-context-class";
    public static final String INITIAL_CONTEXT_MODULE = "initial-context-module";


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
        final Hashtable<?, ?> newEnvironment = new Hashtable<>(environment);
        newEnvironment.remove(CACHE_CONTEXT);
        newEnvironment.remove(INITIAL_CONTEXT_CLASS);
        newEnvironment.remove(INITIAL_CONTEXT_MODULE);


        ClassLoader loader;
        if(System.getSecurityManager() == null) {
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
        final Context context;
        if (initialContextModule == null) {
            initialContextClass = Class.forName(initialContextClassName);
            Constructor ctor = initialContextClass.getConstructor(Hashtable.class);
            context = (Context) ctor.newInstance(newEnvironment);
        } else {
            Module module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.fromString(initialContextModule));
            loader = module.getClassLoader();
            initialContextClass = Class.forName(initialContextClassName, true, module.getClassLoader());
            Constructor ctor = initialContextClass.getConstructor(Hashtable.class);
            context = (Context) ctor.newInstance(newEnvironment);
        }

        if (!useProxy) {
            return context;
        }
        ProxyConfiguration config = new ProxyConfiguration();
        config.setClassLoader(loader);
        config.setSuperClass(initialContextClass);
        config.setProxyName(initialContextClassName + "$$$$Proxy" + PROXY_ID.incrementAndGet());
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
}
