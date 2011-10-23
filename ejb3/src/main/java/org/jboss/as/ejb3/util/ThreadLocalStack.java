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

import java.util.LinkedList;

/**
* @author Stuart Douglas
*/
public class ThreadLocalStack<T> {
    private final ThreadLocal<LinkedList<T>> stack = new ThreadLocal<LinkedList<T>>();

    public void push(T obj) {
        LinkedList<T> list = stack.get();
        if (list == null) {
            list = new LinkedList<T>();
            stack.set(list);
        }
        list.addLast(obj);
    }

    public T pop() {
        LinkedList<T> list = stack.get();
        if (list == null) {
            return null;
        }
        T rtn = list.removeLast();
        if (list.size() == 0) {
            stack.remove();
        }
        return rtn;
    }

    public T get() {
        LinkedList<T> list = stack.get();
        if (list == null) {
            return null;
        }
        return list.getLast();
    }
}
