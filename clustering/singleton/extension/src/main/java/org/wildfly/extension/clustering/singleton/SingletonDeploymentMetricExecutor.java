/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.function.Function;

import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.Singleton;

/**
 * Executor for singleton deployment metrics.
 * @author Paul Ferraro
 */
public class SingletonDeploymentMetricExecutor extends SingletonMetricExecutor {

    public SingletonDeploymentMetricExecutor(FunctionExecutorRegistry<Singleton> executors) {
        super(new Function<String, ServiceName>() {
            @Override
            public ServiceName apply(String deployment) {
                return Services.deploymentUnitName(deployment).append("installer");
            }
        }, executors);
    }
}
