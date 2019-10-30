/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.service.PassiveServiceSupplier;

/**
 * A handler for cache-container metrics.
 *
 * @author Paul Ferraro
 */
public class CacheContainerMetricExecutor implements MetricExecutor<EmbeddedCacheManager> {

    @Override
    public ModelNode execute(OperationContext context, Metric<EmbeddedCacheManager> metric) throws OperationFailedException {
        String containerName = context.getCurrentAddressValue();
        CacheContainer container = new PassiveServiceSupplier<CacheContainer>(context.getServiceRegistry(true), InfinispanRequirement.CONTAINER.getServiceName(context, containerName)).get();
        return (container != null) ? metric.execute(container) : null;
    }
}
