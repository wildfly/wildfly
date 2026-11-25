/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.glassfish.concurro.spi.ContextHandle;
import org.glassfish.concurro.spi.ContextSetupProvider;
import org.jboss.as.ee.concurrent.handle.ResetContextHandle;
import org.jboss.as.ee.concurrent.handle.SetupContextHandle;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.concurrent.handle.NullContextHandle;

import jakarta.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * The default context setup provider, delegates context saving/setting/resetting to the context handle factory provided by the current concurrent context.
 *
 * @author Eduardo Martins
 */
public class ConcurroDefaultContextSetupProviderImpl implements ContextSetupProvider {

    @Override
    public ContextHandle saveContext(ContextService contextService) {
        return saveContext(contextService, null);
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        final ConcurrentContext concurrentContext = ConcurrentContext.current();
        if (concurrentContext != null) {
            return new SetupContextHandleWrapper(concurrentContext.saveContext(contextService, contextObjectProperties));
        } else {
            EeLogger.ROOT_LOGGER.debug("ee concurrency context not found in invocation context");
            return new SetupContextHandleWrapper(new NullContextHandle());
        }
    }

    @Override
    public ContextHandle setup(ContextHandle contextHandle) throws IllegalStateException {
        return ((SetupContextHandleWrapper) contextHandle).setup();
    }

    @Override
    public void reset(ContextHandle contextHandle) {
        ((ResetContextHandle) contextHandle).reset();
    }

    static class SetupContextHandleWrapper implements SetupContextHandle, ContextHandle {
        private final SetupContextHandle contextHandle;
        public SetupContextHandleWrapper(SetupContextHandle contextHandle) {
            this.contextHandle = contextHandle;
        }
        @Override
        public ResetContextHandleWrapper setup() throws IllegalStateException {
            return new ResetContextHandleWrapper(contextHandle.setup());
        }
        @Override
        public String getFactoryName() {
            return contextHandle.getFactoryName();
        }
    }

    static class ResetContextHandleWrapper implements ResetContextHandle, ContextHandle {
        private final ResetContextHandle contextHandle;
        public ResetContextHandleWrapper(ResetContextHandle contextHandle) {
            this.contextHandle = contextHandle;
        }
        @Override
        public void reset() {
            contextHandle.reset();
        }
        @Override
        public String getFactoryName() {
            return contextHandle.getFactoryName();
        }
    }
}
