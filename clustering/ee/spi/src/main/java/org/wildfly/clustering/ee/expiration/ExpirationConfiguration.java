/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.expiration;

import java.util.function.Consumer;

/**
 * Encapsulates expiration configuration.
 * @author Paul Ferraro
 * @param <T> the expired object type
 */
public interface ExpirationConfiguration<T> extends Expiration {
    /**
     * The listener to notify of expiration events.
     * @return the listener to invoke when an object expires.
     */
    Consumer<T> getExpirationListener();
}
