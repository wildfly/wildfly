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
package org.jboss.as.ee.concurrent.handle;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.msc.service.ServiceName;

import jakarta.enterprise.concurrent.ContextService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * The context handle factory responsible for saving and setting the naming context.
 *
 * @author Eduardo Martins
 */
public class NamingContextHandleFactory implements EE10ContextHandleFactory {

    private static final NamingContextHandle CLEARED_CONTEXT_HANDLE = new NamingContextHandle(null, null);

    public static final String NAME = "NAMING";

    private final NamespaceContextSelector namespaceContextSelector;
    private final ServiceName duServiceName;

    public NamingContextHandleFactory(NamespaceContextSelector namespaceContextSelector, ServiceName duServiceName) {
        this.namespaceContextSelector = namespaceContextSelector;
        this.duServiceName = duServiceName;
    }

    @Override
    public String getContextType() {
        return ContextServiceDefinition.APPLICATION;
    }

    @Override
    public SetupContextHandle clearedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return CLEARED_CONTEXT_HANDLE;
    }

    @Override
    public SetupContextHandle propagatedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new NamingContextHandle(namespaceContextSelector,duServiceName);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        return 200;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
        out.writeBoolean(contextHandle != CLEARED_CONTEXT_HANDLE);
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return in.readBoolean() ? new NamingContextHandle(namespaceContextSelector,duServiceName) : CLEARED_CONTEXT_HANDLE;
    }

    private static class NamingContextHandle implements SetupContextHandle, ResetContextHandle {

        private static final long serialVersionUID = -4631099493960707685L;
        private final NamespaceContextSelector namespaceContextSelector;
        private final ServiceName duServiceName;

        private NamingContextHandle(NamespaceContextSelector namespaceContextSelector, ServiceName duServiceName) {
            this.namespaceContextSelector = namespaceContextSelector;
            this.duServiceName = duServiceName;
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            NamespaceContextSelector.pushCurrentSelector(namespaceContextSelector);
            WritableServiceBasedNamingStore.pushOwner(duServiceName);
            return this;
        }

        @Override
        public void reset() {
            WritableServiceBasedNamingStore.popOwner();
            NamespaceContextSelector.popCurrentSelector();
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
