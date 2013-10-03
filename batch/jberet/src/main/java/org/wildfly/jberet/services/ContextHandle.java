package org.wildfly.jberet.services;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;

/**
 * Allows a handle to setup any thread local context needed.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
interface ContextHandle {

    /**
     * Sets up any thread local context.
     *
     * @return a handle to tear down the thread local context when the invocation has finished
     */
    Handle setup();

    /**
     * Handles tearing down the thread local context.
     */
    interface Handle {

        /**
         * Tears down the thread local context.
         */
        void tearDown();
    }

    /**
     * A handle that processes {@link ContextHandle context handles} in the order in which they are added. The {@link
     * org.wildfly.jberet.services.ContextHandle.Handle#tearDown()} is processed in the reverse order that the {@link
     * ContextHandle context handles} are processed.
     * <p/>
     * If a {@link ContextHandle context handle} throws an exception the {@link org.wildfly.jberet.services.ContextHandle.Handle#tearDown()
     * tear downs} will be processed.
     * <p/>
     * If an exception is thrown during the {@link org.wildfly.jberet.services.ContextHandle.Handle#tearDown() tear
     * down} the remaining tear downs will be processed and only the initial exception will be re-thrown.
     */
    class ChainedContextHandle implements ContextHandle {

        private final Collection<ContextHandle> contextHandles;

        public ChainedContextHandle(final ContextHandle... contextHandles) {
            this.contextHandles = Arrays.asList(contextHandles);
        }

        @Override
        public Handle setup() {
            final Deque<Handle> handles = new ArrayDeque<>();
            try {
                for (ContextHandle contextHandle : contextHandles) {
                    // reverse order
                    handles.addFirst(contextHandle.setup());
                }
                return new Handle() {
                    @Override
                    public void tearDown() {
                        Exception rethrow = null;
                        for (Handle handle : handles) {
                            try {
                                handle.tearDown();
                            } catch (Exception e) {
                                if (rethrow == null) rethrow = e;
                            }
                        }
                        if (rethrow != null) {
                            throw new RuntimeException(rethrow);
                        }
                    }
                };
            } catch (Exception e) {
                for (Handle handle : handles) {
                    try {
                        handle.tearDown();
                    } catch (Exception ignore) {
                    }
                }
                throw e;
            }
        }
    }
}
