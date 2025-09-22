/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import java.util.concurrent.Executor;

import org.jboss.as.clustering.service.SuspendableService;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;

/**
 * A stateful session bean cache decorator that restarts its cache on suspend/resume.
 * @author Paul Ferraro
 */
public class SuspendableStatefulSessionBeanCache<K, V extends StatefulSessionBeanInstance<K>> extends DecoratedStatefulSessionBeanCache<K, V> {

    public SuspendableStatefulSessionBeanCache(StatefulSessionBeanCache<K, V> cache, SuspendableActivityRegistry registry, Executor executor) {
        super(cache, new SuspendableService(cache, registry, executor));
    }
}
