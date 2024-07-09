/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.service.capture.FunctionExecutorRegistry;

/**
 * Executor for singleton service metrics.
 * @author Paul Ferraro
 */
public class SingletonServiceMetricExecutor extends SingletonMetricExecutor {

    public SingletonServiceMetricExecutor(FunctionExecutorRegistry<ServiceName, Singleton> executors) {
        super(ServiceName::parse, executors);
    }
}
