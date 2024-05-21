/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import java.time.Duration;

import org.wildfly.clustering.server.expiration.Expiration;

/**
 * Encapsulates the expiration criteria for a bean.
 * Overrides the inherited semantics for zero timeout behavior.
 * @author Paul Ferraro
 */
public interface BeanExpiration extends Expiration {

    @Override
    default boolean isImmortal() {
        // EJB specification does not consider zero timeout to be immortal
        Duration timeout = this.getTimeout();
        return (timeout == null) || timeout.isNegative();
    }
}
