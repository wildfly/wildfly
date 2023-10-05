/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.wildfly.clustering.web.cache.session.attributes.ImmutableSessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.metadata.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Generic immutable session factory implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
public class CompositeImmutableSessionFactory<MV, AV> implements ImmutableSessionFactory<MV, AV> {

    private final ImmutableSessionMetaDataFactory<MV> metaDataFactory;
    private final ImmutableSessionAttributesFactory<AV> attributesFactory;

    public CompositeImmutableSessionFactory(ImmutableSessionMetaDataFactory<MV> metaDataFactory, ImmutableSessionAttributesFactory<AV> attributesFactory) {
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
    }

    @Override
    public Map.Entry<MV, AV> findValue(String id) {
        MV metaDataValue = this.metaDataFactory.findValue(id);
        if (metaDataValue != null) {
            AV attributesValue = this.attributesFactory.findValue(id);
            if (attributesValue != null) {
                return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
            }
        }
        return null;
    }

    @Override
    public Map.Entry<MV, AV> tryValue(String id) {
        MV metaDataValue = this.metaDataFactory.tryValue(id);
        if (metaDataValue != null) {
            AV attributesValue = this.attributesFactory.tryValue(id);
            if (attributesValue != null) {
                return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
            }
        }
        return null;
    }

    @Override
    public ImmutableSessionMetaDataFactory<MV> getMetaDataFactory() {
        return this.metaDataFactory;
    }

    @Override
    public ImmutableSessionAttributesFactory<AV> getAttributesFactory() {
        return this.attributesFactory;
    }

    @Override
    public ImmutableSession createImmutableSession(String id, ImmutableSessionMetaData metaData, ImmutableSessionAttributes attributes) {
        return new CompositeImmutableSession(id, metaData, attributes);
    }
}
