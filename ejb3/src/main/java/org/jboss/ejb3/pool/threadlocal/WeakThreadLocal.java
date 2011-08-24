/*
<<<<<<< HEAD
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
=======
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
>>>>>>> pool
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
package org.jboss.ejb3.pool.threadlocal;

import java.lang.ref.WeakReference;

/**
 * ThreadLocal has an inherent memory leak: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6254531
 * <p/>
 * This class will only keep a weak reference so garbage collections works.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class WeakThreadLocal<T> {
    private ThreadLocal<WeakReference<T>> delegate = new ThreadLocal<WeakReference<T>>();

    public T get() {
        WeakReference<T> ref = delegate.get();
        if (ref == null)
            return null;
        return ref.get();
    }

    public void remove() {
        delegate.remove();
    }

    public void set(T value) {
        delegate.set(new WeakReference<T>(value));
    }
}
