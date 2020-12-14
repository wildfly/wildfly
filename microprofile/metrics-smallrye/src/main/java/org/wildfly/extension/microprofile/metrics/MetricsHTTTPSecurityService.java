/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.microprofile.metrics;


import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics.MetricsSubsystemDefinition;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MetricsHTTTPSecurityService implements Service {

    private final boolean securityEnabled;
    private final Consumer<Boolean> consumer;


    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(ServiceName.parse(MetricsSubsystemDefinition.METRICS_HTTP_SECURITY_CAPABILITY));

        Consumer<Boolean> consumer = serviceBuilder.provides(ServiceName.parse(MetricsSubsystemDefinition.METRICS_HTTP_SECURITY_CAPABILITY));
        serviceBuilder.setInstance(new MetricsHTTTPSecurityService(consumer, securityEnabled)).install();
    }


    public MetricsHTTTPSecurityService(Consumer<Boolean> consumer, boolean securityEnabled) {
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
