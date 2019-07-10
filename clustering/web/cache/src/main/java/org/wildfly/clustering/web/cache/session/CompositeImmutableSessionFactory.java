/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Generic immutable session factory implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
public class CompositeImmutableSessionFactory<V, L> implements ImmutableSessionFactory<CompositeSessionMetaDataEntry<L>, V> {

    private final ImmutableSessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory;
    private final ImmutableSessionAttributesFactory<V> attributesFactory;

    public CompositeImmutableSessionFactory(ImmutableSessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory, ImmutableSessionAttributesFactory<V> attributesFactory) {
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
    }

    @Override
    public Map.Entry<CompositeSessionMetaDataEntry<L>, V> findValue(String id) {
        CompositeSessionMetaDataEntry<L> metaDataValue = this.metaDataFactory.findValue(id);
        if (metaDataValue != null) {
            V attributesValue = this.attributesFactory.findValue(id);
            if (attributesValue != null) {
                return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
            }
        }
        return null;
    }

    @Override
    public Map.Entry<CompositeSessionMetaDataEntry<L>, V> tryValue(String id) {
        CompositeSessionMetaDataEntry<L> metaDataValue = this.metaDataFactory.tryValue(id);
        if (metaDataValue != null) {
            V attributesValue = this.attributesFactory.tryValue(id);
            if (attributesValue != null) {
                return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
            }
        }
        return null;
    }

    @Override
    public ImmutableSessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> getMetaDataFactory() {
        return this.metaDataFactory;
    }

    @Override
    public ImmutableSessionAttributesFactory<V> getAttributesFactory() {
        return this.attributesFactory;
    }

    @Override
    public ImmutableSession createImmutableSession(String id, ImmutableSessionMetaData metaData, ImmutableSessionAttributes attributes) {
        return new CompositeImmutableSession(id, metaData, attributes);
    }
}
