/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.ExpirationResourceDefinition.Attribute.INTERVAL;
import static org.jboss.as.clustering.infinispan.subsystem.ExpirationResourceDefinition.Attribute.LIFESPAN;
import static org.jboss.as.clustering.infinispan.subsystem.ExpirationResourceDefinition.Attribute.MAX_IDLE;

import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class ExpirationServiceConfigurator extends ComponentServiceConfigurator<ExpirationConfiguration> {

    private volatile long interval;
    private volatile long lifespan;
    private volatile long maxIdle;

    ExpirationServiceConfigurator(PathAddress address) {
        super(CacheComponent.EXPIRATION, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.interval = INTERVAL.resolveModelAttribute(context, model).asLong();
        this.lifespan = LIFESPAN.resolveModelAttribute(context, model).asLong(-1L);
        this.maxIdle = MAX_IDLE.resolveModelAttribute(context, model).asLong(-1L);
        return this;
    }

    @Override
    public ExpirationConfiguration get() {
        return new ConfigurationBuilder().expiration()
                .lifespan(this.lifespan, TimeUnit.MILLISECONDS)
                .maxIdle(this.maxIdle, TimeUnit.MILLISECONDS)
                .reaperEnabled(this.interval > 0)
                .wakeUpInterval(this.interval, TimeUnit.MILLISECONDS)
                .create();
    }
}
