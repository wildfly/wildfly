/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Map;

import org.infinispan.xsite.XSiteAdminOperations;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;

/**
 * Backup site operations.
 * @author Paul Ferraro
 */
public enum BackupSiteOperation implements RuntimeOperation<Map.Entry<String, XSiteAdminOperations>> {

    BRING_SITE_ONLINE("bring-site-online", false) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, Map.Entry<String, XSiteAdminOperations> context) {
            return new ModelNode(context.getValue().bringSiteOnline(context.getKey()));
        }
    },
    TAKE_SITE_OFFLINE("take-site-offline", false) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, Map.Entry<String, XSiteAdminOperations> context) {
            return new ModelNode(context.getValue().takeSiteOffline(context.getKey()));
        }
    },
    SITE_STATUS("site-status", true) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, Map.Entry<String, XSiteAdminOperations> context) {
            return new ModelNode(context.getValue().siteStatus(context.getKey()));
        }
    },
    ;
    private final OperationDefinition definition;

    BackupSiteOperation(String name, boolean readOnly) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(BackupSiteResourceDefinitionRegistrar.REGISTRATION.getPathElement());
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(name, resolver);
        if (readOnly) {
            builder.setReadOnly();
        }
        this.definition = builder.setReplyType(ModelType.STRING).setRuntimeOnly().build();
    }

    @Override
    public OperationDefinition getOperationDefinition() {
        return this.definition;
    }
}
