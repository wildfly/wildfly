/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.Singleton;

/**
 * Executor for singleton service metrics.
 * @author Paul Ferraro
 */
public class SingletonServiceMetricExecutor extends SingletonMetricExecutor {

    public SingletonServiceMetricExecutor(FunctionExecutorRegistry<Singleton> executors) {
        super(ServiceName::parse, executors);
    }
}
