/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.function.Supplier;

import org.wildfly.clustering.web.cache.Contextual;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.metadata.InvalidatableSessionMetaData;
import org.wildfly.clustering.web.cache.session.metadata.SessionMetaDataFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;

/**
 * @param <C> the ServletContext specification type
 * @param <V> the session attribute value type
 * @param <L> the local context type
 * @author Paul Ferraro
 */
public class CompositeSessionFactory<C, MV extends Contextual<L>, AV, L> extends CompositeImmutableSessionFactory<MV, AV> implements SessionFactory<C, MV, AV, L> {

    private final SessionMetaDataFactory<MV> metaDataFactory;
    private final SessionAttributesFactory<C, AV> attributesFactory;
    private final Supplier<L> localContextFactory;

    public CompositeSessionFactory(SessionMetaDataFactory<MV> metaDataFactory, SessionAttributesFactory<C, AV> attributesFactory, Supplier<L> localContextFactory) {
        super(metaDataFactory, attributesFactory);
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public Map.Entry<MV, AV> createValue(String id, Duration defaultTimeout) {
        MV metaDataValue = this.metaDataFactory.createValue(id, defaultTimeout);
        if (metaDataValue == null) return null;
        AV attributesValue = this.attributesFactory.createValue(id, null);
        return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
    }

    @Override
    public Map.Entry<MV, AV> findValue(String id) {
        MV metaDataValue = this.metaDataFactory.findValue(id);
        if (metaDataValue != null) {
            AV attributesValue = this.attributesFactory.findValue(id);
            if (attributesValue != null) {
                return Map.entry(metaDataValue, attributesValue);
            }
            // Purge obsolete meta data
            this.metaDataFactory.purge(id);
        }
        return null;
    }

    @Override
    public Map.Entry<MV, AV> tryValue(String id) {
        MV metaDataValue = this.metaDataFactory.tryValue(id);
        if (metaDataValue != null) {
            AV attributesValue = this.attributesFactory.tryValue(id);
            if (attributesValue != null) {
                return Map.entry(metaDataValue, attributesValue);
            }
        }
        return null;
    }

    @Override
    public boolean remove(String id) {
        this.attributesFactory.remove(id);
        return this.metaDataFactory.remove(id);
    }

    @Override
    public boolean purge(String id) {
        this.attributesFactory.purge(id);
        return this.metaDataFactory.purge(id);
    }

    @Override
    public SessionMetaDataFactory<MV> getMetaDataFactory() {
        return this.metaDataFactory;
    }

    @Override
    public SessionAttributesFactory<C, AV> getAttributesFactory() {
        return this.attributesFactory;
    }

    @Override
    public Session<L> createSession(String id, Map.Entry<MV, AV> entry, C context) {
        MV metaDataValue = entry.getKey();
        AV attributeValue = entry.getValue();
        InvalidatableSessionMetaData metaData = this.metaDataFactory.createSessionMetaData(id, metaDataValue);
        SessionAttributes attributes = this.attributesFactory.createSessionAttributes(id, attributeValue, metaData, context);
        return new CompositeSession<>(id, metaData, attributes, metaDataValue, this.localContextFactory, this);
    }

    @Override
    public ImmutableSession createImmutableSession(String id, ImmutableSessionMetaData metaData, ImmutableSessionAttributes attributes) {
        return new CompositeImmutableSession(id, metaData, attributes);
    }

    @Override
    public void close() {
        this.metaDataFactory.close();
        this.attributesFactory.close();
    }
}
