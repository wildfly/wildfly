/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Metrics for singleton deployments and services.
 * @author Paul Ferraro
 */
public enum SingletonMetric implements Metric<Singleton> {

    IS_PRIMARY("is-primary", ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(Singleton singleton) throws OperationFailedException {
            return singleton.getSingletonState().isPrimaryProvider() ? ModelNode.TRUE : ModelNode.FALSE;
        }
    },
    PRIMARY_PROVIDER("primary-provider", ModelType.STRING) {
        @Override
        public ModelNode execute(Singleton singleton) throws OperationFailedException {
            return singleton.getSingletonState().getPrimaryProvider().map(GroupMember::getName).map(ModelNode::new).orElse(null);
        }
    },
    PROVIDERS("providers") {
        @Override
        public ModelNode execute(Singleton singleton) throws OperationFailedException {
            ModelNode result = new ModelNode();
            for (GroupMember provider : singleton.getSingletonState().getProviders()) {
                result.add(provider.getName());
            }
            return result;
        }
    }
    ;
    private final AttributeDefinition definition;

    SingletonMetric(String name, ModelType type) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type).setStorageRuntime().build();
    }

    SingletonMetric(String name) {
        this.definition = new StringListAttributeDefinition.Builder(name).setStorageRuntime().build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }
}
