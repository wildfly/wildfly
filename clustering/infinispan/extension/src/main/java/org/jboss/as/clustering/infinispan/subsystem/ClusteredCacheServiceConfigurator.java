/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.ClusteredCacheResourceDefinition.Attribute.REMOTE_TIMEOUT;

import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Builds the configuration of a clustered cache.
 * @author Paul Ferraro
 */
public class ClusteredCacheServiceConfigurator extends CacheConfigurationServiceConfigurator {

    private volatile long remoteTimeout;

    ClusteredCacheServiceConfigurator(PathAddress address, CacheMode mode) {
        super(address, mode.toSync());
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.remoteTimeout = REMOTE_TIMEOUT.resolveModelAttribute(context, model).asLong();
        return super.configure(context, model);
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        builder.clustering().remoteTimeout(this.remoteTimeout, TimeUnit.MILLISECONDS);
        super.accept(builder);
    }
}
