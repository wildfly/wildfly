/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Map;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public enum RemoteClusterOperation implements Operation<Map.Entry<String, RemoteCacheManagerMXBean>> {

    SWITCH_CLUSTER("switch-cluster", ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(ExpressionResolver resolver, ModelNode operation, Map.Entry<String, RemoteCacheManagerMXBean> entry) throws OperationFailedException {
            return new ModelNode(entry.getValue().switchToCluster(entry.getKey()));
        }
    },
    ;

    private final OperationDefinition definition;

    RemoteClusterOperation(String name, ModelType replyType) {
        this.definition = new SimpleOperationDefinitionBuilder(name, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(RemoteClusterResourceDefinition.WILDCARD_PATH))
                .setReplyType(replyType)
                .setRuntimeOnly()
                .build();
    }

    @Override
    public OperationDefinition getDefinition() {
        return this.definition;
    }
}
