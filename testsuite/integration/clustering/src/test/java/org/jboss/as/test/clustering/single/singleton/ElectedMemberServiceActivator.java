/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.singleton;

import java.util.Map;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.service.ServiceDependency;

/**
 * References a SingletonServiceTargetFactory via its backing cache, without a singleton policy.
 * @author Paul Ferraro
 */
public class ElectedMemberServiceActivator extends org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberServiceActivator {

    @Override
    public ServiceDependency<SingletonServiceTargetFactory> getSingletonServiceTargetFactoryDependency() {
        Map.Entry<String, String[]> resolved = SingletonServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR.resolve("server");
        return ServiceDependency.on(ServiceName.parse(resolved.getKey()).append(resolved.getValue()));
    }
}
