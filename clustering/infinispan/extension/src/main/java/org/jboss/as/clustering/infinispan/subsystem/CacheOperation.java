/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;

/**
 * Enumerates runtime operations of a cache.
 * @author Paul Ferraro
 */
public enum CacheOperation implements RuntimeOperation<CacheMgmtInterceptor> {

    RESET_STATISTICS("reset-statistics", ModelType.UNDEFINED) {
        @Override
        public ModelNode execute(ExpressionResolver resolver, ModelNode operation, CacheMgmtInterceptor interceptor) {
            interceptor.resetStatistics();
            return null;
        }
    },
    ;
    private final OperationDefinition definition;

    CacheOperation(String name, ModelType returnType) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(CacheRuntimeResourceDefinitionRegistrar.REGISTRATION.getPathElement());
        this.definition = new SimpleOperationDefinitionBuilder(name, resolver)
                .setReplyType(returnType)
                .setRuntimeOnly()
                .build();
    }

    @Override
    public OperationDefinition getOperationDefinition() {
        return this.definition;
    }
}
