/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability;

/** Performs one metrics assertion during an eventually-consistent check. */
@FunctionalInterface
public interface ResourceMetricsCheck {
    /** Evaluates the current resource metrics state. */
    void evaluate() throws Exception;
}
