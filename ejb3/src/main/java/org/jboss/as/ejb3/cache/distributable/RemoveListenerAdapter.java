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
package org.jboss.as.ejb3.cache.distributable;

import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.wildfly.clustering.ejb.RemoveListener;

/**
 * Adapts a {@link StatefulObjectFactory} to a {@link RemoveListener}.
 *
 * @author Paul Ferraro
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class RemoveListenerAdapter<K, V extends Identifiable<K>> implements RemoveListener<V> {

    private final StatefulObjectFactory<V> factory;

    public RemoveListenerAdapter(StatefulObjectFactory<V> factory) {
        this.factory = factory;
    }

    @Override
    public void removed(V bean) {
        this.factory.destroyInstance(bean);
    }
}
