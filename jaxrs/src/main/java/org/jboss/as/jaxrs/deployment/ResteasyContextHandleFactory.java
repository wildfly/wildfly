/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.deployment;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import jakarta.enterprise.concurrent.ContextService;

import org.jboss.as.ee.concurrent.handle.ContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.ResetContextHandle;
import org.jboss.as.ee.concurrent.handle.SetupContextHandle;
import org.jboss.resteasy.core.ResteasyContext;

/**
 * A context factory for propagating the RESTEasy context for managed threads.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ResteasyContextHandleFactory implements ContextHandleFactory {
    private static final String NAME = "RESTEASY";
    static ResteasyContextHandleFactory INSTANCE = new ResteasyContextHandleFactory();

    private ResteasyContextHandleFactory() {
    }

    @Override
    public SetupContextHandle saveContext(final ContextService contextService, final Map<String, String> contextObjectProperties) {
        return new ResteasySetupContextHandle();
    }

    @Override
    public int getChainPriority() {
        return 900;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void writeSetupContextHandle(final SetupContextHandle contextHandle, final ObjectOutputStream out) {
    }

    @Override
    public SetupContextHandle readSetupContextHandle(final ObjectInputStream in) {
        return new ResteasySetupContextHandle();
    }

    private static class ResteasySetupContextHandle implements SetupContextHandle {
        private final Map<Class<?>, Object> currentContext;

        private ResteasySetupContextHandle() {
            this.currentContext = ResteasyContext.getContextDataMap();
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            ResteasyContext.pushContextDataMap(currentContext);
            return new ResetContextHandle() {
                @Override
                public void reset() {
                    ResteasyContext.removeContextDataLevel();
                }

                @Override
                public String getFactoryName() {
                    return NAME;
                }
            };
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }
    }
}
