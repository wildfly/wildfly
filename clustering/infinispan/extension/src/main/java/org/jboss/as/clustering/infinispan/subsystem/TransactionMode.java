/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

/**
 * @author Paul Ferraro
 */
public enum TransactionMode {
    NONE,
    BATCH,
    NON_XA,
    NON_DURABLE_XA,
    FULL_XA,
    ;
}
