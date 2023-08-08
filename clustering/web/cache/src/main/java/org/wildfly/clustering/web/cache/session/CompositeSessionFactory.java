/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.wildfly.clustering.web.LocalContextFactory;
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
public class CompositeSessionFactory<C, V, L> extends CompositeImmutableSessionFactory<V, L> implements SessionFactory<C, CompositeSessionMetaDataEntry<L>, V, L> {

    private final SessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory;
    private final SessionAttributesFactory<C, V> attributesFactory;
    private final LocalContextFactory<L> localContextFactory;

    public CompositeSessionFactory(SessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory, SessionAttributesFactory<C, V> attributesFactory, LocalContextFactory<L> localContextFactory) {
        super(metaDataFactory, attributesFactory);
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public Map.Entry<CompositeSessionMetaDataEntry<L>, V> createValue(String id, Duration defaultTimeout) {
        CompositeSessionMetaDataEntry<L> metaDataValue = this.metaDataFactory.createValue(id, defaultTimeout);
        if (metaDataValue == null) return null;
        V attributesValue = this.attributesFactory.createValue(id, null);
        return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
    }

    @Override
    public Map.Entry<CompositeSessionMetaDataEntry<L>, V> findValue(String id) {
        CompositeSessionMetaDataEntry<L> metaDataValue = this.metaDataFactory.findValue(id);
        if (metaDataValue != null) {
            V attributesValue = this.attributesFactory.findValue(id);
            if (attributesValue != null) {
                return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
            }
            // Purge obsolete meta data
            this.metaDataFactory.purge(id);
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
    public SessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> getMetaDataFactory() {
        return this.metaDataFactory;
    }

    @Override
    public SessionAttributesFactory<C, V> getAttributesFactory() {
        return this.attributesFactory;
    }

    @Override
    public Session<L> createSession(String id, Map.Entry<CompositeSessionMetaDataEntry<L>, V> entry, C context) {
        CompositeSessionMetaDataEntry<L> key = entry.getKey();
        InvalidatableSessionMetaData metaData = this.metaDataFactory.createSessionMetaData(id, key);
        SessionAttributes attributes = this.attributesFactory.createSessionAttributes(id, entry.getValue(), metaData, context);
        return new CompositeSession<>(id, metaData, attributes, key.getLocalContext(), this.localContextFactory, this);
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
