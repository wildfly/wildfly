package org.jboss.as.ejb3.remote;

import java.io.IOException;
import java.lang.reflect.Proxy;

import org.jboss.marshalling.cloner.ClassCloner;

/**
 * {@link ClassCloner} that clones classes between class loaders, falling back
 * to original class if it cannot be found in the destination class loader.
 *
 * @author Stuart Douglas
 */
public class LocalInvocationClassCloner implements ClassCloner {

    private final ClassLoader destClassLoader;

    public LocalInvocationClassCloner(final ClassLoader destClassLoader) {
        this.destClassLoader = destClassLoader;
    }

    public Class<?> clone(final Class<?> original) throws IOException, ClassNotFoundException {
        final String name = original.getName();
        if (name.startsWith("java.")) {
            return original;
        } else if (original.getClassLoader() == destClassLoader) {
            return original;
        } else {
            try {
                return Class.forName(name, true, destClassLoader);
            } catch (ClassNotFoundException e) {
                return original;
            }
        }
    }

    public Class<?> cloneProxy(final Class<?> proxyClass) throws IOException, ClassNotFoundException {
        final Class<?>[] origInterfaces = proxyClass.getInterfaces();
        final Class<?>[] interfaces = new Class[origInterfaces.length];
        for (int i = 0, origInterfacesLength = origInterfaces.length; i < origInterfacesLength; i++) {
            interfaces[i] = clone(origInterfaces[i]);
        }
        return Proxy.getProxyClass(destClassLoader, interfaces);
    }
}
