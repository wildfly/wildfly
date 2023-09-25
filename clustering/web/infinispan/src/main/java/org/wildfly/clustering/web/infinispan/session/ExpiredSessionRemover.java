/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Session remover that removes a session if and only if it is expired.
 * @param <SC> the ServletContext specification type
 * @param <MV> the meta-data value type
 * @param <AV> the attributes value type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public class ExpiredSessionRemover<SC, MV, AV, LC> implements Remover<String>, Registrar<Consumer<ImmutableSession>> {

    private final SessionFactory<SC, MV, AV, LC> factory;
    private final Collection<Consumer<ImmutableSession>> listeners = new CopyOnWriteArraySet<>();

    public ExpiredSessionRemover(SessionFactory<SC, MV, AV, LC> factory) {
        this.factory = factory;
    }

    @Override
    public boolean remove(String id) {
        MV metaDataValue = this.factory.getMetaDataFactory().tryValue(id);
        if (metaDataValue != null) {
            ImmutableSessionMetaData metaData = this.factory.getMetaDataFactory().createImmutableSessionMetaData(id, metaDataValue);
            if (metaData.isExpired()) {
                AV attributesValue = this.factory.getAttributesFactory().findValue(id);
                if (attributesValue != null) {
                    ImmutableSessionAttributes attributes = this.factory.getAttributesFactory().createImmutableSessionAttributes(id, attributesValue);
                    ImmutableSession session = this.factory.createImmutableSession(id, metaData, attributes);
                    InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s has expired.", id);
                    for (Consumer<ImmutableSession> listener : this.listeners) {
                        listener.accept(session);
                    }
                }
                return this.factory.remove(id);
            }
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s is not yet expired.", id);
        } else {
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was not found or is currently in use.", id);
        }
        return false;
    }

    @Override
    public Registration register(Consumer<ImmutableSession> listener) {
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }
}
