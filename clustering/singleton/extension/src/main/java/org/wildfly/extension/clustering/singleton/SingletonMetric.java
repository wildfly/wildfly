/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
