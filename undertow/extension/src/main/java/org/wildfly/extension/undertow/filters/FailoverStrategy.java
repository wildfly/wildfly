/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

/**
 * @author Radoslav Husar
 */
public enum FailoverStrategy {
    /**
     * Failover target chosen via load balancing mechanism.
     */
    LOAD_BALANCED,
    /**
     * Failover target chosen deterministically from the associated session identifier.
     */
    DETERMINISTIC,
    ;
}