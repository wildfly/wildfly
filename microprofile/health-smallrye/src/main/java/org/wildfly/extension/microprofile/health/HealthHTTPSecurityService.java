/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;


import static org.wildfly.extension.health.HealthSubsystemDefinition.HEALTH_HTTP_SECURITY_CAPABILITY;

import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class HealthHTTPSecurityService implements Service {

    private final boolean securityEnabled;
    private final Consumer<Boolean> consumer;


    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(ServiceName.parse(HEALTH_HTTP_SECURITY_CAPABILITY));

        Consumer<Boolean> consumer = serviceBuilder.provides(ServiceName.parse(HEALTH_HTTP_SECURITY_CAPABILITY));
        serviceBuilder.setInstance(new HealthHTTPSecurityService(consumer, securityEnabled)).install();
    }


    public HealthHTTPSecurityService(Consumer<Boolean> consumer, boolean securityEnabled) {
        this.consumer = consumer;
        this.securityEnabled = securityEnabled;
    }

    @Override
    public void start(StartContext context) {
        consumer.accept(securityEnabled);
    }

    @Override
    public void stop(StopContext context) {
        consumer.accept(null);
    }
}
