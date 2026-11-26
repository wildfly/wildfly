/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import java.util.function.UnaryOperator;

import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;

/**
 * @author Paul Ferraro
 */
public class SingletonDeploymentServiceTargetTransformer implements UnaryOperator<ServiceTarget> {
    private final ServiceTargetFactory targetFactory;

    public SingletonDeploymentServiceTargetTransformer(ServiceTargetFactory targetFactory) {
        this.targetFactory = targetFactory;
    }

    @Override
    public ServiceTarget apply(ServiceTarget target) {
        return this.targetFactory.createSingletonServiceTarget(target);
    }
}
