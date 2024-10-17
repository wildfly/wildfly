/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import jakarta.enterprise.concurrent.ManagedThreadFactory;

/**
 *
 * @author Eduardo Martins
 */
public interface WildFlyManagedThreadFactory extends ManagedThreadFactory {
    void stop();
    int getPriority();
}
