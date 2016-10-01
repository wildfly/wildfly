/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.infinispan.Cache;
import org.infinispan.xsite.XSiteAdminOperations;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;

/**
 * Operation handler for backup site operations.
 * @author Paul Ferraro
 */
public class BackupOperationExecutor implements OperationExecutor<BackupOperationContext> {

    @Override
    public ModelNode execute(OperationContext context, Operation<BackupOperationContext> operation) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress cacheAddress = address.getParent();

        final String site = address.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();
        String containerName = cacheAddress.getParent().getLastElement().getValue();

        final Cache<?, ?> cache = ServiceContainerHelper.findValue(context.getServiceRegistry(true), InfinispanCacheRequirement.CACHE.getServiceName(context, containerName, cacheName));

        BackupOperationContext operationContext = new BackupOperationContext() {
            @Override
            public String getSite() {
                return site;
            }

            @Override
            public XSiteAdminOperations getOperations() {
                return cache.getAdvancedCache().getComponentRegistry().getComponent(XSiteAdminOperations.class);
            }
        };
        return (cache != null) ? operation.execute(operationContext) : null;
    }
}
