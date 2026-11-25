/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.function.ToIntFunction;

/**
 * Manages passivation handling/tracking of beans.
 */
public interface BeanPassivationManager extends ToIntFunction<String>, AutoCloseable {
    @Override
    void close();
}
