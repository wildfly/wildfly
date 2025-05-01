/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemResourceDefinitionRegistrar;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;

/**
 * Enumerates the runtime operations of a remote cache.
 * @author Paul Ferraro
 */
public enum RemoteCacheOperation implements RuntimeOperation<RemoteCacheClientStatisticsMXBean> {

    RESET_STATISTICS("reset-statistics", ModelType.UNDEFINED) {
        @Override
        public ModelNode execute(ExpressionResolver resolver, ModelNode operation, RemoteCacheClientStatisticsMXBean statistics) throws OperationFailedException {
            statistics.resetStatistics();
            return null;
        }
    },
    ;
    private final OperationDefinition definition;

    RemoteCacheOperation(String name, ModelType replyType) {
        this.definition = new SimpleOperationDefinitionBuilder(name, InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(RemoteCacheRuntimeResourceDefinitionRegistrar.REGISTRATION.getPathElement()))
                .setReplyType(replyType)
                .setRuntimeOnly()
                .build();
    }

    @Override
    public OperationDefinition getOperationDefinition() {
        return this.definition;
    }
}