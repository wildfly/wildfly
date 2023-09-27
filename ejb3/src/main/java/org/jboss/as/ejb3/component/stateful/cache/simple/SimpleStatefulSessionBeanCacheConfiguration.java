/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.simple;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheConfiguration;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.server.ServerEnvironment;

/**
 * Configuration of a simple stateful session bean cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface SimpleStatefulSessionBeanCacheConfiguration<K, V extends StatefulSessionBeanInstance<K>> extends StatefulSessionBeanCacheConfiguration<K, V> {

    ServerEnvironment getEnvironment();
}
