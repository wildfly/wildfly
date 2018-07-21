/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.compat.jpa.hibernate.transformer;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.engine.spi.SessionImplementor;

public class PersistentQueue extends PersistentBag implements Queue {

    public PersistentQueue(SessionImplementor session) {
        super( session );
    }

    public PersistentQueue(SessionImplementor session, List list) {
        super( session, list );
    }

    @Override
    public boolean offer(Object o) {
        return add( o );
    }

    @Override
    public Object remove() {
        return poll();
    }

    @Override
    public Object poll() {
        int size = size();
        if(size > 0) {
            Object first = get(0);
            remove( 0 );
            return first;
        }
        throw new NoSuchElementException();
    }

    @Override
    public Object element() {
        return peek();
    }

    @Override
    public Object peek() {
        return size() > 0 ? get( 0 ) : null;
    }
}
