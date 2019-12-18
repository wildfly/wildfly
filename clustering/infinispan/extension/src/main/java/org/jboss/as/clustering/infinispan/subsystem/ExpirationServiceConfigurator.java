/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
        this.lifespan = LIFESPAN.resolveModelAttribute(context, model).asLong(-1);
        this.maxIdle = MAX_IDLE.resolveModelAttribute(context, model).asLong(-1);
        return this;
    }

    @Override
    public ExpirationConfiguration get() {
        return new ConfigurationBuilder().expiration()
                .lifespan(this.lifespan, TimeUnit.MILLISECONDS)
                .maxIdle(this.maxIdle, TimeUnit.MILLISECONDS)
                .reaperEnabled((this.lifespan > 0) || (this.maxIdle > 0))
                .wakeUpInterval(this.interval, TimeUnit.MILLISECONDS)
                .create();
    }
}
