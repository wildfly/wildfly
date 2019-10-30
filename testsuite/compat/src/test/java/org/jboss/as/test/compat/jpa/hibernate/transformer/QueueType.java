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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

public class QueueType implements UserCollectionType {

    @Override
    public PersistentCollection instantiate(
            SessionImplementor session,
            CollectionPersister persister) throws HibernateException {
        return new PersistentQueue( session );
    }

    @Override
    public PersistentCollection wrap(
            SessionImplementor session,
            Object collection) {
        return new PersistentQueue( session, (List) collection );
    }

    @Override
    public Iterator getElementsIterator(Object collection) {
        return ( (Queue) collection ).iterator();
    }

    @Override
    public boolean contains(Object collection, Object entity) {
        return ( (Queue) collection ).contains( entity );
    }

    @Override
    public Object indexOf(Object collection, Object entity) {
        int i = ( (List) collection ).indexOf( entity );
        return ( i < 0 ) ? null : i;
    }

    @Override
    public Object replaceElements(
            Object original,
            Object target,
            CollectionPersister persister,
            Object owner,
            Map copyCache,
            SessionImplementor session)
            throws HibernateException {
        Queue result = (Queue) target;
        result.clear();
        result.addAll( (Queue) original );
        return result;
    }

    @Override
    public Object instantiate(int anticipatedSize) {
        return new LinkedList<>();
    }

}
