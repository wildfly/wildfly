/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.context;

import org.jboss.as.ejb3.context.spi.InvocationContext;
import org.jboss.as.ejb3.context.util.ThreadLocalStack;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class CurrentInvocationContext {
    private static ThreadLocalStack<InvocationContext> stack = new ThreadLocalStack<InvocationContext>();

    public static InvocationContext get() {
        InvocationContext current = stack.get();
        if (current == null)
            throw new IllegalStateException("No current invocation context available");
        return current;
    }

    public static <T extends InvocationContext> T get(Class<T> expectedType) {
        return expectedType.cast(get());
    }

    public static InvocationContext pop() {
        return stack.pop();
    }

    public static void push(InvocationContext invocation) {
        assert invocation != null : "invocation is null";
        stack.push(invocation);
    }
}
