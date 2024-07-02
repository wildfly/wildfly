/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import java.time.Duration;

import org.wildfly.clustering.server.expiration.ExpirationMetaData;

/**
 * Encapsulates the expiration-related meta data of a cached bean.
 * Overrides the inherited semantics for zero timeout behavior.
 * @author Paul Ferraro
 */
public interface BeanExpirationMetaData extends BeanExpiration, ExpirationMetaData {

    @Override
    default boolean isExpired() {
        Duration timeout = this.getTimeout();
        // EJB specification considers a zero timeout to be expired.
        if ((timeout != null) && timeout.isZero()) return true;
        return ExpirationMetaData.super.isExpired();
    }
}
