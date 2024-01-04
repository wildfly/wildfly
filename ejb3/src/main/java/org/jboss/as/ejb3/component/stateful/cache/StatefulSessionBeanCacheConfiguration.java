/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Configuration of a stateful session bean cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanCacheConfiguration<K, V extends StatefulSessionBeanInstance<K>> {

    String getComponentName();
    Supplier<K> getIdentifierFactory();
    StatefulSessionBeanInstanceFactory<V> getInstanceFactory();
    Duration getTimeout();
}
