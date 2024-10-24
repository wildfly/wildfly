/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.spi.ThreadContextProvider;
import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * The context handle factory responsible for saving and setting the context for a deployement's ThreadContextProvider.
 *
 * @author Eduardo Martins
 */
public class ThreadContextProviderContextHandleFactory implements EE10ContextHandleFactory {

    private static final int BASE_PRIORITY = 1000;

    private final ThreadContextProvider threadContextProvider;
    private final int priority;

    public ThreadContextProviderContextHandleFactory(ThreadContextProvider threadContextProvider, int priority) {
        this.threadContextProvider = threadContextProvider;
        this.priority = BASE_PRIORITY + priority;
    }

    @Override
    public String getContextType() {
        return threadContextProvider.getThreadContextType();
    }

    @Override
    public SetupContextHandle clearedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new ContextHandle(threadContextProvider, contextObjectProperties, true);
    }

    @Override
    public SetupContextHandle propagatedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new ContextHandle(threadContextProvider, contextObjectProperties, false);
    }

    @Override
    public String getName() {
        return getContextType();
    }

    @Override
    public int getChainPriority() {
        return priority;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
        out.writeObject(contextHandle);
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return (ContextHandle) in.readObject();
    }

    private static class ContextHandle implements SetupContextHandle {

        private static final long serialVersionUID = 842115413317072688L;
        private final String factoryName;
        private final ThreadContextSnapshot savedContextSnapshot;

        private ContextHandle(ThreadContextProvider threadContextProvider, Map<String, String> contextObjectProperties, boolean cleared) {
            this.factoryName = threadContextProvider.getThreadContextType();
            this.savedContextSnapshot = cleared ? threadContextProvider.clearedContext(contextObjectProperties) : threadContextProvider.currentContext(contextObjectProperties);
        }

        @Override
        public String getFactoryName() {
            return factoryName;
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            final ThreadContextRestorer threadContextRestorer = savedContextSnapshot.begin();
            return new ResetContextHandle() {
                @Override
                public void reset() {
                    threadContextRestorer.endContext();
                }
                @Override
                public String getFactoryName() {
                    return factoryName;
                }
            };
        }
    }

    /**
     * Retrieves a collection containing a new factory for each ThreadContextProvider found on the specified ClassLoader, through the ServiceLoader framework.
     * @param classLoader
     * @return
     */
    public static Collection<ThreadContextProviderContextHandleFactory> fromServiceLoader(ClassLoader classLoader) {
        if(WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked((PrivilegedAction<Collection<ThreadContextProviderContextHandleFactory>>) () -> fromServiceLoaderUnchecked(classLoader));
        } else {
            return fromServiceLoaderUnchecked(classLoader);
        }
    }

    private static Collection<ThreadContextProviderContextHandleFactory> fromServiceLoaderUnchecked(ClassLoader classLoader) {
        final Set<ThreadContextProviderContextHandleFactory> factories = new HashSet<>();
        final ServiceLoader<ThreadContextProvider> threadContextProviderServiceLoader = ServiceLoader.load(ThreadContextProvider.class, classLoader);
        final Iterator<ThreadContextProvider> threadContextProviderIterator = threadContextProviderServiceLoader.iterator();
        int count = 0;
        while (threadContextProviderIterator.hasNext()) {
            final ThreadContextProvider threadContextProvider = threadContextProviderIterator.next();
            factories.add(new ThreadContextProviderContextHandleFactory(threadContextProvider, count));
            count++;
        }
        return factories;
    }
}
