/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Map;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemResourceDefinitionRegistrar;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;

/**
 * Enumerates the runtime operations of a remote Infinispan cluster.
 * @author Paul Ferraro
 */
public enum RemoteClusterOperation implements RuntimeOperation<Map.Entry<String, RemoteCacheManagerMXBean>> {

    SWITCH_CLUSTER("switch-cluster", ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(ExpressionResolver resolver, ModelNode operation, Map.Entry<String, RemoteCacheManagerMXBean> entry) throws OperationFailedException {
            return new ModelNode(entry.getValue().switchToCluster(entry.getKey()));
        }
    },
    ;

    private final OperationDefinition definition;

    RemoteClusterOperation(String name, ModelType replyType) {
        this.definition = new SimpleOperationDefinitionBuilder(name, InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(RemoteClusterResourceDefinitionRegistrar.REGISTRATION.getPathElement()))
                .setReplyType(replyType)
                .setRuntimeOnly()
                .build();
    }

    @Override
    public OperationDefinition getOperationDefinition() {
        return this.definition;
    }
}
