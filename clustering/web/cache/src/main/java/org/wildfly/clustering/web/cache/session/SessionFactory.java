/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.util.Map;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.metadata.SessionMetaDataFactory;
import org.wildfly.clustering.web.session.Session;

/**
 * Factory for creating sessions. Encapsulates the cache mapping strategy for sessions.
 * @param <SC> the ServletContext specification type
 * @param <MV> the meta-data value type
 * @param <AV> the attributes value type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public interface SessionFactory<SC, MV, AV, LC> extends ImmutableSessionFactory<MV, AV>, Creator<String, Map.Entry<MV, AV>, Duration>, Remover<String>, AutoCloseable {
    @Override
    SessionMetaDataFactory<MV> getMetaDataFactory();
    @Override
    SessionAttributesFactory<SC, AV> getAttributesFactory();

    Session<LC> createSession(String id, Map.Entry<MV, AV> entry, SC context);

    @Override
    void close();
}
