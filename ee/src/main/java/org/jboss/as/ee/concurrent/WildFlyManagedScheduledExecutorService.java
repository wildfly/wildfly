/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;

/**
 *
 * @author Eduardo Martins
 */
public interface WildFlyManagedScheduledExecutorService extends WildFlyManagedExecutorService, ManagedScheduledExecutorService {
}
