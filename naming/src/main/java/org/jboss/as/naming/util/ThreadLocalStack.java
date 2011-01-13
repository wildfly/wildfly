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
package org.jboss.as.naming.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;

/**
 *
 * @author Stuart Douglas
 *
 */
public class ThreadLocalStack<E> {

    private final ThreadLocal<Deque<E>> deque = new ThreadLocal<Deque<E>>();

    public void push(E item) {
        Deque<E> st = deque.get();
        if (st == null) {
            st = new ArrayDeque<E>();
            deque.set(st);
        }
        st.push(item);
    }

    public E peek() {
        Deque<E> st = deque.get();
        if (st == null) {
            return null;
        }
        return st.peek();
    }

    public E pop() {
        Deque<E> st = deque.get();
        if (st == null) {
            throw new EmptyStackException();
        }
        E val = st.pop();
        if (st.isEmpty()) {
            deque.remove();
        }
        return val;
    }

}
