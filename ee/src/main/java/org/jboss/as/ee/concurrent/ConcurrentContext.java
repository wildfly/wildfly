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

import org.jboss.as.ee.concurrent.handle.ChainedContextHandleFactory;
import org.jboss.as.naming.util.ThreadLocalStack;

/**
 * The concurrent context allows the retrieval of the managed objects related with the component in context, and is also responsible for providing the component's default context handle factory.
 *
 * @author Eduardo Martins
 */
public class ConcurrentContext {

    private final ChainedContextHandleFactory allChainedContextHandleFactory;

    public ConcurrentContext(ChainedContextHandleFactory allChainedContextHandleFactory) {
        this.allChainedContextHandleFactory = allChainedContextHandleFactory;
    }

    public ChainedContextHandleFactory getAllChainedContextHandleFactory() {
        return allChainedContextHandleFactory;
    }

    // current thread concurrent context management

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


}
