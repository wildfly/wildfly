/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability;

@FunctionalInterface
public interface ResourceMetricsCheck {
    void evaluate() throws Exception;
}
