/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.fine;

import java.util.function.BiConsumer;

/**
 * @author Paul Ferraro
 */
public interface SessionAttributeActivationNotifier extends AutoCloseable {
    BiConsumer<SessionAttributeActivationNotifier, Object> PRE_PASSIVATE = SessionAttributeActivationNotifier::prePassivate;
    BiConsumer<SessionAttributeActivationNotifier, Object> POST_ACTIVATE = SessionAttributeActivationNotifier::postActivate;

    /**
     * Notifies the specified attribute that it will be passivated, if interested.
     */
    void prePassivate(Object value);

    /**
     * Notifies the specified attribute that it was activated, if interested.
     */
    void postActivate(Object value);

    @Override
    void close();
}
