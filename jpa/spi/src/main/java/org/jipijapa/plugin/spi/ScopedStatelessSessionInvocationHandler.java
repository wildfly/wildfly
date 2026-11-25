/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * InvocationHandler for proxies implementing StatelessSession that delegates calls to another StatelessSession.
 * Which session is delegated to depends on any active transaction, and if there is no
 * active transaction, on the thread. The sessions that are delegated to are created
 * by a provided supplier. Closing them is controlled by the associated transaction, or
 * for instances not associated with transactions, by the {@code NoTxEmCloser} ThreadLocal-based
 * utility.
 */
public final class ScopedStatelessSessionInvocationHandler implements InvocationHandler, Serializable {

    @Serial
    private static final long serialVersionUID = 455498112L;

    private static final Method EQUALS = getObjectMethod("equals", Object.class);
    private static final Method HASH_CODE = getObjectMethod("hashCode");
    private static final Method TO_STRING = getObjectMethod("toString");

    private static Method getObjectMethod(String name, Class<?>... params) {
        try {
            return Object.class.getDeclaredMethod(name, params);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a proxy implementing the given {@code statelessSessionClass} that delegates to
     * implementations supplied by the given {@code delegateSupplier}.
     *
     * @param statelessSessionClass the Hibernate StatelessSession class as available from {@code classLoader}
     * @param classLoader the classloader to use for the proxy
     * @param delegateSupplier supplier that provides an appropriate StatelessSession instance based on the current context
     * @return a proxy implementing the Hibernate StatelessSession class
     * @param <T> the Hibernate StatelessSession class as available from {@code classLoader}
     */
    public static <T> T createStatelessSessionProxy(Class<T> statelessSessionClass, ClassLoader classLoader,
                                                  ScopedStatelessSessionSupplier delegateSupplier) {
        return statelessSessionClass.cast(
                Proxy.newProxyInstance(classLoader,
                    new Class[]{statelessSessionClass},
                    new ScopedStatelessSessionInvocationHandler(delegateSupplier, statelessSessionClass))
        );
    }

    private final ScopedStatelessSessionSupplier delegateSupplier;
    private final Class<?> statelessSessionClass;

    private ScopedStatelessSessionInvocationHandler(ScopedStatelessSessionSupplier delegateSupplier,
                                                   Class<?> statelessSessionClass) {
        this.delegateSupplier = delegateSupplier;
        this.statelessSessionClass = statelessSessionClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle Object methods, ignore "close" and directly implement "unwrap"
        switch (method.getName()) {
            case "equals":
                if (method.equals(EQUALS)) {
                    return proxy == args[0];
                } // else fall through to invoking on the delegate
                break;
            case "hashCode":
                if (method.equals(HASH_CODE)) {
                    return System.identityHashCode(proxy);
                } // else fall through to invoking on the delegate
                break;
            case "toString":
                if (method.equals(TO_STRING)) {
                    return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                } // else fall through to invoking on the delegate
                break;
            case "close":
                if (args == null || args.length == 0) {
                    // We don't close the delegate.
                    // Delegates representing a transaction scoped stateless session will be closed when the (owning) component invocation completes
                    // For call stacks that wrap calls with NoTxEmCloser.pushCall/popCall, the popCall will close the delegate
                    // TODO https://issues.redhat.com/browse/WFLY-21272 what about call stacks not using NoTxSSCloser.pushCall/popCall???
                    return null;
                } // else fall through to invoking on the delegate
                break;
            case "unwrap":
                // If ORM adds a new 'unwrap' variant we need to update to account for that
                // This should not happen at runtime as StatelessSessionAPITestCase should catch this
                assert args != null && args.length == 1 && Class.class.equals(args[0].getClass());

                // For the StatelessSession interface exposed by the proxy we deliberately do not invoke on the delegate
                // for this method, since 'unwrap' is all about casting and returning the object being called.
                // We don't want to return the delegate.
                if (((Class<?>) args[0]).isAssignableFrom(statelessSessionClass)) {
                    return ((Class<?>) args[0]).cast(proxy);
                } // else we presume this is ORM needing implementation details, so fall through and let it have access to the delegate.
                break;
            default:
                break;
        }

        // For all other cases invoke the delegate
        try {
            return method.invoke(getDelegate(), args);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            throw cause != null ? cause : ite;
        }
    }

    private Object getDelegate() {
        return delegateSupplier.get();
    }
}
