/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.concurrent;

import org.jboss.as.ee.EeLogger;
import org.jboss.as.ee.EeMessages;
import org.jboss.as.ee.concurrent.handle.ContextHandle;
import org.jboss.as.ee.concurrent.handle.ContextHandleFactory;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.naming.util.ThreadLocalStack;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import javax.enterprise.concurrent.ContextService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.lang.Thread.currentThread;

/**
 * Manages context handle factories, it is used by EE Context Services to save the invocation context.
 *
 * @author Eduardo Martins
 */
public class ConcurrentContext {

    /**
     * the name of the factory used by the chained context handles
     */
    public static final String CONTEXT_HANDLE_FACTORY_NAME = "CONCURRENT_CONTEXT";

    /**
     * a thread local stack with the contexts pushed
     */
    private static final ThreadLocalStack<ConcurrentContext> current = new ThreadLocalStack<ConcurrentContext>();

    /**
     * Sets the specified context as the current one, in the current thread.
     *
     * @param context The current context
     */
    public static void pushCurrent(final ConcurrentContext context) {
        current.push(context);
    }

    /**
     * Pops the current context in the current thread.
     *
     * @return
     */
    public static ConcurrentContext popCurrent() {
        return current.pop();
    }

    /**
     * Retrieves the current context in the current thread.
     *
     * @return
     */
    public static ConcurrentContext current() {
        return current.peek();
    }

    private final Map<String, ContextHandleFactory> factoryMap = new HashMap<>();
    private List<ContextHandleFactory> factoryOrderedList;

    private final String applicationName;
    private final String moduleName;
    private final String componentName;

    /**
     *
     * @param applicationName
     * @param moduleName
     * @param componentName
     */
    public ConcurrentContext(String applicationName, String moduleName, String componentName) {
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.componentName = componentName;
    }

    /**
     * Retrieves the name of the application related with the context.
     * @return
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Retrieves the name of the module related with the context.
     * @return
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Retrieves the name of the component related with the context, if any.
     * @return
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Adds a new factory.
     * @param factory
     */
    public synchronized void addFactory(ContextHandleFactory factory) {
        final String factoryName = factory.getName();
        if(factoryMap.containsKey(factoryName)) {
            throw EeMessages.MESSAGES.factoryAlreadyExists(this,factoryName);
        }
        factoryMap.put(factoryName, factory);
        final Comparator<ContextHandleFactory> comparator = new Comparator<ContextHandleFactory>() {
            @Override
            public int compare(ContextHandleFactory o1, ContextHandleFactory o2) {
                return Integer.compare(o1.getChainPriority(),o2.getChainPriority());
            }
        };
        SortedSet<ContextHandleFactory> sortedSet = new TreeSet<>(comparator);
        sortedSet.addAll(factoryMap.values());
        factoryOrderedList = new ArrayList<>(sortedSet);
    }

    /**
     * Saves the current invocation context on a chained context handle.
     * @param contextService
     * @param contextObjectProperties
     * @return
     */
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        final List<ContextHandle> handles = new ArrayList<>(factoryOrderedList.size());
        for (ContextHandleFactory factory : factoryOrderedList) {
            handles.add(factory.saveContext(contextService, contextObjectProperties));
        }
        return new ChainedContextHandle(this, handles);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(applicationName).append('/').append(moduleName);
        if(componentName != null) {
            sb.append('/').append(componentName);
        }
        return sb.toString();
    }

    /**
     * A context handle that is a chain of other context handles
     */
    private static class ChainedContextHandle implements ContextHandle {

        private transient ConcurrentContext concurrentContext;
        private transient List<ContextHandle> setupHandles;
        private transient LinkedList<ContextHandle> resetHandles;

        private ChainedContextHandle(ConcurrentContext concurrentContext, List<ContextHandle> setupHandles) {
            this.concurrentContext = concurrentContext;
            this.setupHandles = setupHandles;
        }

        @Override
        public void setup() throws IllegalStateException {
            resetHandles = new LinkedList<>();
            try {
                ConcurrentContext.pushCurrent(concurrentContext);
                for (ContextHandle handle : setupHandles) {
                    handle.setup();
                    resetHandles.addFirst(handle);
                }
            } catch (Error | RuntimeException e) {
                reset();
                throw e;
            }
        }

        @Override
        public void reset() {
            if(resetHandles != null) {
                for (ContextHandle handle : resetHandles) {
                    try {
                        handle.reset();
                    } catch (Throwable e) {
                        EeLogger.ROOT_LOGGER.debug("failed to reset handle",e);
                    }
                }
                resetHandles = null;
                ConcurrentContext.popCurrent();
            }
        }

        @Override
        public String getFactoryName() {
            return CONTEXT_HANDLE_FACTORY_NAME;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            // write the concurrent context service name
            out.writeObject(ConcurrentServiceNames.getConcurrentContextServiceName(concurrentContext.applicationName, concurrentContext.getModuleName(), concurrentContext.getComponentName()));
            // write the number of setup handles
            out.write(setupHandles.size());
            // write each handle
            ContextHandleFactory factory = null;
            String factoryName = null;
            for(ContextHandle handle : setupHandles) {
                factoryName = handle.getFactoryName();
                factory = concurrentContext.factoryMap.get(factoryName);
                if(factory == null) {
                    throw EeMessages.MESSAGES.factoryNotFound(concurrentContext,factoryName);
                }
                out.writeUTF(factoryName);
                factory.writeHandle(handle, out);
            }
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            // switch to EE module classloader, otherwise, due to serialization, deployments would need to have dependencies to other internal modules
            final Module module;
            try {
                module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.as.ee"));
            } catch (Throwable e) {
                throw new IOException(e);
            }
            final SecurityManager sm = System.getSecurityManager();
            final ClassLoader classLoader;
            if (sm == null) {
                classLoader = currentThread().getContextClassLoader();
            } else {
                classLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return currentThread().getContextClassLoader();
                    }
                });
            }
            if (sm == null) {
                currentThread().setContextClassLoader(module.getClassLoader());
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        currentThread().setContextClassLoader(module.getClassLoader());
                        return null;
                    }
                });
            }
            try {
                in.defaultReadObject();
                // restore concurrent context from msc
                final ServiceName serviceName = (ServiceName) in.readObject();
                final ServiceController<?> serviceController = CurrentServiceContainer.getServiceContainer().getService(serviceName);
                if(serviceController == null) {
                    throw EeMessages.MESSAGES.concurrentContextServiceNotInstalled(serviceName);
                }
                concurrentContext = (ConcurrentContext) serviceController.getValue();
                // read setup handles
                setupHandles = new ArrayList<>();
                ContextHandleFactory factory = null;
                String factoryName = null;
                for(int i = in.read(); i > 0; i--) {
                    factoryName = in.readUTF();
                    factory = concurrentContext.factoryMap.get(factoryName);
                    if(factory == null) {
                        throw EeMessages.MESSAGES.factoryNotFound(concurrentContext,factoryName);
                    }
                    setupHandles.add(factory.readHandle(in));
                }
            } finally {
                if (sm == null) {
                    currentThread().setContextClassLoader(classLoader);
                } else {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            currentThread().setContextClassLoader(classLoader);
                            return null;
                        }
                    });
                }
            }
        }
    }

}
