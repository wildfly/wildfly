/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A thread local stack data structure. In order to avoid memory churn the underlying
 * ArrayDeque is never freed. If we remove the deque when it is empty then this results
 * in excessive deque allocations.
 *
 * @author Stuart Douglas
 *
 */
public class ThreadLocalStack<E> {

    private static final Object NULL_VALUE = new Object();

    private final ThreadLocal<Deque<Object>> deque = new ThreadLocal<Deque<Object>>() {
        @Override
        protected ArrayDeque<Object> initialValue() {
            return new ArrayDeque<>();
        }
    };

    public void push(E item) {
        Deque<Object> st = deque.get();
        if(item == null) {
            st.push(NULL_VALUE);
        } else {
            st.push(item);
        }
    }

    public E peek() {
        Deque<Object> st = deque.get();
        Object o =  st.peek();
        if(o == NULL_VALUE) {
            return null;
        } else {
            return (E) o;
        }
    }

    public E pop() {
        Deque<Object> st = deque.get();
        Object o =  st.pop();
        if(o == NULL_VALUE) {
            return null;
        } else {
            return (E) o;
        }
    }

    public boolean isEmpty() {
        return deque.get().isEmpty();
    }

}
