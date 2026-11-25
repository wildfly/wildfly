/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

import jakarta.enterprise.concurrent.ContextService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * A context handle factory which is responsible for saving and setting proper classloading context.
 *
 * @author Eduardo Martins
 */
public class ClassLoaderContextHandleFactory implements EE10ContextHandleFactory {

    public static final String NAME = "CLASSLOADER";

    private final ClassLoader classLoader;

    public ClassLoaderContextHandleFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public String getContextType() {
        return ContextServiceDefinition.APPLICATION;
    }

    @Override
    public SetupContextHandle propagatedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new ClassLoaderSetupContextHandle(classLoader);
    }

    @Override
    public SetupContextHandle clearedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new ClassLoaderSetupContextHandle(null);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        return 100;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
        out.writeBoolean(((ClassLoaderSetupContextHandle)contextHandle).classLoader != null);
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return new ClassLoaderSetupContextHandle(in.readBoolean() ? classLoader : null);
    }

    static class ClassLoaderSetupContextHandle implements SetupContextHandle {

        private static final long serialVersionUID = -2669625643479981561L;
        private final ClassLoader classLoader;

        ClassLoaderSetupContextHandle(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            final ClassLoaderResetContextHandle resetContextHandle = new ClassLoaderResetContextHandle(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            return resetContextHandle;
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }

    private static class ClassLoaderResetContextHandle implements ResetContextHandle {

        private static final long serialVersionUID = -579159484365527468L;
        private final ClassLoader previous;

        private ClassLoaderResetContextHandle(ClassLoader previous) {
            this.previous = previous;
        }

        @Override
        public void reset() {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(previous);
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw EeLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }
}
