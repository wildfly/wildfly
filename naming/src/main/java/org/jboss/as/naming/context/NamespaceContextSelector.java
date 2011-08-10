/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.context;

import javax.naming.Context;

import org.jboss.as.naming.util.ThreadLocalStack;

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
