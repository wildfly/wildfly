/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.context;

import javax.naming.Context;

import org.wildfly.common.function.ThreadLocalStack;

/**
 * Selects a naming context based on the provided identifier (eg. comp).  Maintains a thread local used to managed the current selector.
 * The current selector will be used by instances of {@code org.jboss.as.naming.contexts.NamespaceObjectFactory} to determine
 * which context to return.
 *
 * @author John E. Bailey
 */
public abstract class NamespaceContextSelector {
    /* Thread local maintaining the current context selector */
    private static ThreadLocalStack<NamespaceContextSelector> currentSelector = new ThreadLocalStack<NamespaceContextSelector>();

    private static NamespaceContextSelector defaultSelector;

    /**
     * Set the current context selector for the current thread.
     *
     * @param selector The current selector
     */
    public static void pushCurrentSelector(final NamespaceContextSelector selector) {
        currentSelector.push(selector);
    }

    /**
     * Pops the current selector for the thread, replacing it with the previous selector
     *
     * @return selector The current selector
     */
    public static NamespaceContextSelector popCurrentSelector() {
        return currentSelector.pop();
    }

    /**
     * Get the current context selector for the current thread.
     *
     * @return The current context selector.
     */
    public static NamespaceContextSelector getCurrentSelector() {
        NamespaceContextSelector selector = currentSelector.peek();
        if(selector != null) {
            return selector;
        }
        return defaultSelector;
    }

    /**
     * Get the context for a given identifier (eg. comp -> java:comp).  Implementers of this method can use any means to
     * determine which context to return.
     *
     * @param identifier The context identifier
     * @return The context for this identifier
     */
    public abstract Context getContext(final String identifier);

    public static void setDefault(NamespaceContextSelector selector) {
        defaultSelector = selector;
    }
}
