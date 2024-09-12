/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.service.SingletonServiceActivator;
import org.wildfly.clustering.singleton.service.SingletonServiceActivatorContext;

/**
 * @author Paul Ferraro
 */
public class NodeServicePolicyActivator implements SingletonServiceActivator {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "default-policy");

    @Override
    public void activate(SingletonServiceActivatorContext context) {
        ServiceBuilder<?> builder = context.getServiceTarget().addService();
        builder.provides(SERVICE_NAME);
        builder.install();
    }
}
