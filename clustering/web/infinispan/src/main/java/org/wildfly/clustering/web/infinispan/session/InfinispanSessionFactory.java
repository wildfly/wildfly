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

package org.wildfly.clustering.web.infinispan.session;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;

/**
 * @author Paul Ferraro
 */
public class InfinispanSessionFactory<V, L> implements SessionFactory<InfinispanSessionMetaData<L>, V, L> {

    private final SessionMetaDataFactory<InfinispanSessionMetaData<L>, L> metaDataFactory;
    private final SessionAttributesFactory<V> attributesFactory;
    private final LocalContextFactory<L> localContextFactory;

    public InfinispanSessionFactory(SessionMetaDataFactory<InfinispanSessionMetaData<L>, L> metaDataFactory, SessionAttributesFactory<V> attributesFactory, LocalContextFactory<L> localContextFactory) {
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public Map.Entry<InfinispanSessionMetaData<L>, V> createValue(String id, Void context) {
        InfinispanSessionMetaData<L> metaDataValue = this.metaDataFactory.createValue(id, context);
        if (metaDataValue == null) return null;
        V attributesValue = this.attributesFactory.createValue(id, context);
        return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
    }

    @Override
    public Map.Entry<InfinispanSessionMetaData<L>, V> findValue(String id) {
        InfinispanSessionMetaData<L> metaDataValue = this.metaDataFactory.findValue(id);
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
    public Map.Entry<InfinispanSessionMetaData<L>, V> tryValue(String id) {
        InfinispanSessionMetaData<L> metaDataValue = this.metaDataFactory.tryValue(id);
        if (metaDataValue != null) {
            V attributesValue = this.attributesFactory.tryValue(id);
            if (attributesValue != null) {
                return new SimpleImmutableEntry<>(metaDataValue, attributesValue);
            }
            // Purge obsolete meta data
            this.metaDataFactory.purge(id);
        }
        return null;
    }

    @Override
    public boolean remove(String id) {
        if (this.metaDataFactory.remove(id)) {
            this.attributesFactory.remove(id);
            return true;
        }
        return false;
    }

    @Override
    public SessionMetaDataFactory<InfinispanSessionMetaData<L>, L> getMetaDataFactory() {
        return this.metaDataFactory;
    }

    @Override
    public SessionAttributesFactory<V> getAttributesFactory() {
        return this.attributesFactory;
    }

    @Override
    public Session<L> createSession(String id, Map.Entry<InfinispanSessionMetaData<L>, V> entry) {
        InfinispanSessionMetaData<L> key = entry.getKey();
        InvalidatableSessionMetaData metaData = this.metaDataFactory.createSessionMetaData(id, key);
        SessionAttributes attributes = this.attributesFactory.createSessionAttributes(id, entry.getValue());
        return new InfinispanSession<>(id, metaData, attributes, key.getLocalContext(), this.localContextFactory, this);
    }

    @Override
    public ImmutableSession createImmutableSession(String id, ImmutableSessionMetaData metaData, ImmutableSessionAttributes attributes) {
        return new InfinispanImmutableSession(id, metaData, attributes);
    }
}
