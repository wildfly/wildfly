/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum EventType {
    /**
     * @see jakarta.persistence.PrePersist
     */
    PRE_PERSIST,
    /**
     * @see jakarta.persistence.PreRemove
     */
    PRE_REMOVE,
    /**
     * @see jakarta.persistence.PreUpdate
     */
    PRE_UPDATE,
    /**
     * @see jakarta.persistence.PostLoad
     */
    POST_LOAD,
    /**
     * @see jakarta.persistence.PostPersist
     */
    POST_PERSIST,
    /**
     * @see jakarta.persistence.PostRemove
     */
    POST_REMOVE,
    /**
     * @see jakarta.persistence.PostUpdate
     */
    POST_UPDATE,
}
