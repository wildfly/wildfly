/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.Singleton;

/**
 * Metrics for singleton deployments and services.
 * @author Paul Ferraro
 */
public enum SingletonMetric implements Metric<Singleton> {

    IS_PRIMARY("is-primary", ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(Singleton singleton) throws OperationFailedException {
            return new ModelNode(singleton.isPrimary());
        }
    },
    PRIMARY_PROVIDER("primary-provider", ModelType.STRING) {
        @Override
        public ModelNode execute(Singleton singleton) throws OperationFailedException {
            Node primary = singleton.getPrimaryProvider();
            return (primary != null) ? new ModelNode(primary.getName()) : null;
        }
    },
    PROVIDERS("providers") {
        @Override
        public ModelNode execute(Singleton singleton) throws OperationFailedException {
            ModelNode result = new ModelNode();
            for (Node provider : singleton.getProviders()) {
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
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}
