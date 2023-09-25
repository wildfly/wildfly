/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
