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

import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.msc.service.ServiceName;

import javax.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * The context handle factory responsible for saving and setting the naming context.
 *
 * @author Eduardo Martins
 */
public class NamingContextHandleFactory extends ChainedContextHandleFactory {

    public static final NamingContextHandleFactory INSTANCE = new NamingContextHandleFactory();

    private NamingContextHandleFactory() {
        super(Type.NAMING_CHAINED);
        add(NamespaceContextSelectorContextHandleFactory.INSTANCE);
        add(DeploymentUnitServiceNameContextHandleFactory.INSTANCE);
    }

    private static class NamespaceContextSelectorContextHandleFactory implements ContextHandleFactory {

        public static final NamespaceContextSelectorContextHandleFactory INSTANCE = new NamespaceContextSelectorContextHandleFactory();

        private NamespaceContextSelectorContextHandleFactory() {
        }

        @Override
        public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
            final NamespaceContextSelector namespaceContextSelector = NamespaceContextSelector.getCurrentSelector();
            return new ContextHandle() {
                @Override
                public void setup() throws IllegalStateException {
                    if(namespaceContextSelector != null) {
                        NamespaceContextSelector.pushCurrentSelector(namespaceContextSelector);
                    }
                }

                @Override
                public void reset() {
                    if(namespaceContextSelector != null) {
                        NamespaceContextSelector.popCurrentSelector();
                    }
                }
            };
        }

        @Override
        public Type getType() {
            return Type.NAMING_NAMESPACE_SELECTOR;
        }
    }

    private static class DeploymentUnitServiceNameContextHandleFactory implements ContextHandleFactory {

        public static final DeploymentUnitServiceNameContextHandleFactory INSTANCE = new DeploymentUnitServiceNameContextHandleFactory();

        private DeploymentUnitServiceNameContextHandleFactory() {
        }

        @Override
        public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
            final ServiceName duServiceName = WritableServiceBasedNamingStore.currentOwner();
            return new ContextHandle() {
                @Override
                public void setup() throws IllegalStateException {
                    if(duServiceName != null) {
                        WritableServiceBasedNamingStore.pushOwner(duServiceName);
                    }
                }

                @Override
                public void reset() {
                    if(duServiceName != null) {
                        WritableServiceBasedNamingStore.popOwner();
                    }
                }
            };
        }

        @Override
        public Type getType() {
            return Type.NAMING_WRITABLE_SERVICE_BASED_STORE_OWNER;
        }
    }
}
