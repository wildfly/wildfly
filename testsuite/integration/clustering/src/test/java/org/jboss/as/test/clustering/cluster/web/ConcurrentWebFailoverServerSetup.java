/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.web;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class ConcurrentWebFailoverServerSetup implements ServerSetupTask {
    private static final String CONTAINER_NAME = "web";
    private static final String CACHE_NAME = "concurrent";

    @Override
    public void setup(ManagementClient client, String containerId) throws Exception {
        if (!client.isClosed()) {
            PathAddress cacheAddress = getCacheAddress();
            ModelNode addCacheOperation = Util.createAddOperation(cacheAddress);
            ModelNode addCacheStoreOperation = Util.createAddOperation(getCacheStoreAddress(cacheAddress));

            ManagementOperations.executeOperationRaw(client.getControllerClient(), Operations.createCompositeOperation(addCacheOperation, addCacheStoreOperation));
        }
    }

    @Override
    public void tearDown(ManagementClient client, String containerId) throws Exception {
        if (!client.isClosed()) {
            PathAddress cacheAddress = getCacheAddress();
            ModelNode removeCacheOperation = Util.createRemoveOperation(cacheAddress);
            ModelNode removeCacheStoreOperation = Util.createRemoveOperation(getCacheStoreAddress(cacheAddress));

            ManagementOperations.executeOperationRaw(client.getControllerClient(), Operations.createCompositeOperation(removeCacheOperation, removeCacheStoreOperation));
        }
    }

    private static PathAddress getCacheAddress() {
        return PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "infinispan"), PathElement.pathElement("cache-container", CONTAINER_NAME), PathElement.pathElement("distributed-cache", CACHE_NAME));
    }

    private static PathAddress getCacheStoreAddress(PathAddress cacheAddress) {
        return cacheAddress.append("store", "file");
    }
}
